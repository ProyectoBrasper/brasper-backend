package com.brasper.platform.iam.interfaces.rest.transform;

import com.brasper.platform.iam.application.commandservices.AuthenticatedUser;
import com.brasper.platform.iam.interfaces.rest.resources.AuthenticatedUserResource;

public final class AuthenticatedUserResourceFromEntityAssembler {

    private AuthenticatedUserResourceFromEntityAssembler() {
    }

    public static AuthenticatedUserResource toResourceFromEntity(AuthenticatedUser authenticated) {
        var user = authenticated.user();
        return new AuthenticatedUserResource(user.getId(), user.getUsername().value(), user.getFullName(),
                user.getRole().name(), authenticated.token());
    }
}
