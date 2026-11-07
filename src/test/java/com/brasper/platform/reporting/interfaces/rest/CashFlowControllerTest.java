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
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Months in 1990 are always in the past with no data; months in 2041 are always in the future. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CashFlowControllerTest {

    private static final String PASSWORD = "clave1234";
    private static final String BASE = "/api/v1/reports/cash-flow";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private HashingService hashing;

    private String bearer(Role role) throws Exception {
        String username = role.name().toLowerCase().replace("role_", "") + "-cf";
        users.save(new User(new SignUpCommand(new Username(username), PASSWORD, "Full " + username, role),
                hashing.encode(PASSWORD)));
        String body = mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    @Test
    void reportsPastMonthsAsRealAndFutureOnesAsProjected() throws Exception {
        String token = bearer(Role.ROLE_MANAGEMENT);

        mockMvc.perform(get(BASE + "/weekly?year=1990&month=5").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(new Period(1990, 5).weeks().size()))
                .andExpect(jsonPath("$.lines[0].projected").value(false))
                .andExpect(jsonPath("$.finalBalance").value(0))
                .andExpect(jsonPath("$.hasNegativeBalance").value(false));
        mockMvc.perform(get(BASE + "/weekly?year=2041&month=5").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].projected").value(true));
        mockMvc.perform(get(BASE + "/monthly?fromYear=1990&fromMonth=1&toYear=1990&toMonth=3").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(3))
                .andExpect(jsonPath("$.lines[0].label").value("1990-01"));
        mockMvc.perform(get(BASE + "/projection?from=2041-05-01&weeks=4").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(4))
                .andExpect(jsonPath("$.lines[0].startDate").value("2041-05-01"));
    }

    @Test
    void rejectsBadParametersAndUnauthorizedRoles() throws Exception {
        String token = bearer(Role.ROLE_ACCOUNTING);

        mockMvc.perform(get(BASE + "/weekly?year=2041&month=13").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/monthly?fromYear=1990&fromMonth=3&toYear=1990&toMonth=1").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/projection?from=2041-05-01&weeks=0").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/weekly?year=1990&month=5").header("Authorization", bearer(Role.ROLE_ENGINEER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + "/weekly?year=1990&month=5")).andExpect(status().isUnauthorized());
    }
}
