package com.brasper.platform.iam.infrastructure.bootstrap;

import com.brasper.platform.iam.application.commandservices.UserCommandService;
import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.Role;
import com.brasper.platform.iam.domain.model.valueobjects.Username;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin at startup, because only an admin can create users. Does nothing unless
 * {@code BOOTSTRAP_ADMIN_USERNAME} and {@code BOOTSTRAP_ADMIN_PASSWORD} are set and no active admin exists.
 */
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserCommandService userCommands;
    private final UserRepository users;
    private final String username;
    private final String password;

    public BootstrapAdminRunner(UserCommandService userCommands, UserRepository users,
            @Value("${brasper.bootstrap-admin.username:}") String username,
            @Value("${brasper.bootstrap-admin.password:}") String password) {
        this.userCommands = userCommands;
        this.users = users;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank() || users.countByRoleAndActive(Role.ROLE_ADMIN, true) > 0) {
            return;
        }
        // Never log the password; a bad username or weak password fails here and the app keeps starting.
        try {
            userCommands.handle(new SignUpCommand(new Username(username), password, "Administrator",
                    Role.ROLE_ADMIN)).fold(
                    user -> {
                        log.info("Bootstrap admin '{}' created", username);
                        return null;
                    },
                    failure -> {
                        log.warn("Bootstrap admin not created: {}", failure.messageKey());
                        return null;
                    });
        } catch (IllegalArgumentException e) {
            log.warn("Bootstrap admin not created: invalid username");
        }
    }
}
