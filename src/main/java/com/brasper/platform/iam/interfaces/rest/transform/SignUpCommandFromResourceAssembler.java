package com.brasper.platform.iam.interfaces.rest.transform;

import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.Username;
import com.brasper.platform.iam.interfaces.rest.resources.SignUpResource;

public final class SignUpCommandFromResourceAssembler {

    private SignUpCommandFromResourceAssembler() {
    }

    /** @throws IllegalArgumentException if the username is malformed (mapped to 400 by the global handler). */
    public static SignUpCommand toCommandFromResource(SignUpResource resource) {
        return new SignUpCommand(new Username(resource.username()), resource.password(),
                resource.fullName().strip(), resource.role());
    }
}
