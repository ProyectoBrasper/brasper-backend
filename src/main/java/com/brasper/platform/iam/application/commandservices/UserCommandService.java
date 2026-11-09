package com.brasper.platform.iam.application.commandservices;

import com.brasper.platform.iam.application.outboundservices.HashingService;
import com.brasper.platform.iam.application.outboundservices.TokenService;
import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.model.commands.ChangePasswordCommand;
import com.brasper.platform.iam.domain.model.commands.ChangeUserRoleCommand;
import com.brasper.platform.iam.domain.model.commands.DeactivateUserCommand;
import com.brasper.platform.iam.domain.model.commands.ReactivateUserCommand;
import com.brasper.platform.iam.domain.model.commands.ResetUserPasswordCommand;
import com.brasper.platform.iam.domain.model.commands.SignInCommand;
import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.Role;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import com.brasper.platform.shared.application.result.Result;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCommandService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72; // BCrypt ignores (or rejects) anything longer.

    private final UserRepository users;
    private final HashingService hashing;
    private final TokenService tokens;
    private final SignInThrottle throttle;
    /** Checked when the username does not exist, so unknown and wrong-password logins take the same time. */
    private final String decoyHash;

    public UserCommandService(UserRepository users, HashingService hashing, TokenService tokens,
            SignInThrottle throttle) {
        this.users = users;
        this.hashing = hashing;
        this.tokens = tokens;
        this.throttle = throttle;
        this.decoyHash = hashing.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public Result<User, UserCommandFailure> handle(SignUpCommand command) {
        if (!isStrong(command.password())) {
            return Result.failure(UserCommandFailure.WEAK_PASSWORD);
        }
        if (users.existsByUsername(command.username())) {
            return Result.failure(UserCommandFailure.DUPLICATE_USERNAME);
        }
        return Result.success(users.save(new User(command, hashing.encode(command.password()))));
    }

    @Transactional(readOnly = true)
    public Result<AuthenticatedUser, UserCommandFailure> handle(SignInCommand command) {
        if (throttle.isLocked(command.username().value())) {
            return Result.failure(UserCommandFailure.TOO_MANY_ATTEMPTS);
        }
        User user = users.findByUsername(command.username()).orElse(null);
        if (user == null) {
            hashing.matches(command.password(), decoyHash);
            throttle.failed(command.username().value());
            return Result.failure(UserCommandFailure.INVALID_CREDENTIALS);
        }
        if (!hashing.matches(command.password(), user.getPasswordHash())) {
            throttle.failed(command.username().value());
            return Result.failure(UserCommandFailure.INVALID_CREDENTIALS);
        }
        throttle.succeeded(command.username().value());
        // Checked after the password so an inactive account is not revealed to someone who does not know it.
        if (!user.isActive()) {
            return Result.failure(UserCommandFailure.USER_INACTIVE);
        }
        return Result.success(new AuthenticatedUser(user, tokens.generateToken(user)));
    }

    /**
     * A signed-in user changes their own password. Wrong current passwords count toward the sign-in lockout, so a
     * stolen token cannot be used to guess it. shortcut: tokens already issued stay valid until they expire (the JWT is
     * stateless); add a per-user token version if revoking them matters.
     */
    @Transactional
    public Result<User, UserCommandFailure> handle(ChangePasswordCommand command) {
        String name = command.username().value();
        if (throttle.isLocked(name)) {
            return Result.failure(UserCommandFailure.TOO_MANY_ATTEMPTS);
        }
        User user = users.findByUsername(command.username()).filter(User::isActive).orElse(null);
        if (user == null) {
            return Result.failure(UserCommandFailure.USER_NOT_FOUND);
        }
        if (!hashing.matches(command.currentPassword(), user.getPasswordHash())) {
            throttle.failed(name);
            return Result.failure(UserCommandFailure.INVALID_CURRENT_PASSWORD);
        }
        if (!isStrong(command.newPassword())) {
            return Result.failure(UserCommandFailure.WEAK_PASSWORD);
        }
        throttle.succeeded(name);
        user.changePasswordHash(hashing.encode(command.newPassword()));
        return Result.success(users.save(user));
    }

    @Transactional
    public Result<User, UserCommandFailure> handle(ChangeUserRoleCommand command) {
        User user = users.findById(command.userId()).orElse(null);
        if (user == null) {
            return Result.failure(UserCommandFailure.USER_NOT_FOUND);
        }
        if (command.role() != Role.ROLE_ADMIN && isLastActiveAdmin(user)) {
            return Result.failure(UserCommandFailure.CANNOT_MODIFY_LAST_ADMIN);
        }
        user.changeRole(command.role());
        return Result.success(users.save(user));
    }

    @Transactional
    public Result<User, UserCommandFailure> handle(DeactivateUserCommand command) {
        User user = users.findById(command.userId()).orElse(null);
        if (user == null) {
            return Result.failure(UserCommandFailure.USER_NOT_FOUND);
        }
        if (isLastActiveAdmin(user)) {
            return Result.failure(UserCommandFailure.CANNOT_MODIFY_LAST_ADMIN);
        }
        user.deactivate();
        return Result.success(users.save(user));
    }

    /** An admin sets a new password for any user, which also lifts that user's sign-in lockout. */
    @Transactional
    public Result<User, UserCommandFailure> handle(ResetUserPasswordCommand command) {
        User user = users.findById(command.userId()).orElse(null);
        if (user == null) {
            return Result.failure(UserCommandFailure.USER_NOT_FOUND);
        }
        if (!isStrong(command.newPassword())) {
            return Result.failure(UserCommandFailure.WEAK_PASSWORD);
        }
        user.changePasswordHash(hashing.encode(command.newPassword()));
        throttle.succeeded(user.getUsername().value());
        return Result.success(users.save(user));
    }

    @Transactional
    public Result<User, UserCommandFailure> handle(ReactivateUserCommand command) {
        User user = users.findById(command.userId()).orElse(null);
        if (user == null) {
            return Result.failure(UserCommandFailure.USER_NOT_FOUND);
        }
        user.reactivate();
        return Result.success(users.save(user));
    }

    /** The system must always keep one admin who can still manage users. */
    private boolean isLastActiveAdmin(User user) {
        return user.isActive() && user.getRole() == Role.ROLE_ADMIN
                && users.countByRoleAndActive(Role.ROLE_ADMIN, true) <= 1;
    }

    private static boolean isStrong(String password) {
        return password != null
                && password.length() >= MIN_PASSWORD_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
    }
}
