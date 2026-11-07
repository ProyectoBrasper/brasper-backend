package com.brasper.platform.accounting.interfaces.rest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.application.commandservices.*;
import com.brasper.platform.accounting.domain.model.commands.*;
import com.brasper.platform.accounting.domain.model.valueobjects.*;
import com.brasper.platform.accounting.interfaces.rest.transform.TaxFailureResponseAssembler;
import com.brasper.platform.iam.application.outboundservices.HashingService;
import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.*;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandService;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.*;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class) @Transactional
class TaxReturnsControllerTest {
    private static final String URL = "/api/v1/tax-returns";
    private static final String BODY = "{\"year\":2026,\"month\":1}";
    private static final String PAYMENT = "{\"paidOn\":\"2026-02-15\",\"bankAccountId\":%d}";
    @Autowired MockMvc mvc;
    @Autowired LedgerCommandService ledger;
    @Autowired BankAccountCommandService accounts;
    @Autowired UserRepository users;
    @Autowired HashingService hashing;
    @Autowired MessageSource messages;
    private String bearer(Role role) throws Exception {
        users.save(new User(new SignUpCommand(new Username(role.name()), "clave1234", "Tester", role), hashing.encode("clave1234")));
        return "Bearer " + JsonPath.read(mvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"clave1234\"}".formatted(role.name())))
                .andReturn().getResponse().getContentAsString(), "$.token");
    }
    private ResultActions send(String token, String path, String body) throws Exception {
        return mvc.perform(post(path).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private JournalLineData line(String code, String debit, String credit) {
        return new JournalLineData(new AccountCode(code), new Money(new BigDecimal(debit), Currency.PEN),
                new Money(new BigDecimal(credit), Currency.PEN), null);
    }
    @Test void preparesReadsHistoryAndPaysWithMappedFailures() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);
        ledger.handle(new RegisterJournalEntryCommand(LocalDate.of(2026, 1, 15), "Taxable sale", EntrySource.MANUAL, null,
                List.of(line("10", "118", "0"), line("70", "0", "100"), line("40111", "0", "18")))).success().orElseThrow();
        int id = JsonPath.read(send(token, URL, BODY).andExpect(status().isCreated())
                .andExpect(header().string("Location", URL + "?year=2026&month=1"))
                .andExpect(jsonPath("$.year").value(2026)).andExpect(jsonPath("$.month").value(1))
                .andExpect(jsonPath("$.outputIgv.amount").value(18)).andExpect(jsonPath("$.inputIgv.amount").value(0))
                .andExpect(jsonPath("$.carriedCredit.amount").value(0)).andExpect(jsonPath("$.igvPayable.amount").value(18))
                .andExpect(jsonPath("$.incomeTaxPrepayment.amount").value(1.50)).andExpect(jsonPath("$.totalPayable.amount").value(19.50))
                .andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.paidOn").isEmpty())
                .andExpect(jsonPath("$.bankAccountId").isEmpty()).andReturn().getResponse().getContentAsString(), "$.id");
        send(token, URL, BODY).andExpect(status().isConflict());
        mvc.perform(get(URL).param("year", "2026").param("month", "1").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get(URL).param("year", "2025").param("month", "12").header("Authorization", token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("accounting.error.tax-return-not-found"));
        send(token, URL, BODY.replace("2026", "2025")).andExpect(status().isCreated());
        mvc.perform(get(URL + "/history").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").value(2026)).andExpect(jsonPath("$[1].year").value(2025));
        String paymentPath = URL + "/" + id + "/payment";
        send(token, paymentPath, PAYMENT.formatted(999999L)).andExpect(status().is(
                TaxFailureResponseAssembler.toResponse(TaxCommandFailure.BANK_ACCOUNT_NOT_FOUND, messages).getStatusCode().value()));
        Long bankId = accounts.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber("001-9"), AccountType.CHECKING,
                Currency.PEN, new Money(new BigDecimal("1000"), Currency.PEN), LocalDate.of(2025, 1, 1))).success().orElseThrow().getId();
        send(token, paymentPath, PAYMENT.formatted(bankId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID")).andExpect(jsonPath("$.paidOn").value("2026-02-15"))
                .andExpect(jsonPath("$.bankAccountId").value(bankId));
        send(token, paymentPath, PAYMENT.formatted(bankId)).andExpect(status().isConflict());
        send(token, URL + "/999999/payment", PAYMENT.formatted(bankId)).andExpect(status().isNotFound());
        for (String field : new String[]{"outputIgv", "inputIgv", "carriedCredit", "igvPayable", "incomeTaxPrepayment", "totalPayable"})
            mvc.perform(get(URL).param("year", "2026").param("month", "1").header("Authorization", token))
                    .andExpect(jsonPath("$." + field + ".currency").value("PEN"));
    }
    @Test void validatesPeriodAndPayment() throws Exception {
        String token = bearer(Role.ROLE_ADMIN);
        for (String body : new String[]{"{}", BODY.replace("2026", "1999"), BODY.replace("2026", "2101"),
                BODY.replace(":1}", ":0}"), BODY.replace(":1}", ":13}")})
            send(token, URL, body).andExpect(status().isBadRequest());
        for (String body : new String[]{"{}", "{\"paidOn\":\"2026-02-15\"}", "{\"bankAccountId\":1}"})
            send(token, URL + "/1/payment", body).andExpect(status().isBadRequest());
        for (String query : new String[]{"", "?year=2026", "?month=1", "?year=2026&month=13"})
            mvc.perform(get(URL + query).header("Authorization", token)).andExpect(status().isBadRequest());
    }
    @Test void enforcesRolesAndAuthentication() throws Exception {
        for (Role role : new Role[]{Role.ROLE_ADMIN, Role.ROLE_ACCOUNTING, Role.ROLE_MANAGEMENT, Role.ROLE_ENGINEER}) {
            String token = bearer(role);
            boolean writes = role == Role.ROLE_ADMIN || role == Role.ROLE_ACCOUNTING;
            send(token, URL, BODY.replace("2026", role == Role.ROLE_ADMIN ? "2024" : "2025"))
                    .andExpect(status().is(writes ? 201 : 403));
            send(token, URL + "/999999/payment", PAYMENT.formatted(999999L)).andExpect(status().is(writes ? 404 : 403));
            mvc.perform(get(URL + "?year=2024&month=1").header("Authorization", token))
                    .andExpect(status().is(role == Role.ROLE_ENGINEER ? 403 : 200));
            mvc.perform(get(URL + "/history").header("Authorization", token))
                    .andExpect(status().is(role == Role.ROLE_ENGINEER ? 403 : 200));
        }
        for (String path : new String[]{URL + "?year=2026&month=1", URL + "/history"})
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        for (String path : new String[]{URL, URL + "/1/payment"})
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(path.equals(URL) ? BODY : PAYMENT.formatted(1L)))
                    .andExpect(status().isUnauthorized());
    }
}
