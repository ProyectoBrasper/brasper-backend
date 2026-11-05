package com.brasper.platform.iam.infrastructure.bootstrap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brasper.platform.iam.application.commandservices.UserCommandFailure;
import com.brasper.platform.iam.application.commandservices.UserCommandService;
import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.Role;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import com.brasper.platform.shared.application.result.Result;
import org.junit.jupiter.api.Test;

class BootstrapAdminRunnerTest {

    private final UserCommandService commands = mock(UserCommandService.class);
    private final UserRepository users = mock(UserRepository.class);

    private void run(String username, String password) {
        new BootstrapAdminRunner(commands, users, username, password).run(null);
    }

    @Test
    void createsTheAdminWhenNoneExists() {
        when(users.countByRoleAndActive(Role.ROLE_ADMIN, true)).thenReturn(0L);
        when(commands.handle(any(SignUpCommand.class))).thenReturn(Result.failure(UserCommandFailure.WEAK_PASSWORD));

        run("root", "clave1234");

        verify(commands).handle(argThat((SignUpCommand c) -> c.username().value().equals("root")
                && c.role() == Role.ROLE_ADMIN && c.password().equals("clave1234")));
    }

    @Test
    void doesNothingWhenAnAdminAlreadyExists() {
        when(users.countByRoleAndActive(Role.ROLE_ADMIN, true)).thenReturn(1L);

        run("root", "clave1234");

        verify(commands, never()).handle(any(SignUpCommand.class));
    }

    @Test
    void doesNothingWithoutCredentials() {
        run("", "");
        run("root", " ");

        verify(commands, never()).handle(any(SignUpCommand.class));
    }

    @Test
    void aMalformedUsernameDoesNotStopStartup() {
        when(users.countByRoleAndActive(Role.ROLE_ADMIN, true)).thenReturn(0L);

        run("a b", "clave1234");

        verify(commands, never()).handle(any(SignUpCommand.class));
    }
}
