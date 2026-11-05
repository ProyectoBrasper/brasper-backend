package com.brasper.platform.iam.interfaces.rest.transform;

import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.interfaces.rest.resources.UserResource;

public final class UserResourceFromEntityAssembler {

    private UserResourceFromEntityAssembler() {
    }

    public static UserResource toResourceFromEntity(User user) {
        return new UserResource(user.getId(), user.getUsername().value(), user.getFullName(),
                user.getRole().name(), user.isActive());
    }
}
