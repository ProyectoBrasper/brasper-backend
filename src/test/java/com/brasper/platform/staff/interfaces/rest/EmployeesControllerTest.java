package com.brasper.platform.staff.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class EmployeesControllerTest {

    private static final String PASSWORD = "clave1234";
    private static final String URL = "/api/v1/employees";
    private static final String HIRE = "{\"fullName\":\"Ana Perez\",\"dni\":\"12345678\",\"position\":\"Engineer\","
            + "\"monthlySalary\":3000.50,\"currency\":\"PEN\",\"hiredOn\":\"2026-03-01\"}";

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

    @Test
    void accountingHiresUpdatesAndTerminatesAnEmployee() throws Exception {
        String token = bearer("ana", Role.ROLE_ACCOUNTING);
        int id = JsonPath.read(send(token, URL, HIRE).andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put(URL + "/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\":\"Lead\",\"monthlySalary\":4000,\"currency\":\"PEN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.position").value("Lead"))
                .andExpect(jsonPath("$.monthlySalary").value(4000));
        send(token, URL + "/" + id + "/termination", "{\"terminatedOn\":\"2026-10-31\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.terminatedOn").value("2026-10-31"));
        mockMvc.perform(get(URL + "?active=true").header("Authorization", token))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(URL).header("Authorization", token)).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get(URL + "/" + id).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dni").value("12345678"));
    }

    @Test
    void mapsEachFailureToItsStatus() throws Exception {
        String token = bearer("ana", Role.ROLE_ADMIN);
        int id = JsonPath.read(send(token, URL, HIRE).andReturn().getResponse().getContentAsString(), "$.id");

        send(token, URL, HIRE).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("staff.error.duplicate-dni"));
        send(token, URL + "/" + id + "/termination", "{\"terminatedOn\":\"2026-02-01\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("staff.error.invalid-termination-date"));
        send(token, URL + "/" + id + "/termination", "{\"terminatedOn\":\"2026-10-31\"}").andExpect(status().isOk());
        send(token, URL + "/" + id + "/termination", "{\"terminatedOn\":\"2026-11-30\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("staff.error.already-terminated"));
        send(token, URL + "/999999/termination", "{\"terminatedOn\":\"2026-11-30\"}").andExpect(status().isNotFound());
        mockMvc.perform(get(URL + "/999999").header("Authorization", token)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("staff.error.employee-not-found"));
    }

    @Test
    void rejectsInvalidBodies() throws Exception {
        String token = bearer("ana", Role.ROLE_ADMIN);

        send(token, URL, HIRE.replace("12345678", "123")).andExpect(status().isBadRequest());
        send(token, URL, HIRE.replace("3000.50", "-1")).andExpect(status().isBadRequest());
        send(token, URL, "{}").andExpect(status().isBadRequest());
    }

    @Test
    void managementReadsButCannotWriteAndEngineersAndAnonymousAreRejected() throws Exception {
        send(bearer("admin", Role.ROLE_ADMIN), URL, HIRE).andExpect(status().isCreated());
        String management = bearer("gerente", Role.ROLE_MANAGEMENT);
        String engineer = bearer("ingeniero", Role.ROLE_ENGINEER);

        mockMvc.perform(get(URL).header("Authorization", management)).andExpect(status().isOk());
        send(management, URL, HIRE).andExpect(status().isForbidden());
        mockMvc.perform(get(URL).header("Authorization", engineer)).andExpect(status().isForbidden());
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
