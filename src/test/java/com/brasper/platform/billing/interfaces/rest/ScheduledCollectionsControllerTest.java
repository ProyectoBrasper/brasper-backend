package com.brasper.platform.billing.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.billing.domain.model.aggregates.ScheduledCollection;
import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.commands.IssueInvoiceCommand;
import com.brasper.platform.billing.domain.model.commands.ScheduleCollectionCommand;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import com.brasper.platform.billing.domain.repositories.ScheduledCollectionRepository;
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
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
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
class ScheduledCollectionsControllerTest {

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
    private ScheduledCollectionRepository collections;
    @Autowired
    private InvoiceRepository invoices;
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

    /** A just-quoted project: scheduling does not need it to be started. */
    private Long project() {
        Long clientId = clients.save(new Client(
                new CreateClientCommand("Linings", new Ruc("20123456789"), null, null, null))).getId();
        return projects.save(new Project(new CreateProjectCommand(new ProjectCode("862BR-AGO-2026"), "Planta", null,
                clientId, new Money(BigDecimal.TEN, Currency.PEN), LocalDate.of(2026, 8, 3),
                new Percentage(BigDecimal.ONE), new Percentage(BigDecimal.ZERO)))).getId();
    }

    private ResultActions schedule(String token, Object projectId, String expectedOn) throws Exception {
        return mockMvc.perform(post("/api/v1/scheduled-collections").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"projectId\":%s,\"expectedAmount\":500,\"currency\":\"PEN\",\"expectedOn\":\"%s\"}"
                        .formatted(projectId, expectedOn)));
    }

    private ResultActions action(String token, Object id, String action, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/scheduled-collections/" + id + "/" + action)
                .header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void schedulesReschedulesCancelsAndListsByMonth() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);
        Long projectId = project();

        // Aug 1 2026 is a Saturday, so week 1 is Aug 1-2 and Aug 20 falls in week 4.
        String body = schedule(token, projectId, "2026-08-20")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(8))
                .andExpect(jsonPath("$.week").value(4))
                .andExpect(jsonPath("$.expectedAmount").value(500))
                .andReturn().getResponse().getContentAsString();
        Object id = JsonPath.read(body, "$.id");

        action(token, id, "reschedule", "{\"expectedOn\":\"2026-09-02\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESCHEDULED"))
                .andExpect(jsonPath("$.month").value(9));

        mockMvc.perform(get("/api/v1/scheduled-collections?year=2026&month=9").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/v1/scheduled-collections?year=2026&month=8").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(0));

        action(token, id, "cancel", "")
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        action(token, id, "cancel", "")
                .andExpect(status().isConflict());
        action(token, id, "reschedule", "{\"expectedOn\":\"2026-09-09\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void aRealizedCollectionCannotBeChanged() throws Exception {
        String token = bearer(Role.ROLE_ADMIN);
        ScheduledCollection collection = new ScheduledCollection(new ScheduleCollectionCommand(3L,
                new Money(new BigDecimal("500"), Currency.PEN), CashFlowWeek.from(LocalDate.of(2026, 8, 20)), null));
        Long invoiceId = invoices.save(new Invoice(new IssueInvoiceCommand(new InvoiceNumber("F001", 7),
                LocalDate.of(2026, 8, 20), null, 3L, new Money(new BigDecimal("1000"), Currency.PEN), false, false,
                false, false, null), 4L, TaxRates.standard())).getId();
        collection.markAsRealized(invoiceId);
        Long id = collections.save(collection).getId();

        action(token, id, "cancel", "").andExpect(status().isConflict());
    }

    @Test
    void unknownProjectOrCollectionIsNotFound() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);

        schedule(token, 999999, "2026-08-20").andExpect(status().isNotFound());
        action(token, 999999, "cancel", "").andExpect(status().isNotFound());
        action(token, 999999, "reschedule", "{\"expectedOn\":\"2026-09-02\"}").andExpect(status().isNotFound());
    }

    @Test
    void rejectsBadInput() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);

        mockMvc.perform(post("/api/v1/scheduled-collections").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":1,\"expectedAmount\":-5,\"currency\":\"PEN\",\"expectedOn\":\"2026-08-20\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/scheduled-collections?year=2026").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/scheduled-collections?year=2026&month=13").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdminAndAccountingWriteButAnyoneSignedInReads() throws Exception {
        String management = bearer(Role.ROLE_MANAGEMENT);

        schedule(management, 1, "2026-08-20").andExpect(status().isForbidden());
        action(management, 1, "cancel", "").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/scheduled-collections?year=2026&month=8").header("Authorization", management))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/scheduled-collections?year=2026&month=8"))
                .andExpect(status().isUnauthorized());
    }
}
