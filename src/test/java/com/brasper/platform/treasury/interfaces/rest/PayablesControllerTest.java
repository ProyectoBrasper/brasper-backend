package com.brasper.platform.treasury.interfaces.rest;

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
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandService;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
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
class PayablesControllerTest {

    private static final String PASSWORD = "clave1234";
    private static final String URL = "/api/v1/payables";
    private static final String CREATE = "{\"concept\":\"Supplier invoice\",\"type\":\"SUPPLIER\",\"totalAmount\":100,"
            + "\"currency\":\"PEN\",\"year\":2026,\"month\":11,\"week\":1}";
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private HashingService hashing;
    @Autowired
    private BankAccountCommandService accountCommands;

    private String bearer(String username, Role role) throws Exception {
        users.save(new User(new SignUpCommand(new Username(username), PASSWORD, "Full " + username, role),
                hashing.encode(PASSWORD)));
        String body = mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private ResultActions send(String token, String url, String json) throws Exception {
        return mockMvc.perform(post(url).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private int create(String token) throws Exception {
        String body = send(token, URL, CREATE).andExpect(status().isCreated())
                .andExpect(jsonPath("$.concept").value("Supplier invoice"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions pay(String token, int id, Long account, int amount) throws Exception {
        return send(token, URL + "/" + id + "/payments",
                "{\"paidOn\":\"2026-11-03\",\"amount\":%d,\"bankAccountId\":%d}".formatted(amount, account));
    }

    private ResultActions schedule(String token, int id, int month, int week) throws Exception {
        return mockMvc.perform(put(URL + "/" + id + "/schedule").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"year\":2026,\"month\":%d,\"week\":%d}".formatted(month, week)));
    }

    @Test
    void paymentsUpdateThePayableAndDebitTheAccount() throws Exception {
        String token = bearer("ana", Role.ROLE_ACCOUNTING);
        int id = create(token);
        Long account = accountCommands.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber("001-100"),
                AccountType.CHECKING, Currency.PEN, new Money(new BigDecimal("250"), Currency.PEN),
                LocalDate.of(2026, 11, 1))).success().orElseThrow().getId();
        schedule(token, id, 11, 6).andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026)).andExpect(jsonPath("$.month").value(11))
                .andExpect(jsonPath("$.week").value(6));
        pay(token, id, account, 40).andExpect(status().isCreated())
                .andExpect(jsonPath("$.paidAmount").value(40)).andExpect(jsonPath("$.balance").value(60))
                .andExpect(jsonPath("$.status").value("PARTIALLY_PAID"));
        pay(token, id, account, 61).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("treasury.error.payment-exceeds-balance"));
        pay(token, id, account, 60).andExpect(status().isCreated())
                .andExpect(jsonPath("$.paidAmount").value(100)).andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("PAID"));
        mockMvc.perform(get("/api/v1/bank-accounts/" + account).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentBalance").value(150));
        schedule(token, id, 12, 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("treasury.error.payable-already-paid"));
        mockMvc.perform(get(URL + "?status=PAID").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get(URL + "?status=PENDING").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsInvalidBodiesUnknownPayablesAndInvalidWeeks() throws Exception {
        String token = bearer("admin", Role.ROLE_ADMIN);
        int id = create(token);
        send(token, URL, "{}").andExpect(status().isBadRequest());
        send(token, URL + "/" + id + "/payments", "{}").andExpect(status().isBadRequest());
        schedule(token, id, 2, 6).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "/999999").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("treasury.error.payable-not-found"));
        pay(token, 999999, 1L, 1).andExpect(status().isNotFound());
        schedule(token, 999999, 11, 1).andExpect(status().isNotFound());
    }

    @Test
    void managementReadsButCannotWriteAndAnonymousIsRejected() throws Exception {
        int id = create(bearer("admin", Role.ROLE_ADMIN));
        String token = bearer("gerente", Role.ROLE_MANAGEMENT);
        mockMvc.perform(get(URL + "/" + id).header("Authorization", token)).andExpect(status().isOk());
        mockMvc.perform(get(URL).header("Authorization", token)).andExpect(status().isOk());
        send(token, URL, CREATE).andExpect(status().isForbidden());
        pay(token, id, 1L, 1).andExpect(status().isForbidden());
        schedule(token, id, 11, 1).andExpect(status().isForbidden());
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isUnauthorized());
    }
}
