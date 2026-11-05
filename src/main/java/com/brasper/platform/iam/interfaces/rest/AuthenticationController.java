package com.brasper.platform.iam.interfaces.rest;

import com.brasper.platform.iam.application.commandservices.UserCommandFailure;
import com.brasper.platform.iam.application.commandservices.UserCommandService;
import com.brasper.platform.iam.domain.model.commands.SignInCommand;
import com.brasper.platform.iam.domain.model.valueobjects.Username;
import com.brasper.platform.iam.interfaces.rest.resources.SignInResource;
import com.brasper.platform.iam.interfaces.rest.resources.SignUpResource;
import com.brasper.platform.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import com.brasper.platform.iam.interfaces.rest.transform.SignUpCommandFromResourceAssembler;
import com.brasper.platform.iam.interfaces.rest.transform.UserFailureResponseAssembler;
import com.brasper.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final UserCommandService userCommands;
    private final MessageSource messages;

    public AuthenticationController(UserCommandService userCommands, MessageSource messages) {
        this.userCommands = userCommands;
        this.messages = messages;
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInResource resource) {
        Username username;
        try {
            username = new Username(resource.username());
        } catch (IllegalArgumentException e) {
            // A malformed name cannot exist; answer like any wrong login instead of describing the name rules.
            return UserFailureResponseAssembler.toResponse(UserCommandFailure.INVALID_CREDENTIALS, messages);
        }
        return userCommands.handle(new SignInCommand(username, resource.password())).fold(
                authenticated -> ResponseEntity.ok(
                        AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(authenticated)),
                failure -> UserFailureResponseAssembler.toResponse(failure, messages));
    }

    @PostMapping("/sign-up")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpResource resource) {
        return userCommands.handle(SignUpCommandFromResourceAssembler.toCommandFromResource(resource)).fold(
                user -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(UserResourceFromEntityAssembler.toResourceFromEntity(user)),
                failure -> UserFailureResponseAssembler.toResponse(failure, messages));
    }
}
