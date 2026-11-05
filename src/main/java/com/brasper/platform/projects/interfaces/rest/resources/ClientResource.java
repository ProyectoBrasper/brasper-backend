package com.brasper.platform.projects.interfaces.rest.resources;

public record ClientResource(Long id, String businessName, String ruc, String contactName, String phone,
        String email) {
}
