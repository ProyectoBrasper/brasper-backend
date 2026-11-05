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
class ProjectCostEstimatesControllerTest {

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

    private Long project(boolean cancelled) {
        Long clientId = clients.save(new Client(
                new CreateClientCommand("Linings", new Ruc("20123456789"), null, null, null))).getId();
        Project project = new Project(new CreateProjectCommand(new ProjectCode("862BR-AGO-2026"), "Planta", null,
                clientId, new Money(BigDecimal.TEN, Currency.USD), LocalDate.of(2026, 8, 3),
                new Percentage(BigDecimal.ONE), new Percentage(BigDecimal.ZERO)));
        if (cancelled) {
            project.cancel();
        }
        return projects.save(project).getId();
    }

    private ResultActions set(String token, Long projectId, String category, String amount, String currency)
            throws Exception {
        return mockMvc.perform(put("/api/v1/projects/" + projectId + "/cost-estimates")
                .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"%s\",\"estimatedAmount\":%s,\"currency\":\"%s\"}"
                        .formatted(category, amount, currency)));
    }

    @Test
    void setsReplacesAndListsEstimatesAndTheProjectShowsTheirSum() throws Exception {
        String token = bearer(Role.ROLE_ENGINEER);
        Long id = project(false);

        set(token, id, "MATERIALS", "300", "USD").andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("MATERIALS"))
                .andExpect(jsonPath("$.estimatedAmount").value(300));
        set(token, id, "LABOR", "150.50", "USD").andExpect(status().isOk());
        set(token, id, "MATERIALS", "400", "USD").andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedAmount").value(400));

        mockMvc.perform(get("/api/v1/projects/" + id + "/cost-estimates").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].category").value("MATERIALS"));
        mockMvc.perform(get("/api/v1/projects/" + id).header("Authorization", token))
                .andExpect(jsonPath("$.estimatedDirectCost").value(550.5));
    }

    @Test
    void rejectsBrokenRulesWithTheMatchingStatus() throws Exception {
        String token = bearer(Role.ROLE_ENGINEER);
        Long id = project(false);

        set(token, id, "OTHER", "10", "PEN").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("projects.error.cost-currency-mismatch"));
        set(token, id, "OTHER", "-1", "USD").andExpect(status().isBadRequest());
        set(token, id, "BOGUS", "1", "USD").andExpect(status().isBadRequest());
        set(token, 999_999L, "OTHER", "1", "USD").andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/projects/999999/cost-estimates").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void aCancelledProjectRejectsEstimatesAndManagementCannotSet() throws Exception {
        Long cancelled = project(true);

        set(bearer(Role.ROLE_ACCOUNTING), cancelled, "OTHER", "1", "USD").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("projects.error.invalid-status-transition"));
        set(bearer(Role.ROLE_MANAGEMENT), cancelled, "OTHER", "1", "USD").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects/" + cancelled + "/cost-estimates"))
                .andExpect(status().isUnauthorized());
    }
}
