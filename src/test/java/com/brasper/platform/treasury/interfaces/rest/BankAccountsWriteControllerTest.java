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
import com.jayway.jsonpath.JsonPath;
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
class BankAccountsWriteControllerTest {

    private static final String PASSWORD = "clave1234";
    private static final String URL = "/api/v1/bank-accounts";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private HashingService hashing;

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

    private int open(String token, String number, String currency, String opening) throws Exception {
        String json = "{\"bank\":\"BCP\",\"accountNumber\":\"%s\",\"type\":\"CHECKING\",\"currency\":\"%s\",\"openingBalance\":%s,\"openedOn\":\"2026-11-01\"}"
                .formatted(number, currency, opening);
        String body = send(token, URL, json).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void accountingOpensAnAccountAndReadsIt() throws Exception {
        String token = bearer("ana", Role.ROLE_ACCOUNTING);
        int id = open(token, "001-100", "PEN", "250.50");

        mockMvc.perform(get(URL + "/" + id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("001-100"))
                .andExpect(jsonPath("$.currentBalance").value(250.5));
        mockMvc.perform(get(URL).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void adjustmentsAndTransfersMoveTheBalances() throws Exception {
        String token = bearer("ana", Role.ROLE_ADMIN);
        int from = open(token, "001-1", "PEN", "100");
        int to = open(token, "001-2", "PEN", "0");

        send(token, URL + "/" + from + "/adjustments",
                "{\"occurredOn\":\"2026-11-03\",\"direction\":\"INFLOW\",\"amount\":50,\"concept\":\"deposit\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.currentBalance").value(150));
        send(token, URL + "/transfers", ("{\"fromAccountId\":%d,\"toAccountId\":%d,"
                + "\"transferredOn\":\"2026-11-04\",\"amount\":40}").formatted(from, to))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.currentBalance").value(110));

        mockMvc.perform(get(URL + "/" + to).header("Authorization", token))
                .andExpect(jsonPath("$.currentBalance").value(40));
        mockMvc.perform(get(URL + "/" + from + "/movements?year=2026&month=11").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].source").exists());
        mockMvc.perform(get(URL + "/" + from + "/movements?year=2026&month=10").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsInvalidRequestsAndUnknownAccounts() throws Exception {
        String token = bearer("ana", Role.ROLE_ACCOUNTING);
        int pen = open(token, "001-1", "PEN", "0");
        int usd = open(token, "001-2", "USD", "0");

        send(token, URL, "{\"bank\":\"BCP\",\"accountNumber\":\"001-1\",\"type\":\"CHECKING\",\"currency\":\"PEN\","
                + "\"openingBalance\":0,\"openedOn\":\"2026-11-01\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("treasury.error.duplicate-account-number"));
        send(token, URL, "{\"bank\":\"BCP\"}").andExpect(status().isBadRequest());
        send(token, URL + "/" + pen + "/adjustments",
                "{\"occurredOn\":\"2026-11-03\",\"direction\":\"INFLOW\",\"amount\":0}")
                .andExpect(status().isBadRequest());
        send(token, URL + "/transfers", ("{\"fromAccountId\":%d,\"toAccountId\":%d,"
                + "\"transferredOn\":\"2026-11-04\",\"amount\":1}").formatted(pen, usd))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("treasury.error.currency-mismatch"));
        send(token, URL + "/transfers", ("{\"fromAccountId\":%d,\"toAccountId\":%d,"
                + "\"transferredOn\":\"2026-11-04\",\"amount\":1}").formatted(pen, pen))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("treasury.error.same-account"));
        mockMvc.perform(get(URL + "/999999").header("Authorization", token)).andExpect(status().isNotFound());
        mockMvc.perform(get(URL + "/999999/movements?year=2026&month=11").header("Authorization", token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(URL + "/" + pen + "/movements?year=2026&month=13").header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void managementReadsButCannotWriteAndAnonymousIsRejected() throws Exception {
        String admin = bearer("admin", Role.ROLE_ADMIN);
        int id = open(admin, "001-1", "PEN", "0");
        String token = bearer("gerente", Role.ROLE_MANAGEMENT);

        mockMvc.perform(get(URL + "/" + id).header("Authorization", token)).andExpect(status().isOk());
        send(token, URL + "/" + id + "/adjustments",
                "{\"occurredOn\":\"2026-11-03\",\"direction\":\"INFLOW\",\"amount\":1}")
                .andExpect(status().isForbidden());
        send(bearer("ing", Role.ROLE_ENGINEER), URL, "{\"bank\":\"BCP\",\"accountNumber\":\"9\",\"type\":\"CHECKING\","
                + "\"currency\":\"PEN\",\"openingBalance\":0,\"openedOn\":\"2026-11-01\"}")
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
