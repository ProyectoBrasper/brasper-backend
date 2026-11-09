package com.brasper.platform.reporting.interfaces.rest;

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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DashboardControllerTest {

    private static final String PASSWORD = "clave1234";
    private static final String URL = "/api/v1/reports/dashboard";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private HashingService hashing;

    private String bearer(Role role) throws Exception {
        String username = role.name().toLowerCase().replace("role_", "") + "-db";
        users.save(new User(new SignUpCommand(new Username(username), PASSWORD, "Full " + username, role),
                hashing.encode(PASSWORD)));
        String body = mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    @Test
    void returnsTheDashboardOfAMonthForManagers() throws Exception {
        mockMvc.perform(get(URL + "?year=1990&month=5").header("Authorization", bearer(Role.ROLE_MANAGEMENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").exists())
                .andExpect(jsonPath("$.receivablesOutstanding").exists())
                .andExpect(jsonPath("$.payablesOutstanding").exists())
                .andExpect(jsonPath("$.netProfitYtd").value(0))
                .andExpect(jsonPath("$.projectedClosingBalance").value(0))
                .andExpect(jsonPath("$.projectsInProgress").isNumber())
                .andExpect(jsonPath("$.alerts").isArray());
    }

    @Test
    void rejectsBadMonthsAndUnauthorizedRoles() throws Exception {
        mockMvc.perform(get(URL + "?year=1990&month=13").header("Authorization", bearer(Role.ROLE_ACCOUNTING)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "?year=1990&month=5").header("Authorization", bearer(Role.ROLE_ENGINEER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "?year=1990&month=5")).andExpect(status().isUnauthorized());
    }
}
