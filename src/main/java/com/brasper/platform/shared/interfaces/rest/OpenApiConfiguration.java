package com.brasper.platform.shared.interfaces.rest;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at {@code /swagger-ui.html}, spec at {@code /v3/api-docs}. Sign in with
 * {@code POST /api/v1/auth/sign-in}, then paste the token in "Authorize".
 */
@Configuration
public class OpenApiConfiguration {

    static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI brasperOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Brasper Platform API").version("v1")
                        .description("Backend of the Brasper financial platform."))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
