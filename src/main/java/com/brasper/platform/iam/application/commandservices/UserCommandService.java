package com.brasper.platform.iam.application.commandservices;

import com.brasper.platform.iam.application.outboundservices.HashingService;
import com.brasper.platform.iam.application.outboundservices.TokenService;
import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.model.commands.SignInCommand;
import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
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
    /** Checked when the username does not exist, so unknown and wrong-password logins take the same time. */
    private final String decoyHash;

    public UserCommandService(UserRepository users, HashingService hashing, TokenService tokens) {
        this.users = users;
        this.hashing = hashing;
        this.tokens = tokens;
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
        User user = users.findByUsername(command.username()).orElse(null);
        if (user == null) {
            hashing.matches(command.password(), decoyHash);
            return Result.failure(UserCommandFailure.INVALID_CREDENTIALS);
        }
        if (!hashing.matches(command.password(), user.getPasswordHash())) {
            return Result.failure(UserCommandFailure.INVALID_CREDENTIALS);
        }
        // Checked after the password so an inactive account is not revealed to someone who does not know it.
        if (!user.isActive()) {
            return Result.failure(UserCommandFailure.USER_INACTIVE);
        }
        return Result.success(new AuthenticatedUser(user, tokens.generateToken(user)));
    }

    private static boolean isStrong(String password) {
        return password != null
                && password.length() >= MIN_PASSWORD_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
    }
}
