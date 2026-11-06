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
class TaxParametersControllerTest {
    private static final String URL = "/api/v1/tax-parameters";
    private static final String BODY = """
            {"effectiveFrom":"2000-01-01","igv":0.18,"detraction":0.12,"retention":0.03,"incomeTaxPrepayment":0.015}
            """;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired HashingService hashing;
    private String bearer(Role role) throws Exception {
        users.save(new User(new SignUpCommand(new Username(role.name()), "clave1234", "Tester", role), hashing.encode("clave1234")));
        return "Bearer " + JsonPath.read(mvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"clave1234\"}".formatted(role.name())))
                .andReturn().getResponse().getContentAsString(), "$.token");
    }
    private ResultActions send(String token, String url, String json) throws Exception {
        return mvc.perform(post(url).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(json));
    }
    @Test void adminSetsRatesAndReadsEffectiveDatesAndHistory() throws Exception {
        String token = bearer(Role.ROLE_ADMIN);
        mvc.perform(get(URL).header("Authorization", token)).andExpect(status().isNotFound());
        send(token, URL, BODY.replace("2000-01-01", "2001-01-01").replace("0.18", "0.19")).andExpect(status().isCreated());
        send(token, URL, BODY).andExpect(status().isCreated()).andExpect(jsonPath("$.igv").value(0.18))
                .andExpect(jsonPath("$.detraction").value(0.12)).andExpect(jsonPath("$.retention").value(0.03))
                .andExpect(jsonPath("$.incomeTaxPrepayment").value(0.015));
        send(token, URL, BODY).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("accounting.error.duplicate-effective-date"));
        for (String date : new String[]{"2000-01-01", "2000-12-31"})
            mvc.perform(get(URL).param("date", date).header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.igv").value(0.18));
        mvc.perform(get(URL).header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.igv").value(0.19));
        mvc.perform(get(URL).param("date", "1999-12-31").header("Authorization", token)).andExpect(status().isNotFound());
        mvc.perform(get(URL + "/history").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].effectiveFrom").value("2001-01-01")).andExpect(jsonPath("$[1].effectiveFrom").value("2000-01-01"));
        for (String field : new String[]{"igv", "detraction", "retention", "incomeTaxPrepayment"})
            for (String value : new String[]{"-0.1", "1.01", "null"})
                send(token, URL, BODY.replaceAll("\"" + field + "\":0\\.[0-9]+", "\"" + field + "\":" + value)).andExpect(status().isBadRequest());
        send(token, URL, "{}").andExpect(status().isBadRequest());
        send(token, URL, BODY.replace("\"2000-01-01\"", "null")).andExpect(status().isBadRequest());
    }
    @Test void onlyAdminWritesAndAccountingAndManagementRead() throws Exception {
        send(bearer(Role.ROLE_ADMIN), URL, BODY).andExpect(status().isCreated());
        for (Role role : new Role[]{Role.ROLE_ACCOUNTING, Role.ROLE_MANAGEMENT, Role.ROLE_ENGINEER}) {
            String token = bearer(role);
            for (String path : new String[]{URL, URL + "/history"})
                mvc.perform(get(path).header("Authorization", token)).andExpect(status().is(role == Role.ROLE_ENGINEER ? 403 : 200));
            send(token, URL, BODY).andExpect(status().isForbidden());
        }
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
    }
}
