package com.brasper.platform.iam.interfaces.rest.resources;

public record AuthenticatedUserResource(Long id, String username, String fullName, String role, String token) {
}
