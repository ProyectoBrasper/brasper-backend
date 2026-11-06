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
class FixedAssetsControllerTest {
    private static final String URL = "/api/v1/fixed-assets";
    private static final String BODY = """
            {"description":"Laptop","acquiredOn":"2000-01-01","taxableBase":1200,"currency":"PEN","usefulLifeMonths":12,"expenseId":42}
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
    @Test void accountingRegistersReadsAndDisposesAndFailuresAreMapped() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);
        int id = JsonPath.read(send(token, URL, BODY).andExpect(status().isCreated())
                .andExpect(jsonPath("$.igv").value(216)).andExpect(jsonPath("$.total").value(1416))
                .andExpect(jsonPath("$.monthlyDepreciation").value(100)).andExpect(jsonPath("$.accumulatedDepreciation").value(1200))
                .andExpect(jsonPath("$.bookValue").value(0)).andExpect(jsonPath("$.expenseId").value(42))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(get(URL).header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get(URL + "/" + id).header("Authorization", token)).andExpect(status().isOk()).andExpect(jsonPath("$.description").value("Laptop"));
        mvc.perform(get(URL + "/999999").header("Authorization", token)).andExpect(status().isNotFound());
        send(token, URL + "/999999/disposal", "{\"disposedOn\":\"2000-02-01\"}").andExpect(status().isNotFound());
        send(token, URL + "/" + id + "/disposal", "{\"disposedOn\":\"1999-01-01\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("accounting.error.invalid-disposal-date"));
        send(token, URL + "/" + id + "/disposal", "{\"disposedOn\":\"2000-02-01\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.disposedOn").value("2000-02-01")).andExpect(jsonPath("$.accumulatedDepreciation").value(100));
        send(token, URL + "/" + id + "/disposal", "{\"disposedOn\":\"2000-03-01\"}").andExpect(status().isConflict());
        for (String body : new String[]{"{}", BODY.replace("1200", "-1"), BODY.replace(":12,", ":0,"), BODY.replace(":12,", ":601,"),
                BODY.replace("Laptop", " "), BODY.replace("Laptop", "x".repeat(301)), BODY.replace("1200", "1.001"), BODY.replace("1200", "1000000000000")})
            send(token, URL, body).andExpect(status().isBadRequest());
        send(token, URL + "/" + id + "/disposal", "{}").andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(400, com.brasper.platform.accounting.interfaces.rest.transform.FixedAssetFailureResponseAssembler
                .toResponse(com.brasper.platform.accounting.application.commandservices.FixedAssetCommandFailure.INVALID_USEFUL_LIFE,
                        new org.springframework.context.support.StaticMessageSource()).getStatusCode().value());
    }
    @Test void enforcesRoles() throws Exception {
        for (Role role : Role.values()) {
            String token = bearer(role);
            boolean reader = role != Role.ROLE_ENGINEER;
            boolean writer = role == Role.ROLE_ADMIN || role == Role.ROLE_ACCOUNTING;
            mvc.perform(get(URL).header("Authorization", token)).andExpect(status().is(reader ? 200 : 403));
            send(token, URL, BODY).andExpect(status().is(writer ? 201 : 403));
            send(token, URL + "/999999/disposal", "{\"disposedOn\":\"2000-02-01\"}").andExpect(status().is(writer ? 404 : 403));
            mvc.perform(get(URL + "/999999").header("Authorization", token)).andExpect(status().is(reader ? 404 : 403));
        }
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
    }
}
