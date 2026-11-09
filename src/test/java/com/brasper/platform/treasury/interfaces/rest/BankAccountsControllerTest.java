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
import com.brasper.platform.treasury.domain.model.commands.RegisterCashMovementCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
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
class BankAccountsControllerTest {

    private static final String PASSWORD = "clave1234";
    private static final String URL = "/api/v1/bank-accounts";

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

    private Long seedAccountWithMovements() {
        Long id = accountCommands.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber("001-100"),
                AccountType.CHECKING, Currency.PEN, new Money(new BigDecimal("100"), Currency.PEN),
                LocalDate.of(2026, 11, 1))).success().orElseThrow().getId();
        accountCommands.handle(new RegisterCashMovementCommand(id, LocalDate.of(2026, 11, 3),
                MovementDirection.INFLOW, new Money(new BigDecimal("50"), Currency.PEN), "deposit",
                MovementSource.ADJUSTMENT, null)).success().orElseThrow();
        return id;
    }

    @Test
    void readsAnAccountWithItsBalanceAndTheList() throws Exception {
        String token = bearer("gerente", Role.ROLE_MANAGEMENT);
        Long id = seedAccountWithMovements();

        mockMvc.perform(get(URL + "/" + id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("001-100"))
                .andExpect(jsonPath("$.openingBalance").value(100))
                .andExpect(jsonPath("$.currentBalance").value(150));
        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listsTheMovementsOfAMonth() throws Exception {
        String token = bearer("ana", Role.ROLE_ACCOUNTING);
        Long id = seedAccountWithMovements();

        mockMvc.perform(get(URL + "/" + id + "/movements?year=2026&month=11").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].direction").value("INFLOW"))
                .andExpect(jsonPath("$[0].source").value("ADJUSTMENT"));
        mockMvc.perform(get(URL + "/" + id + "/movements?year=2026&month=10").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsUnknownAccountsBadMonthsAndAnonymousCalls() throws Exception {
        String token = bearer("ana", Role.ROLE_ACCOUNTING);
        Long id = seedAccountWithMovements();

        mockMvc.perform(get(URL + "/999999").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("treasury.error.bank-account-not-found"));
        mockMvc.perform(get(URL + "/999999/movements?year=2026&month=11").header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(URL + "/" + id + "/movements?year=2026&month=13").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void anEngineerCannotReadTheCompanyBankAccounts() throws Exception {
        String token = bearer("luis", Role.ROLE_ENGINEER);
        Long id = seedAccountWithMovements();

        mockMvc.perform(get(URL).header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/" + id).header("Authorization", token)).andExpect(status().isForbidden());
    }
}
