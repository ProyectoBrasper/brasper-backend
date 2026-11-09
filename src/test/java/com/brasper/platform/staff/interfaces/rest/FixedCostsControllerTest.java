package com.brasper.platform.staff.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.iam.application.outboundservices.HashingService;
import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.Role;
import com.brasper.platform.iam.domain.model.valueobjects.Username;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.staff.application.commandservices.EmployeeCommandService;
import com.brasper.platform.staff.domain.model.commands.HireEmployeeCommand;
import com.brasper.platform.staff.domain.model.valueobjects.Dni;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandService;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.*;
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
class FixedCostsControllerTest {
    private static final String URL = "/api/v1/fixed-costs";
    private static final String COST = """
            {"code":"RENT","concept":"Rent","type":"OFFICE_RENT","monthlyAmount":100,"currency":"PEN"}
            """;
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private HashingService hashing;
    @Autowired private EmployeeCommandService employees;
    @Autowired private BankAccountCommandService accounts;

    private String bearer(String username, Role role) throws Exception {
        users.save(new User(new SignUpCommand(new Username(username), "clave1234", "Full " + username, role),
                hashing.encode("clave1234")));
        var body = mvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"clave1234\"}".formatted(username)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }
    private ResultActions send(String token, String url, String body) throws Exception {
        return mvc.perform(post(url).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private ResultActions read(String token, String path) throws Exception {
        return mvc.perform(get(URL + path).header("Authorization", token));
    }
    private int create(String token, String body) throws Exception {
        return JsonPath.read(send(token, URL, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }
    @Test
    void pagesTheListAndReportsTheTotalInAHeader() throws Exception {
        String token = bearer("ana", Role.ROLE_ACCOUNTING);
        for (String code : new String[]{"RENT1", "RENT2", "RENT3"}) {
            create(token, COST.replace("\"code\":\"RENT\"", "\"code\":\"" + code + "\""));
        }
        String all = read(token, "").andExpect(header().doesNotExist("X-Total-Count"))
                .andReturn().getResponse().getContentAsString();
        int total = JsonPath.read(all, "$.length()");

        read(token, "?page=0&size=2").andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", String.valueOf(total)))
                .andExpect(jsonPath("$.length()").value(2));
        read(token, "?active=false&page=0&size=2").andExpect(jsonPath("$.length()").value(0));
        read(token, "?size=2").andExpect(status().isBadRequest());
        read(token, "?page=0&size=201").andExpect(status().isBadRequest());
    }
    private String payment(Object account) {
        return """
                {"paidOn":"2026-11-02","year":2026,"month":11,"amount":25,"bankAccountId":%s,"notes":"Paid"}
                """.formatted(account);
    }
    @Test
    void accountingManagesCostsAndPayments() throws Exception {
        String token = bearer("accountant", Role.ROLE_ACCOUNTING);
        Long employee = employees.handle(new HireEmployeeCommand("Ana Perez", new Dni("12345678"), "Engineer",
                new Money(new BigDecimal("100"), Currency.PEN), LocalDate.of(2026, 11, 1))).success().orElseThrow().getId();
        Long account = accounts.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber("001-1"),
                AccountType.CHECKING, Currency.PEN, Money.zero(Currency.PEN), LocalDate.of(2026, 11, 1)))
                .success().orElseThrow().getId();
        int id = create(token, COST.replace("OFFICE_RENT", "SALARY").replace("}", ",\"employeeId\":" + employee + "}"));
        read(token, "/" + id).andExpect(status().isOk()).andExpect(jsonPath("$.expectedWeeks.length()").value(0));
        read(token, "").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(put(URL + "/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"concept\":\"Updated\",\"monthlyAmount\":120,\"expectedWeeks\":[1,3]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.monthlyAmount").value(120))
                .andExpect(jsonPath("$.currency").value("PEN")).andExpect(jsonPath("$.expectedWeeks[1]").value(3));
        send(token, URL + "/" + id + "/payments", payment(null)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.fixedCostId").value(id))
                .andExpect(jsonPath("$.currency").value("PEN"));
        send(token, URL + "/" + id + "/payments", payment(account)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.bankAccountId").value(account)).andExpect(jsonPath("$.amount").value(25));
        read(token, "/payments?year=2026&month=11").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].fixedCostId").value(id));
        read(token, "/payments?year=2026&month=12").andExpect(jsonPath("$.length()").value(0));
        send(token, URL + "/" + id + "/deactivate", "{}").andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        read(token, "?active=true").andExpect(jsonPath("$.length()").value(0));
        read(token, "?active=false").andExpect(jsonPath("$.length()").value(1));
        send(token, URL + "/" + id + "/payments", payment(null)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("staff.error.fixed-cost-inactive"));
    }
    @Test
    void reportsFailuresAndRejectsInvalidRequests() throws Exception {
        String token = bearer("accountant", Role.ROLE_ACCOUNTING);
        int id = create(token, COST);
        send(token, URL, COST).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("staff.error.duplicate-cost-code"));
        send(token, URL, COST.replace("\"code\":\"RENT\"", "\"code\":\"SAL\"").replace("OFFICE_RENT", "SALARY"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("staff.error.employee-required"));
        send(token, URL, COST.replace("\"code\":\"RENT\"", "\"code\":\"OTHER\"").replace("}", ",\"employeeId\":999999}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("staff.error.employee-not-found"));
        read(token, "/999999").andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("staff.error.fixed-cost-not-found"));
        send(token, URL + "/999999/payments", payment(null)).andExpect(status().isNotFound());
        mvc.perform(put(URL + "/999999").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"concept\":\"Updated\",\"monthlyAmount\":120}")).andExpect(status().isNotFound());
        send(token, URL + "/" + id + "/payments", payment(999999)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("staff.error.bank-account-not-found"));
        send(token, URL + "/" + id + "/payments", payment(null).replace("\"amount\":25", "\"amount\":0"))
                .andExpect(status().isBadRequest());
        read(token, "/payments?year=2026&month=13").andExpect(status().isBadRequest());
        read(token, "/payments?year=2026").andExpect(status().isBadRequest());
    }
    @Test
    void protectsSensitiveReadsAndRestrictsWrites() throws Exception {
        String admin = bearer("admin", Role.ROLE_ADMIN);
        int id = create(admin, COST);
        String manager = bearer("manager", Role.ROLE_MANAGEMENT);
        String engineer = bearer("engineer", Role.ROLE_ENGINEER);
        for (String path : new String[]{"", "/" + id, "/payments?year=2026&month=11"}) {
            read(manager, path).andExpect(status().isOk());
            read(engineer, path).andExpect(status().isForbidden());
            mvc.perform(get(URL + path)).andExpect(status().isUnauthorized());
        }
        for (String token : new String[]{manager, engineer}) {
            send(token, URL, COST).andExpect(status().isForbidden());
            send(token, URL + "/" + id + "/deactivate", "{}").andExpect(status().isForbidden());
            send(token, URL + "/" + id + "/payments", payment(null)).andExpect(status().isForbidden());
            mvc.perform(put(URL + "/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"concept\":\"Updated\",\"monthlyAmount\":120}")).andExpect(status().isForbidden());
        }
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(COST)).andExpect(status().isUnauthorized());
    }
}
