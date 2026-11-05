package com.brasper.platform.iam.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.Role;
import com.brasper.platform.iam.domain.model.valueobjects.Username;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserJpaRepositoryTest {

    @Autowired
    private UserJpaRepository users;

    private User user(String username, Role role) {
        return new User(new SignUpCommand(new Username(username), "raw", "Full " + username, role), "$2a$hash");
    }

    @Test
    void savesAndFindsByUsernameWithAuditDates() {
        User saved = users.saveAndFlush(user("ana", Role.ROLE_ENGINEER));

        User found = users.findByUsername(new Username("ANA")).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getRole()).isEqualTo(Role.ROLE_ENGINEER);
        assertThat(found.isActive()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void existsByUsername() {
        users.saveAndFlush(user("luis", Role.ROLE_ACCOUNTING));

        assertThat(users.existsByUsername(new Username("luis"))).isTrue();
        assertThat(users.existsByUsername(new Username("nadie"))).isFalse();
    }

    @Test
    void rejectsDuplicateUsername() {
        users.saveAndFlush(user("dup", Role.ROLE_ENGINEER));

        assertThatThrownBy(() -> users.saveAndFlush(user("dup", Role.ROLE_ADMIN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void countsActiveUsersOfARole() {
        users.saveAndFlush(user("admin1", Role.ROLE_ADMIN));
        User second = user("admin2", Role.ROLE_ADMIN);
        second.deactivate();
        users.saveAndFlush(second);

        assertThat(users.countByRoleAndActive(Role.ROLE_ADMIN, true)).isEqualTo(1);
        assertThat(users.countByRoleAndActive(Role.ROLE_ADMIN, false)).isEqualTo(1);
    }
}
