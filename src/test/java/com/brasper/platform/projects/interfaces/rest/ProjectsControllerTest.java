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
import com.brasper.platform.projects.domain.model.aggregates.Client;
import com.brasper.platform.projects.domain.model.commands.CreateClientCommand;
import com.brasper.platform.projects.domain.repositories.ClientRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
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
class ProjectsControllerTest {

    private static final String PASSWORD = "clave1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private ClientRepository clients;
    @Autowired
    private HashingService hashing;

    private String bearer(Role role) throws Exception {
        String username = role.name().toLowerCase().replace("role_", "");
        users.save(new User(new SignUpCommand(new Username(username), PASSWORD, "Full " + username, role),
                hashing.encode(PASSWORD)));
        String body = mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private Long clientId() {
        return clients.save(new Client(new CreateClientCommand("Linings", new Ruc("20123456789"), null, null, null)))
                .getId();
    }

    private ResultActions createProject(String token, String code, Long clientId) throws Exception {
        return mockMvc.perform(post("/api/v1/projects").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"%s","name":"Planta","clientId":%d,"budget":1000.50,"currency":"USD",
                         "quotedOn":"2026-08-03","participation":0.5,"salesCommissionRate":0.03}
                        """.formatted(code, clientId)));
    }

    private int createdId(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    @Test
    void engineerCreatesReadsAndFiltersProjects() throws Exception {
        String token = bearer(Role.ROLE_ENGINEER);
        Long clientId = clientId();

        int id = createdId(createProject(token, "862br-ago-2026", clientId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("862BR-AGO-2026"))
                .andExpect(jsonPath("$.status").value("QUOTED"))
                .andExpect(jsonPath("$.budget").value(1000.50))
                .andExpect(jsonPath("$.currency").value("USD")));

        mockMvc.perform(get("/api/v1/projects/" + id).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.clientId").value(clientId));
        mockMvc.perform(get("/api/v1/projects?status=QUOTED&clientId=" + clientId).header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/v1/projects?status=CANCELLED").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/projects?status=NOPE").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStartAndCancelFollowTheStatusRules() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);
        int id = createdId(createProject(token, "862BR-AGO-2026", clientId()));

        mockMvc.perform(put("/api/v1/projects/" + id).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nuevo\",\"budget\":2000,\"participation\":1,\"salesCommissionRate\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nuevo"))
                .andExpect(jsonPath("$.currency").value("USD"));
        mockMvc.perform(post("/api/v1/projects/" + id + "/start").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(post("/api/v1/projects/" + id + "/start").header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("projects.error.invalid-status-transition"));
        mockMvc.perform(post("/api/v1/projects/" + id + "/cancel").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void rejectsDuplicatesUnknownClientsAndInvalidInput() throws Exception {
        String token = bearer(Role.ROLE_ENGINEER);
        Long clientId = clientId();

        createProject(token, "862BR-AGO-2026", clientId).andExpect(status().isCreated());
        createProject(token, "862BR-AGO-2026", clientId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("projects.error.duplicate-code"));
        createProject(token, "001BR-ENE-2027", 999_999L).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("projects.error.client-not-found"));
        createProject(token, "8789R-OCT-2026", clientId).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/projects").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/projects/999999").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void managementReadsButCannotWrite() throws Exception {
        String token = bearer(Role.ROLE_MANAGEMENT);

        mockMvc.perform(get("/api/v1/projects").header("Authorization", token)).andExpect(status().isOk());
        createProject(token, "862BR-AGO-2026", 1L).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/projects/1/start").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects")).andExpect(status().isUnauthorized());
    }
}
