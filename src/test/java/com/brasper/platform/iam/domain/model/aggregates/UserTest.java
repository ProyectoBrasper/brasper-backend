package com.brasper.platform.iam.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.Role;
import com.brasper.platform.iam.domain.model.valueobjects.Username;
import org.junit.jupiter.api.Test;

class UserTest {

    private static SignUpCommand command(String fullName) {
        return new SignUpCommand(new Username("ana"), "raw-password", fullName, Role.ROLE_ENGINEER);
    }

    @Test
    void startsActiveWithTheGivenHashNotTheRawPassword() {
        User user = new User(command("  Ana Pérez "), "$2a$hash");

        assertThat(user.getUsername()).isEqualTo(new Username("ana"));
        assertThat(user.getFullName()).isEqualTo("Ana Pérez");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_ENGINEER);
        assertThat(user.getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void requiresFullNameAndHash() {
        assertThatThrownBy(() -> new User(command(" "), "$2a$hash")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new User(command("Ana"), " ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changesRole() {
        User user = new User(command("Ana"), "$2a$hash");

        user.changeRole(Role.ROLE_ADMIN);

        assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    void deactivatesOnlyOnce() {
        User user = new User(command("Ana"), "$2a$hash");

        user.deactivate();

        assertThat(user.isActive()).isFalse();
        assertThatThrownBy(user::deactivate).isInstanceOf(IllegalStateException.class);
    }
}
