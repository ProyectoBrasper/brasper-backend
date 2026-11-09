package com.brasper.platform.iam.interfaces.rest.resources;

public record UserResource(Long id, String username, String fullName, String role, boolean active) {
}
