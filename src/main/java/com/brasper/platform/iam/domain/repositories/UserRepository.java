package com.brasper.platform.iam.domain.repositories;

import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.model.valueobjects.Role;
import com.brasper.platform.iam.domain.model.valueobjects.Username;
import java.util.List;
import java.util.Optional;

/** Port used by the application layer. Implemented by Spring Data in {@code infrastructure}. */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(Username username);

    boolean existsByUsername(Username username);

    long countByRoleAndActive(Role role, boolean active);

    List<User> findAll();
}
