package com.brasper.platform.accounting.interfaces.rest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.application.commandservices.LedgerCommandService;
import com.brasper.platform.accounting.domain.model.commands.CreateLedgerAccountCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.*;
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
class JournalEntriesControllerTest {
    private static final String URL = "/api/v1/journal-entries";
    static final String BODY = """
            {"entryDate":"2026-01-01","description":"Manual","lines":[
            {"accountCode":"101","debit":{"amount":100,"currency":"PEN"},"credit":{"amount":0,"currency":"PEN"},"projectId":42},
            {"accountCode":"401","debit":{"amount":0,"currency":"PEN"},"credit":{"amount":100,"currency":"PEN"}}]}
            """;
    @Autowired MockMvc mvc;
    @Autowired LedgerCommandService commands;
    @Autowired UserRepository users;
    @Autowired HashingService hashing;
    private ResultActions send(String token, String path, String body) throws Exception {
        return mvc.perform(post(path).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body));
    }
    @Test void registersReadsListsAndReversesWithMappedFailures() throws Exception {
        users.save(new User(new SignUpCommand(new Username("accountant"), "clave1234", "Tester", Role.ROLE_ACCOUNTING), hashing.encode("clave1234")));
        String token = "Bearer " + JsonPath.read(mvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"accountant\",\"password\":\"clave1234\"}")).andReturn().getResponse().getContentAsString(), "$.token");
        for (String code : new String[]{"101", "401"}) commands.handle(new CreateLedgerAccountCommand(new AccountCode(code), "Account " + code, LedgerAccountType.ASSET));
        int id = JsonPath.read(send(token, URL, BODY).andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.source").value("MANUAL")).andExpect(jsonPath("$.sourceId").isEmpty())
                .andExpect(jsonPath("$.totalDebit.amount").value(100)).andExpect(jsonPath("$.totalCredit.currency").value("PEN"))
                .andExpect(jsonPath("$.lines[0].projectId").value(42)).andReturn().getResponse().getContentAsString(), "$.id");
        send(token, URL, BODY.replaceFirst("100", "99")).andExpect(status().isBadRequest());
        send(token, URL, BODY.replace("401", "999")).andExpect(status().isNotFound());
        mvc.perform(get(URL + "/" + id).header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.lines.length()").value(2));
        mvc.perform(get(URL + "/999999").header("Authorization", token)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("accounting.error.journal-entry-not-found"));
        mvc.perform(get(URL + "?year=2026&month=1").header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        send(token, URL + "/" + id + "/reversal", "{\"reversedOn\":\"2026-02-01\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.reversalOfId").value(id)).andExpect(jsonPath("$.lines[0].credit.amount").value(100));
        send(token, URL + "/" + id + "/reversal", "{\"reversedOn\":\"2026-02-01\"}").andExpect(status().isConflict());
        for (String body : new String[]{"{}", BODY.replace("100", "1.001"), BODY.replace("\"PEN\"", "null"), BODY.replace("\"accountCode\":\"101\"", "\"accountCode\":null")})
            send(token, URL, body).andExpect(status().isBadRequest());
        mvc.perform(get(URL + "?year=0&month=1").header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(get(URL).header("Authorization", token)).andExpect(status().isBadRequest());
    }
}
