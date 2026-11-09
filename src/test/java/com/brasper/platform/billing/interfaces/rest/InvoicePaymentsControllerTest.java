package com.brasper.platform.billing.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.commands.IssueInvoiceCommand;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import com.brasper.platform.iam.application.outboundservices.HashingService;
import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.Role;
import com.brasper.platform.iam.domain.model.valueobjects.Username;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
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
class InvoicePaymentsControllerTest {

    private static final String PASSWORD = "clave1234";
    private static final String DEPOSIT = "\"type\":\"NET_DEPOSIT\",\"bankAccountId\":9,\"reference\":\"OP-1\"";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository users;
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

    /** A PEN invoice of 1180 (base 1000 plus IGV). */
    private Long invoice() {
        return invoices.save(new Invoice(new IssueInvoiceCommand(new InvoiceNumber("F001", 7),
                LocalDate.of(2026, 8, 20), null, 3L, new Money(new BigDecimal("1000"), Currency.PEN), false, false,
                false, false, null), 4L, TaxRates.standard())).getId();
    }

    private ResultActions pay(String token, Object invoiceId, String amount, String currency, String typeAndAccount)
            throws Exception {
        return mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/payments").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(("{\"receivedOn\":\"2026-09-01\",\"amount\":%s,\"currency\":\"%s\",%s}")
                        .formatted(amount, currency, typeAndAccount)));
    }

    @Test
    void registersPaymentsListsThemAndTheInvoiceFollows() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);
        Long id = invoice();

        pay(token, id, "400", "PEN", DEPOSIT).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.type").value("NET_DEPOSIT"))
                .andExpect(jsonPath("$.reference").value("OP-1"));
        mockMvc.perform(get("/api/v1/invoices/" + id).header("Authorization", token))
                .andExpect(jsonPath("$.status").value("PARTIALLY_COLLECTED"))
                .andExpect(jsonPath("$.collectedAmount").value(400))
                .andExpect(jsonPath("$.outstandingBalance").value(780));

        pay(token, id, "780", "PEN", "\"type\":\"RETENTION_CERTIFICATE\"").andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/invoices/" + id).header("Authorization", token))
                .andExpect(jsonPath("$.status").value("COLLECTED"));
        mockMvc.perform(get("/api/v1/invoices/" + id + "/payments").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsBrokenRulesWithTheMatchingStatus() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);
        Long id = invoice();

        pay(token, id, "1180.01", "PEN", DEPOSIT).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("billing.error.amount-exceeds-balance"));
        pay(token, id, "10", "USD", DEPOSIT).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("billing.error.payment-currency-mismatch"));
        pay(token, id, "10", "PEN", "\"type\":\"NET_DEPOSIT\"").andExpect(status().isBadRequest());
        pay(token, id, "0", "PEN", DEPOSIT).andExpect(status().isBadRequest());
        pay(token, 999_999L, "10", "PEN", DEPOSIT).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/invoices/999999/payments").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void aVoidedInvoiceTakesNoPaymentsAndOnlyAdminOrAccountingMayRegister() throws Exception {
        Long id = invoice();
        mockMvc.perform(post("/api/v1/invoices/" + id + "/void").header("Authorization", bearer(Role.ROLE_ADMIN)))
                .andExpect(status().isOk());

        pay(bearer(Role.ROLE_ACCOUNTING), id, "10", "PEN", DEPOSIT).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("billing.error.already-voided"));
        pay(bearer(Role.ROLE_ENGINEER), id, "10", "PEN", DEPOSIT).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/invoices/" + id + "/payments")).andExpect(status().isUnauthorized());
    }
}
