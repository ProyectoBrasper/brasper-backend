package com.brasper.platform.iam.infrastructure.persistence.jpa;

import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data generates the implementation of the domain port from the method names. */
public interface UserJpaRepository extends JpaRepository<User, Long>, UserRepository {
}
