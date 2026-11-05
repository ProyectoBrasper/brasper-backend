package com.brasper.platform.projects.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.iam.application.outboundservices.HashingService;
import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.Role;
import com.brasper.platform.iam.domain.model.valueobjects.Username;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ClientsControllerTest {

    private static final String PASSWORD = "clave1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private HashingService hashing;

    private String bearer(String username, Role role) throws Exception {
        users.save(new User(new SignUpCommand(new Username(username), PASSWORD, "Full " + username, role),
                hashing.encode(PASSWORD)));
        String body = mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private ResultActions createClient(String token, String ruc) throws Exception {
        return mockMvc.perform(post("/api/v1/clients").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessName\":\"Linings SAC\",\"ruc\":\"%s\",\"email\":\"a@b.pe\"}".formatted(ruc)));
    }

    @Test
    void engineerCreatesUpdatesAndReadsAClient() throws Exception {
        String token = bearer("ana", Role.ROLE_ENGINEER);

        String body = createClient(token, "20123456789")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruc").value("20123456789"))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");

        mockMvc.perform(put("/api/v1/clients/" + id).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessName\":\"Linings Perú\",\"contactName\":\"Luis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Linings Perú"))
                .andExpect(jsonPath("$.ruc").value("20123456789"));
        mockMvc.perform(get("/api/v1/clients/" + id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactName").value("Luis"));
        mockMvc.perform(get("/api/v1/clients").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void managementReadsButCannotWrite() throws Exception {
        String token = bearer("gerente", Role.ROLE_MANAGEMENT);

        mockMvc.perform(get("/api/v1/clients").header("Authorization", token)).andExpect(status().isOk());
        createClient(token, "20123456789").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/clients")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicateRucBadRucAndUnknownClient() throws Exception {
        String token = bearer("ana", Role.ROLE_ACCOUNTING);

        createClient(token, "20123456789").andExpect(status().isCreated());
        createClient(token, "20123456789").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("projects.error.duplicate-ruc"));
        createClient(token, "123").andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/clients/999999").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("projects.error.client-not-found"));
        mockMvc.perform(post("/api/v1/clients").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"businessName\":\"\",\"ruc\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
