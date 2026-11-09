package com.brasper.platform.iam.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.brasper.platform.iam.domain.model.valueobjects.Username;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserCommandServiceTest {

    private static final Username ANA = new Username("ana");

    private final UserRepository users = mock(UserRepository.class);
    private final HashingService hashing = mock(HashingService.class);
    private final TokenService tokens = mock(TokenService.class);
    private UserCommandService service;

    @BeforeEach
    void setUp() {
        when(hashing.encode(anyString())).thenAnswer(call -> "hash(" + call.getArgument(0) + ")");
        when(hashing.matches(anyString(), anyString()))
                .thenAnswer(call -> ("hash(" + call.getArgument(0) + ")").equals(call.getArgument(1)));
        service = new UserCommandService(users, hashing, tokens, new SignInThrottle());
    }

    private static SignUpCommand signUp(String password) {
        return new SignUpCommand(ANA, password, "Ana Pérez", Role.ROLE_ENGINEER);
    }

    private User storedUser() {
        return new User(signUp("clave1234"), "hash(clave1234)");
    }

    @Test
    void signUpStoresTheHashNotThePassword() {
        when(users.existsByUsername(ANA)).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.handle(signUp("clave1234"));

        assertThat(result.isSuccess()).isTrue();
        User saved = result.success().orElseThrow();
        assertThat(saved.getPasswordHash()).isEqualTo("hash(clave1234)");
        verify(users).save(saved);
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1", "onlyletters", "1234567890", "        "})
    void signUpRejectsWeakPasswords(String password) {
        var result = service.handle(signUp(password));

        assertThat(result.failure()).contains(UserCommandFailure.WEAK_PASSWORD);
        verify(users, never()).save(any());
    }

    @Test
    void signUpRejectsPasswordsOverBcryptLimit() {
        var result = service.handle(signUp("a1".repeat(37)));

        assertThat(result.failure()).contains(UserCommandFailure.WEAK_PASSWORD);
    }

    @Test
    void signUpRejectsDuplicateUsername() {
        when(users.existsByUsername(ANA)).thenReturn(true);

        var result = service.handle(signUp("clave1234"));

        assertThat(result.failure()).contains(UserCommandFailure.DUPLICATE_USERNAME);
        verify(users, never()).save(any());
    }

    @Test
    void changePasswordStoresTheNewHash() {
        User user = storedUser();
        when(users.findByUsername(ANA)).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.handle(new ChangePasswordCommand(ANA, "clave1234", "nueva-clave99"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("hash(nueva-clave99)");
        verify(users).save(user);
    }

    @Test
    void changePasswordRejectsAWrongCurrentPasswordAndAWeakNewOneWithoutSaving() {
        User user = storedUser();
        when(users.findByUsername(ANA)).thenReturn(Optional.of(user));

        assertThat(service.handle(new ChangePasswordCommand(ANA, "otra-clave1", "nueva-clave99")).failure())
                .contains(UserCommandFailure.INVALID_CURRENT_PASSWORD);
        assertThat(service.handle(new ChangePasswordCommand(ANA, "clave1234", "corta1")).failure())
                .contains(UserCommandFailure.WEAK_PASSWORD);
        assertThat(user.getPasswordHash()).isEqualTo("hash(clave1234)");
        verify(users, never()).save(any());
    }

    @Test
    void changePasswordRejectsUnknownAndInactiveUsers() {
        User inactive = storedUser();
        inactive.deactivate();
        when(users.findByUsername(ANA)).thenReturn(Optional.empty(), Optional.of(inactive));

        var change = new ChangePasswordCommand(ANA, "clave1234", "nueva-clave99");
        assertThat(service.handle(change).failure()).contains(UserCommandFailure.USER_NOT_FOUND);
        assertThat(service.handle(change).failure()).contains(UserCommandFailure.USER_NOT_FOUND);
        verify(users, never()).save(any());
    }

    @Test
    void wrongCurrentPasswordsCountTowardTheLockout() {
        when(users.findByUsername(ANA)).thenReturn(Optional.of(storedUser()));
        var wrong = new ChangePasswordCommand(ANA, "otra-clave1", "nueva-clave99");
        for (int attempt = 0; attempt < SignInThrottle.MAX_FAILURES; attempt++) {
            service.handle(wrong);
        }

        var right = new ChangePasswordCommand(ANA, "clave1234", "nueva-clave99");
        assertThat(service.handle(right).failure()).contains(UserCommandFailure.TOO_MANY_ATTEMPTS);
        verify(users, never()).save(any());
    }

    @Test
    void signInReturnsUserAndToken() {
        User user = storedUser();
        when(users.findByUsername(ANA)).thenReturn(Optional.of(user));
        when(tokens.generateToken(user)).thenReturn("jwt-token");

        var result = service.handle(new SignInCommand(ANA, "clave1234"));

        assertThat(result.success()).contains(new AuthenticatedUser(user, "jwt-token"));
    }

    @Test
    void signInWithWrongPasswordIsInvalidCredentials() {
        when(users.findByUsername(ANA)).thenReturn(Optional.of(storedUser()));

        var result = service.handle(new SignInCommand(ANA, "otra-clave1"));

        assertThat(result.failure()).contains(UserCommandFailure.INVALID_CREDENTIALS);
        verify(tokens, never()).generateToken(any());
    }

    @Test
    void signInWithUnknownUserIsTheSameFailureAndStillHashes() {
        when(users.findByUsername(ANA)).thenReturn(Optional.empty());

        var result = service.handle(new SignInCommand(ANA, "clave1234"));

        assertThat(result.failure()).contains(UserCommandFailure.INVALID_CREDENTIALS);
        verify(hashing).matches(anyString(), anyString());
    }

    @Test
    void fiveWrongPasswordsLockTheUsernameEvenForTheRightPassword() {
        when(users.findByUsername(ANA)).thenReturn(Optional.of(storedUser()));

        for (int i = 0; i < 5; i++) {
            assertThat(service.handle(new SignInCommand(ANA, "otra-clave1")).failure())
                    .contains(UserCommandFailure.INVALID_CREDENTIALS);
        }

        assertThat(service.handle(new SignInCommand(ANA, "clave1234")).failure())
                .contains(UserCommandFailure.TOO_MANY_ATTEMPTS);
        verify(tokens, never()).generateToken(any());
    }

    @Test
    void inactiveUserIsOnlyRevealedAfterTheCorrectPassword() {
        User user = storedUser();
        user.deactivate();
        when(users.findByUsername(ANA)).thenReturn(Optional.of(user));

        assertThat(service.handle(new SignInCommand(ANA, "clave1234")).failure())
                .contains(UserCommandFailure.USER_INACTIVE);
        assertThat(service.handle(new SignInCommand(ANA, "otra-clave1")).failure())
                .contains(UserCommandFailure.INVALID_CREDENTIALS);
    }

    @Test
    void anAdminResetsAPasswordAndThatLiftsTheLockout() {
        User user = storedUser();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.findById(2L)).thenReturn(Optional.empty());
        when(users.findByUsername(ANA)).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        for (int attempt = 0; attempt < SignInThrottle.MAX_FAILURES; attempt++) {
            service.handle(new SignInCommand(ANA, "otra-clave1"));
        }
        assertThat(service.handle(new SignInCommand(ANA, "clave1234")).failure())
                .contains(UserCommandFailure.TOO_MANY_ATTEMPTS);

        assertThat(service.handle(new ResetUserPasswordCommand(1L, "corta1")).failure())
                .contains(UserCommandFailure.WEAK_PASSWORD);
        assertThat(service.handle(new ResetUserPasswordCommand(2L, "nueva-clave99")).failure())
                .contains(UserCommandFailure.USER_NOT_FOUND);
        assertThat(user.getPasswordHash()).isEqualTo("hash(clave1234)");

        assertThat(service.handle(new ResetUserPasswordCommand(1L, "nueva-clave99")).isSuccess()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("hash(nueva-clave99)");
        when(tokens.generateToken(user)).thenReturn("jwt-token");
        assertThat(service.handle(new SignInCommand(ANA, "nueva-clave99")).isSuccess()).isTrue();
    }

    @Test
    void reactivatesAnInactiveUserAndRejectsAMissingOne() {
        User user = storedUser();
        user.deactivate();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.findById(2L)).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.handle(new ReactivateUserCommand(1L)).success().orElseThrow().isActive()).isTrue();
        assertThat(service.handle(new ReactivateUserCommand(2L)).failure()).contains(UserCommandFailure.USER_NOT_FOUND);
    }

    private User admin() {
        return new User(new SignUpCommand(new Username("root"), "clave1234", "Root", Role.ROLE_ADMIN), "h");
    }

    @Test
    void changeRoleUpdatesTheUser() {
        User user = storedUser();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);

        var result = service.handle(new ChangeUserRoleCommand(1L, Role.ROLE_ACCOUNTING));

        assertThat(result.success().orElseThrow().getRole()).isEqualTo(Role.ROLE_ACCOUNTING);
    }

    @Test
    void managingAnUnknownUserFails() {
        when(users.findById(9L)).thenReturn(Optional.empty());

        assertThat(service.handle(new ChangeUserRoleCommand(9L, Role.ROLE_ADMIN)).failure())
                .contains(UserCommandFailure.USER_NOT_FOUND);
        assertThat(service.handle(new DeactivateUserCommand(9L)).failure())
                .contains(UserCommandFailure.USER_NOT_FOUND);
    }

    @Test
    void theLastActiveAdminCannotBeDemotedOrDeactivated() {
        User root = admin();
        when(users.findById(1L)).thenReturn(Optional.of(root));
        when(users.countByRoleAndActive(Role.ROLE_ADMIN, true)).thenReturn(1L);

        assertThat(service.handle(new ChangeUserRoleCommand(1L, Role.ROLE_ENGINEER)).failure())
                .contains(UserCommandFailure.CANNOT_MODIFY_LAST_ADMIN);
        assertThat(service.handle(new DeactivateUserCommand(1L)).failure())
                .contains(UserCommandFailure.CANNOT_MODIFY_LAST_ADMIN);
        assertThat(root.isActive()).isTrue();
        assertThat(root.getRole()).isEqualTo(Role.ROLE_ADMIN);
        verify(users, never()).save(any());
    }

    @Test
    void anAdminCanBeDeactivatedWhenAnotherRemains() {
        User root = admin();
        when(users.findById(1L)).thenReturn(Optional.of(root));
        when(users.countByRoleAndActive(Role.ROLE_ADMIN, true)).thenReturn(2L);
        when(users.save(root)).thenReturn(root);

        assertThat(service.handle(new DeactivateUserCommand(1L)).isSuccess()).isTrue();
        assertThat(root.isActive()).isFalse();
    }
}
