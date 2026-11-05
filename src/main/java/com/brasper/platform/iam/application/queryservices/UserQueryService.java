package com.brasper.platform.iam.application.queryservices;

import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository users;

    public UserQueryService(UserRepository users) {
        this.users = users;
    }

    public List<User> findAll() {
        return users.findAll();
    }

    public Optional<User> findById(Long id) {
        return users.findById(id);
    }
}
