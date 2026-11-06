package com.brasper.platform.expenses.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.expenses.domain.model.aggregates.Supplier;
import com.brasper.platform.expenses.domain.model.commands.CreateSupplierCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.SupplierType;
import com.brasper.platform.expenses.domain.repositories.SupplierRepository;
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
class ExpensesControllerTest {

    private static final String PASSWORD = "clave1234";
    private static final String BANK = "\"paymentMethod\":\"BANK_ACCOUNT\",\"bankAccountId\":9,\"paid\":true";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private ClientRepository clients;
    @Autowired
    private ProjectRepository projects;
    @Autowired
    private SupplierRepository suppliers;
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

    private Long project(String code, boolean cancelled) {
        Long clientId = clients.findAll().stream().findFirst().map(Client::getId).orElseGet(() -> clients.save(
                new Client(new CreateClientCommand("Linings", new Ruc("20123456789"), null, null, null))).getId());
        Project project = new Project(new CreateProjectCommand(new ProjectCode(code), "Planta", null, clientId,
                new Money(BigDecimal.TEN, Currency.PEN), LocalDate.of(2026, 8, 3), new Percentage(BigDecimal.ONE),
                new Percentage(BigDecimal.ZERO)));
        if (cancelled) {
            project.cancel();
        }
        return projects.save(project).getId();
    }

    private Long supplier() {
        return suppliers.save(new Supplier(new CreateSupplierCommand("Juan", null, SupplierType.NATURAL_PERSON,
                null))).getId();
    }

    private ResultActions register(String token, Object projectId, Object supplierId, String voucher, String amount,
            String payment) throws Exception {
        return mockMvc.perform(post("/api/v1/expenses").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(("{\"spentOn\":\"2026-08-20\",\"projectId\":%s,\"supplierId\":%s,\"category\":\"MATERIALS\","
                        + "\"description\":\"cables\",%s,\"amount\":%s,\"currency\":\"PEN\",%s}")
                        .formatted(projectId, supplierId, voucher, amount, payment)));
    }

    private static final String INVOICE = "\"voucherType\":\"INVOICE\",\"voucherNumber\":\"F001-1\"";

    @Test
    void registersAnInvoiceCorrectsAnnulsAndLists() throws Exception {
        String token = bearer(Role.ROLE_ENGINEER);
        Long projectId = project("862BR-AGO-2026", false);
        Long otherProjectId = project("863BR-AGO-2026", false);

        String body = register(token, projectId, supplier(), INVOICE, "1000", BANK)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taxableBase").value(1000))
                .andExpect(jsonPath("$.igv").value(180))
                .andExpect(jsonPath("$.total").value(1180))
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.annulled").value(false))
                .andReturn().getResponse().getContentAsString();
        Object id = JsonPath.read(body, "$.id");

        mockMvc.perform(put("/api/v1/expenses/" + id).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":%d,\"category\":\"LABOR\",\"description\":\"jornal\"}"
                                .formatted(otherProjectId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.category").value("LABOR"))
                .andExpect(jsonPath("$.projectId").value(otherProjectId));

        mockMvc.perform(post("/api/v1/expenses/" + id + "/annul").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.annulled").value(true));
        mockMvc.perform(post("/api/v1/expenses/" + id + "/annul").header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("expenses.error.already-annulled"));
    }

    @Test
    void aReceiptHasNoIgvAndAGeneralExpenseNeedsNoProject() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);

        register(token, "null", "null", "\"voucherType\":\"SALES_RECEIPT\",\"voucherNumber\":\"B001-9\"", "500",
                "\"paymentMethod\":\"PARTNER_CONTRIBUTION\",\"partnerId\":7,\"paid\":false,\"amountIncludesIgv\":true")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.igv").value(0))
                .andExpect(jsonPath("$.total").value(500))
                .andExpect(jsonPath("$.projectId").doesNotExist())
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
    }

    @Test
    void mapsEachFailureToItsStatus() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);
        Long projectId = project("862BR-AGO-2026", false);
        Long cancelledId = project("863BR-AGO-2026", true);

        register(token, 999999, "null", INVOICE, "100", BANK).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("expenses.error.project-not-found"));
        register(token, cancelledId, "null", INVOICE, "100", BANK).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("expenses.error.project-closed"));
        register(token, projectId, 999999, INVOICE, "100", BANK).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("expenses.error.supplier-not-found"));
        register(token, projectId, "null", INVOICE, "100", "\"paymentMethod\":\"BANK_ACCOUNT\",\"paid\":true")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("expenses.error.bank-account-required"));
        register(token, projectId, "null", INVOICE, "100", "\"paymentMethod\":\"PARTNER_CONTRIBUTION\",\"paid\":true")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("expenses.error.partner-required"));
        mockMvc.perform(post("/api/v1/expenses/999999/annul").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsBadInput() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);

        register(token, "null", "null", "\"voucherType\":\"INVOICE\"", "100", BANK).andExpect(status().isBadRequest());
        register(token, "null", "null", INVOICE, "-5", BANK).andExpect(status().isBadRequest());
        register(token, "null", "null", INVOICE, "100", "\"paymentMethod\":\"BANK_ACCOUNT\",\"bankAccountId\":9")
                .andExpect(status().isBadRequest());
    }

    @Test
    void managementCannotWrite() throws Exception {
        String token = bearer(Role.ROLE_MANAGEMENT);

        register(token, "null", "null", INVOICE, "100", BANK).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/expenses/1/annul").header("Authorization", token))
                .andExpect(status().isForbidden());
    }
}
