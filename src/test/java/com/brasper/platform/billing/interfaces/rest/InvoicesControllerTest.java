package com.brasper.platform.billing.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class InvoicesControllerTest {

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

    private Long clientId;

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
        if (clientId == null) {
            clientId = clients.save(new Client(
                    new CreateClientCommand("Linings", new Ruc("20123456789"), null, null, null))).getId();
        }
        Project project = new Project(new CreateProjectCommand(new ProjectCode("862BR-AGO-2026"), "Planta", null,
                clientId, new Money(BigDecimal.TEN, Currency.PEN), LocalDate.of(2026, 8, 3),
                new Percentage(BigDecimal.ONE), new Percentage(BigDecimal.ZERO)));
        if (started) {
            project.start();
        }
        return projects.save(project).getId();
    }

    private ResultActions issue(String token, Long projectId, int correlative, String extra) throws Exception {
        return mockMvc.perform(post("/api/v1/invoices").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(("{\"series\":\"f001\",\"correlative\":%d,\"issuedOn\":\"2026-08-20\",\"projectId\":%d,"
                        + "\"taxableBase\":1000,\"currency\":\"PEN\"%s}").formatted(correlative, projectId, extra)));
    }

    @Test
    void issuesAnInvoiceComputingTaxesForTheProjectsClientThenReadsItBack() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);
        Long projectId = project(true);

        String body = issue(token, projectId, 7, ",\"hasRetention\":true,\"hasDetraction\":true,\"purchaseOrder\":\"OC-1\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value("F001-00000007"))
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.igv").value(180))
                .andExpect(jsonPath("$.total").value(1180))
                .andExpect(jsonPath("$.retention").value(35.4))
                .andExpect(jsonPath("$.detraction").value(142))
                .andExpect(jsonPath("$.netAmount").value(1002.6))
                .andExpect(jsonPath("$.outstandingBalance").value(1180))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        Object id = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/v1/invoices/" + id).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.purchaseOrder").value("OC-1"));
        mockMvc.perform(get("/api/v1/invoices?status=PENDING&projectId=" + projectId + "&year=2026&month=8")
                        .header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/v1/invoices?year=2026&month=9").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/invoices?year=2026").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pagesTheListAndReportsTheTotalInAHeader() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);
        Long projectId = project(true);
        for (int correlative = 1; correlative <= 3; correlative++) {
            issue(token, projectId, correlative, "").andExpect(status().isCreated());
        }
        String url = "/api/v1/invoices?projectId=" + projectId;

        mockMvc.perform(get(url + "&page=1&size=2").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].number").value("F001-00000001"));
        mockMvc.perform(get(url).header("Authorization", token))
                .andExpect(header().doesNotExist("X-Total-Count"))
                .andExpect(jsonPath("$.length()").value(3));
        mockMvc.perform(get(url + "&page=0").header("Authorization", token)).andExpect(status().isBadRequest());
        mockMvc.perform(get(url + "&page=0&size=0").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(url + "&page=-1&size=5").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(url + "&page=0&size=201").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsBrokenRulesWithTheMatchingStatus() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);
        Long started = project(true);

        issue(token, started, 7, "").andExpect(status().isCreated());
        issue(token, started, 7, "").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("billing.error.duplicate-number"));
        issue(token, 999_999L, 8, "").andExpect(status().isNotFound());
        issue(token, started, 9, ",\"selfDetraction\":true").andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/invoices").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"series\":\"F1\",\"correlative\":1,\"issuedOn\":\"2026-08-20\",\"projectId\":1,"
                                + "\"taxableBase\":10,\"currency\":\"PEN\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/invoices/999999").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void aQuotedProjectCannotBeInvoiced() throws Exception {
        issue(bearer(Role.ROLE_ADMIN), project(false), 1, "").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("billing.error.project-not-billable"));
    }

    @Test
    void voidsAnInvoiceOnceAndOnlyAdminOrAccountingMayWrite() throws Exception {
        String accounting = bearer(Role.ROLE_ACCOUNTING);
        Long projectId = project(true);
        Object id = JsonPath.read(issue(accounting, projectId, 7, "").andReturn().getResponse().getContentAsString(),
                "$.id");

        mockMvc.perform(post("/api/v1/invoices/" + id + "/void").header("Authorization", bearer(Role.ROLE_ENGINEER)))
                .andExpect(status().isForbidden());
        issue(bearer(Role.ROLE_MANAGEMENT), projectId, 8, "").andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/invoices/" + id + "/void").header("Authorization", accounting))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VOIDED"));
        mockMvc.perform(post("/api/v1/invoices/" + id + "/void").header("Authorization", accounting))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("billing.error.already-voided"));
        mockMvc.perform(post("/api/v1/invoices/999999/void").header("Authorization", accounting))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/invoices")).andExpect(status().isUnauthorized());
    }
}
