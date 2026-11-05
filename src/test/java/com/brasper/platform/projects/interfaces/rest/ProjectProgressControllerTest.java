package com.brasper.platform.projects.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.projects.domain.model.commands.CreateClientCommand;
import com.brasper.platform.projects.domain.model.commands.CreateProjectCommand;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectCode;
import com.brasper.platform.projects.domain.repositories.ClientRepository;
import com.brasper.platform.projects.domain.repositories.ProjectRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class ProjectProgressControllerTest {

    private static final String PASSWORD = "clave1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private ClientRepository clients;
    @Autowired
    private ProjectRepository projects;
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

    private Long project(boolean started) {
        Long clientId = clients.save(new Client(
                new CreateClientCommand("Linings", new Ruc("20123456789"), null, null, null))).getId();
        Project project = new Project(new CreateProjectCommand(new ProjectCode("862BR-AGO-2026"), "Planta", null,
                clientId, new Money(BigDecimal.TEN, Currency.USD), LocalDate.of(2026, 8, 3),
                new Percentage(BigDecimal.ONE), new Percentage(BigDecimal.ZERO)));
        if (started) {
            project.start();
        }
        return projects.save(project).getId();
    }

    private ResultActions register(String token, Long projectId, int month, String percentage) throws Exception {
        return mockMvc.perform(post("/api/v1/projects/" + projectId + "/progress").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"year\":2026,\"month\":%d,\"percentage\":%s}".formatted(month, percentage)));
    }

    @Test
    void registersProgressListsItAndCompletesTheProject() throws Exception {
        String token = bearer(Role.ROLE_ENGINEER);
        Long id = project(true);

        register(token, id, 8, "0.6").andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.month").value(8))
                .andExpect(jsonPath("$.percentage").value(0.6));
        mockMvc.perform(get("/api/v1/projects/" + id).header("Authorization", token))
                .andExpect(jsonPath("$.accumulatedProgress").value(0.6))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        register(token, id, 9, "0.4").andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/projects/" + id).header("Authorization", token))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mockMvc.perform(get("/api/v1/projects/" + id + "/progress").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].month").value(8));
    }

    @Test
    void rejectsBrokenRulesWithTheMatchingStatus() throws Exception {
        String token = bearer(Role.ROLE_ENGINEER);
        Long id = project(true);

        register(token, id, 8, "0.7").andExpect(status().isCreated());
        register(token, id, 8, "0.1").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("projects.error.period-already-registered"));
        register(token, id, 9, "0.31").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("projects.error.progress-exceeds-limit"));
        register(token, id, 13, "0.1").andExpect(status().isBadRequest());
        register(token, id, 9, "0").andExpect(status().isBadRequest());
        register(token, 999_999L, 9, "0.1").andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/projects/999999/progress").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void aProjectNotInProgressRejectsProgressAndManagementCannotRegister() throws Exception {
        Long quoted = project(false);

        register(bearer(Role.ROLE_ACCOUNTING), quoted, 8, "0.1").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("projects.error.invalid-status-transition"));
        register(bearer(Role.ROLE_MANAGEMENT), quoted, 8, "0.1").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects/" + quoted + "/progress")).andExpect(status().isUnauthorized());
    }
}
