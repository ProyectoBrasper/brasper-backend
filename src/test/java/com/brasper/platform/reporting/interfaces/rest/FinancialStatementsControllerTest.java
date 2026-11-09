package com.brasper.platform.reporting.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.application.commandservices.LedgerCommandService;
import com.brasper.platform.accounting.domain.model.commands.JournalLineData;
import com.brasper.platform.accounting.domain.model.commands.RegisterJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.iam.application.outboundservices.HashingService;
import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.Role;
import com.brasper.platform.iam.domain.model.valueobjects.Username;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FinancialStatementsControllerTest {

    private static final String PASSWORD = "clave1234";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private HashingService hashing;
    @Autowired private LedgerCommandService ledger;

    private String bearer(Role role) throws Exception {
        String username = role.name().toLowerCase().replace("role_", "") + "-fs";
        users.save(new User(new SignUpCommand(new Username(username), PASSWORD, "Full " + username, role),
                hashing.encode(PASSWORD)));
        String body = mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    @Test
    void reportsTheIncomeStatementAndTheBalanceSheetFromTheLedger() throws Exception {
        // a sale of 100 plus 18 IGV, in a month no other test uses
        ledger.handle(new RegisterJournalEntryCommand(LocalDate.of(2031, 3, 10), "Sale", EntrySource.MANUAL, null, List.of(
                new JournalLineData(new AccountCode("12"), pen("118"), pen("0"), null),
                new JournalLineData(new AccountCode("70"), pen("0"), pen("100"), null),
                new JournalLineData(new AccountCode("40111"), pen("0"), pen("18"), null))));
        String token = bearer(Role.ROLE_ACCOUNTING);

        mockMvc.perform(get("/api/v1/reports/income-statement?fromYear=2031&fromMonth=3&toYear=2031&toMonth=3")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").value(100))
                .andExpect(jsonPath("$.directCosts").value(0))
                .andExpect(jsonPath("$.netProfit").value(100));
        mockMvc.perform(get("/api/v1/reports/balance-sheet?year=2031&month=3").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value("2031-03-31"))
                .andExpect(jsonPath("$.equity.RESULT").exists())
                .andExpect(jsonPath("$.totalAssets").exists());
    }

    @Test
    void rejectsBadRangesAndUnauthorizedRoles() throws Exception {
        String token = bearer(Role.ROLE_MANAGEMENT);
        mockMvc.perform(get("/api/v1/reports/income-statement?fromYear=2031&fromMonth=3&toYear=2031&toMonth=1")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/reports/balance-sheet?year=2031&month=13").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/reports/balance-sheet?year=2031&month=3")
                        .header("Authorization", bearer(Role.ROLE_ENGINEER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reports/balance-sheet?year=2031&month=3")).andExpect(status().isUnauthorized());
    }
}
