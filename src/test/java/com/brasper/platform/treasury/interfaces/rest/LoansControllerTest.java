package com.brasper.platform.treasury.interfaces.rest;

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
class LoansControllerTest {

    private static final String PASSWORD = "clave1234";
    private static final String URL = "/api/v1/loans";
    private static final String INSTALLMENT = "{\"dueOn\":\"2026-12-01\",\"principal\":400,\"interest\":20}";
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

    private Long seedAccount() {
        return accountCommands.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber("001-100"),
                AccountType.CHECKING, Currency.PEN, new Money(BigDecimal.ZERO, Currency.PEN),
                LocalDate.of(2026, 11, 1))).success().orElseThrow().getId();
    }

    private String registration(Long account, String currency) {
        return ("{\"type\":\"RECEIVED\",\"counterparty\":\"Bank\",\"principal\":1000,\"currency\":\"%s\","
                + "\"interestRate\":0.12,\"disbursedOn\":\"2026-11-01\",\"bankAccountId\":%d}").formatted(currency, account);
    }

    @Test
    void registersSchedulesAndPaysWithCashMovements() throws Exception {
        String token = bearer("ana", Role.ROLE_ACCOUNTING);
        Long account = seedAccount();
        String body = send(token, URL, registration(account, "PEN")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.interestRate").value(0.12))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");
        mockMvc.perform(get("/api/v1/bank-accounts/" + account).header("Authorization", token))
                .andExpect(jsonPath("$.currentBalance").value(1000));
        String installments = URL + "/" + id + "/installments";
        body = send(token, installments, INSTALLMENT).andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalInterest").value(20))
                .andReturn().getResponse().getContentAsString();
        int installment = JsonPath.read(body, "$.installments[0].id");
        send(token, installments, INSTALLMENT.replace("400", "601"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("treasury.error.installments-exceed-principal"));
        String payments = installments + "/" + installment + "/payments";
        String payment = "{\"paidOn\":\"2026-12-01\",\"bankAccountId\":%d}".formatted(account);
        send(token, payments, payment).andExpect(status().isOk())
                .andExpect(jsonPath("$.outstandingBalance").value(600))
                .andExpect(jsonPath("$.installments[0].paid").value(true));
        mockMvc.perform(get("/api/v1/bank-accounts/" + account).header("Authorization", token))
                .andExpect(jsonPath("$.currentBalance").value(580));
        send(token, payments, payment).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("treasury.error.installment-already-paid"));
    }

    @Test
    void rejectsUnknownLoansInvalidBodiesAndCurrencyMismatch() throws Exception {
        String token = bearer("admin", Role.ROLE_ADMIN);
        Long account = seedAccount();
        mockMvc.perform(get(URL + "/999999").header("Authorization", token)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("treasury.error.loan-not-found"));
        send(token, URL + "/999999/installments", INSTALLMENT).andExpect(status().isNotFound());
        send(token, URL, "{}").andExpect(status().isBadRequest());
        send(token, URL, registration(account, "USD")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("treasury.error.loan-currency-mismatch"));
    }

    @Test
    void managementReadsButCannotWriteAndAnonymousIsRejected() throws Exception {
        Long account = seedAccount();
        String body = send(bearer("admin", Role.ROLE_ADMIN), URL, registration(account, "PEN"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");
        String token = bearer("gerente", Role.ROLE_MANAGEMENT);
        mockMvc.perform(get(URL + "/" + id).header("Authorization", token)).andExpect(status().isOk());
        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get(URL + "?type=RECEIVED").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get(URL + "?type=GRANTED").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        send(token, URL, registration(account, "PEN")).andExpect(status().isForbidden());
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
