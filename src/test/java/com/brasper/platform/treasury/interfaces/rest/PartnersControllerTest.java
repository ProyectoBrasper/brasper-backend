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
class PartnersControllerTest {

    private static final String PASSWORD = "clave1234";
    private static final String URL = "/api/v1/partners";
    private static final String PARTNER = "{\"fullName\":\"Ana Perez\",\"role\":\"Engineer\"}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private HashingService hashing;
    @Autowired
    private BankAccountCommandService accounts;

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

    private static String contribution(String amount, Object bankAccountId) {
        return "{\"contributedOn\":\"2026-11-02\",\"amount\":%s,\"currency\":\"PEN\",\"reimbursable\":true,\"bankAccountId\":%s,\"concept\":\"tools\"}"
                .formatted(amount, bankAccountId);
    }

    @Test
    void accountingRegistersAPartnerAndItsContributions() throws Exception {
        String token = bearer("ana", Role.ROLE_ACCOUNTING);
        Long account = accounts.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber("001-1"),
                AccountType.CHECKING, Currency.PEN, Money.zero(Currency.PEN), LocalDate.of(2026, 11, 1)))
                .success().orElseThrow().getId();
        int id = JsonPath.read(send(token, URL, PARTNER).andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalContributed").value(0))
                .andReturn().getResponse().getContentAsString(), "$.id");

        send(token, URL + "/" + id + "/contributions", contribution("100.50", "null")).andExpect(status().isCreated());
        send(token, URL + "/" + id + "/contributions", contribution("50", account))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalContributed").value(150.5))
                .andExpect(jsonPath("$.contributions.length()").value(2));
        mockMvc.perform(get(URL + "/" + id).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Ana Perez"));
        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsDuplicatesUnknownPartnersAccountsAndInvalidBodies() throws Exception {
        String token = bearer("ana", Role.ROLE_ADMIN);
        int id = JsonPath.read(send(token, URL, PARTNER).andReturn().getResponse().getContentAsString(), "$.id");

        send(token, URL, PARTNER).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("treasury.error.duplicate-partner"));
        send(token, URL, "{\"fullName\":\"\"}").andExpect(status().isBadRequest());
        send(token, URL + "/999999/contributions", contribution("10", "null")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("treasury.error.partner-not-found"));
        send(token, URL + "/" + id + "/contributions", contribution("10", "999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("treasury.error.contribution-account-not-found"));
        send(token, URL + "/" + id + "/contributions", contribution("0", "null")).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "/999999").header("Authorization", token)).andExpect(status().isNotFound());
    }

    @Test
    void managementReadsButCannotWriteAndAnonymousIsRejected() throws Exception {
        String admin = bearer("admin", Role.ROLE_ADMIN);
        send(admin, URL, PARTNER).andExpect(status().isCreated());
        String token = bearer("gerente", Role.ROLE_MANAGEMENT);

        mockMvc.perform(get(URL).header("Authorization", token)).andExpect(status().isOk());
        send(token, URL, "{\"fullName\":\"Luis Soto\"}").andExpect(status().isForbidden());
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
