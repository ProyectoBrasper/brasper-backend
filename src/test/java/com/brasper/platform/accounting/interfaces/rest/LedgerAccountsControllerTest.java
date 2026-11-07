package com.brasper.platform.accounting.interfaces.rest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.iam.application.outboundservices.HashingService;
import com.brasper.platform.iam.domain.model.aggregates.User;
import com.brasper.platform.iam.domain.model.commands.SignUpCommand;
import com.brasper.platform.iam.domain.model.valueobjects.*;
import com.brasper.platform.iam.domain.repositories.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class) @Transactional
class LedgerAccountsControllerTest {
    private static final String URL = "/api/v1/ledger-accounts";
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired HashingService hashing;
    private String bearer(Role role) throws Exception {
        users.save(new User(new SignUpCommand(new Username(role.name()), "clave1234", "Tester", role), hashing.encode("clave1234")));
        return "Bearer " + JsonPath.read(mvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"clave1234\"}".formatted(role.name())))
                .andReturn().getResponse().getContentAsString(), "$.token");
    }
    private ResultActions send(String token, String code) throws Exception {
        return mvc.perform(post(URL).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"%s\",\"name\":\"Account %s\",\"type\":\"ASSET\"}".formatted(code, code)));
    }
    @Test void createsListsAndCalculatesTrialBalance() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);
        send(token, "101").andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").value("101")).andExpect(jsonPath("$.active").value(true));
        send(token, "101").andExpect(status().isConflict());
        send(token, "401").andExpect(status().isCreated());
        mvc.perform(get(URL).header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(post("/api/v1/journal-entries").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(JournalEntriesControllerTest.BODY)).andExpect(status().isCreated());
        mvc.perform(get(URL + "/trial-balance?year=2026&month=1").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026)).andExpect(jsonPath("$.month").value(1)).andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[?(@.accountCode == '101')].debit.amount").value(org.hamcrest.Matchers.contains(100.0)))
                .andExpect(jsonPath("$.lines[?(@.accountCode == '101')].credit.amount").value(org.hamcrest.Matchers.contains(0.0)))
                .andExpect(jsonPath("$.lines[?(@.accountCode == '401')].debit.amount").value(org.hamcrest.Matchers.contains(0.0)))
                .andExpect(jsonPath("$.lines[?(@.accountCode == '401')].credit.amount").value(org.hamcrest.Matchers.contains(100.0)));
        for (String params : new String[]{"year=0&month=1", "year=2026&month=13", "year=2026&month=0"})
            mvc.perform(get(URL + "/trial-balance?" + params).header("Authorization", token)).andExpect(status().isBadRequest());
        send(bearer(Role.ROLE_ENGINEER), "501").andExpect(status().isForbidden());
        send(token, "0").andExpect(status().isBadRequest());
    }
    @Test void enforcesRolesAndAuthentication() throws Exception {
        for (Role role : Role.values()) {
            String token = bearer(role);
            boolean reader = role != Role.ROLE_ENGINEER;
            boolean writer = role == Role.ROLE_ADMIN || role == Role.ROLE_ACCOUNTING;
            mvc.perform(get(URL).header("Authorization", token)).andExpect(status().is(reader ? 200 : 403));
            mvc.perform(get(URL + "/trial-balance?year=2026&month=1").header("Authorization", token)).andExpect(status().is(reader ? 200 : 403));
            send(token, "10" + role.ordinal()).andExpect(status().is(writer ? 201 : 403));
            mvc.perform(get("/api/v1/journal-entries?year=2026&month=1").header("Authorization", token)).andExpect(status().is(reader ? 200 : 403));
            mvc.perform(get("/api/v1/journal-entries/999999").header("Authorization", token)).andExpect(status().is(reader ? 404 : 403));
            mvc.perform(post("/api/v1/journal-entries").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                    .content(JournalEntriesControllerTest.BODY)).andExpect(status().is(writer ? 404 : 403));
            mvc.perform(post("/api/v1/journal-entries/999999/reversal").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reversedOn\":\"2026-02-01\"}")).andExpect(status().is(writer ? 404 : 403));
        }
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/journal-entries?year=2026&month=1")).andExpect(status().isUnauthorized());
    }
}
