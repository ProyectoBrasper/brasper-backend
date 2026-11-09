package com.brasper.platform.iam.interfaces.rest;

import com.brasper.platform.iam.application.commandservices.UserCommandFailure;
import com.brasper.platform.iam.application.commandservices.UserCommandService;
import com.brasper.platform.iam.application.queryservices.UserQueryService;
import com.brasper.platform.iam.domain.model.commands.ChangeUserRoleCommand;
import com.brasper.platform.iam.domain.model.commands.DeactivateUserCommand;
import com.brasper.platform.iam.domain.model.commands.ReactivateUserCommand;
import com.brasper.platform.iam.domain.model.commands.ResetUserPasswordCommand;
import com.brasper.platform.iam.interfaces.rest.resources.ChangeRoleResource;
import com.brasper.platform.iam.interfaces.rest.resources.ResetPasswordResource;
import com.brasper.platform.iam.interfaces.rest.transform.UserFailureResponseAssembler;
import com.brasper.platform.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UsersController {

    private final UserCommandService userCommands;
    private final UserQueryService userQueries;
    private final MessageSource messages;

    public UsersController(UserCommandService userCommands, UserQueryService userQueries, MessageSource messages) {
        this.userCommands = userCommands;
        this.userQueries = userQueries;
        this.messages = messages;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(userQueries.findAll().stream()
                .map(UserResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return userQueries.findById(id)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(
                        UserResourceFromEntityAssembler.toResourceFromEntity(user)))
                .orElseGet(() -> UserFailureResponseAssembler.toResponse(UserCommandFailure.USER_NOT_FOUND, messages));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleResource resource) {
        return userCommands.handle(new ChangeUserRoleCommand(id, resource.role())).fold(
                user -> ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user)),
                failure -> UserFailureResponseAssembler.toResponse(failure, messages));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        return userCommands.handle(new DeactivateUserCommand(id)).fold(
                user -> ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user)),
                failure -> UserFailureResponseAssembler.toResponse(failure, messages));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordResource resource) {
        return userCommands.handle(new ResetUserPasswordCommand(id, resource.newPassword())).fold(
                user -> ResponseEntity.noContent().build(),
                failure -> UserFailureResponseAssembler.toResponse(failure, messages));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Long id) {
        return userCommands.handle(new ReactivateUserCommand(id)).fold(
                user -> ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user)),
                failure -> UserFailureResponseAssembler.toResponse(failure, messages));
    }
}
