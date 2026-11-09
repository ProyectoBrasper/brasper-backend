package com.brasper.platform.projects.interfaces.rest.resources;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientResource(
        @NotBlank @Size(max = 200) String businessName,
        @NotBlank String ruc,
        @Size(max = 150) String contactName,
        @Size(max = 30) String phone,
        @Email @Size(max = 150) String email) {
}
