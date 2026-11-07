package com.brasper.platform.reporting.interfaces.rest;

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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ProjectProfitabilityControllerTest {

    private static final String PASSWORD = "clave1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private ClientRepository clients;
    @Autowired private ProjectRepository projects;
    @Autowired private HashingService hashing;

    private String bearer(Role role) throws Exception {
        String username = role.name().toLowerCase().replace("role_", "");
        users.save(new User(new SignUpCommand(new Username(username), PASSWORD, "Full " + username, role),
                hashing.encode(PASSWORD)));
        String body = mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private Long project() {
        Long clientId = clients.save(new Client(
                new CreateClientCommand("Linings", new Ruc("20123456789"), null, null, null))).getId();
        return projects.save(new Project(new CreateProjectCommand(new ProjectCode("862BR-AGO-2026"), "Planta", null,
                clientId, new Money(new BigDecimal("1000"), Currency.PEN), LocalDate.of(2026, 8, 3),
                new Percentage(BigDecimal.ONE), new Percentage(new BigDecimal("0.03"))))).getId();
    }

    @Test
    void reportsTheProjectProfitabilityAndTheWholePortfolio() throws Exception {
        String token = bearer(Role.ROLE_MANAGEMENT);
        Long id = project();

        mockMvc.perform(get("/api/v1/reports/projects/{id}/profitability?year=2026&month=8", id)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("862BR-AGO-2026"))
                .andExpect(jsonPath("$.currency").value("PEN"))
                .andExpect(jsonPath("$.salesCommission").value(35.4))
                .andExpect(jsonPath("$.grossProfit").value(964.6))
                .andExpect(jsonPath("$.actualCost").value(0));
        mockMvc.perform(get("/api/v1/reports/projects/profitability?year=2026&month=8").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.projectId == %d)]".formatted(id)).isNotEmpty());
    }

    @Test
    void rejectsUnknownProjectsBadPeriodsAndUnauthorizedRoles() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);
        mockMvc.perform(get("/api/v1/reports/projects/999999/profitability?year=2026&month=8")
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("reporting.error.project-not-found"));
        mockMvc.perform(get("/api/v1/reports/projects/profitability?year=2026&month=13").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/reports/projects/profitability?year=2026&month=8")
                        .header("Authorization", bearer(Role.ROLE_ENGINEER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reports/projects/profitability?year=2026&month=8"))
                .andExpect(status().isUnauthorized());
    }
}
