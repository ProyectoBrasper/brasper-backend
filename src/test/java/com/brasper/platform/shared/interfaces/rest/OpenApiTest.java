package com.brasper.platform.shared.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brasper.platform.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void thePublicSpecListsEveryContextAndTheBearerScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Brasper Platform API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/sign-in']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/projects']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/invoices']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/scheduled-collections']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/suppliers']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/expenses']").exists());
    }

    @Test
    void swaggerUiIsReachableWithoutAToken() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
