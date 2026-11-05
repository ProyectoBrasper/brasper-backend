package com.brasper.platform.iam.interfaces.rest;

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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class UsersControllerTest {

    private static final String PASSWORD = "clave1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private HashingService hashing;

    private User createUser(String username, Role role) {
        return users.save(new User(new SignUpCommand(new Username(username), PASSWORD, "Full " + username, role),
                hashing.encode(PASSWORD)));
    }

    private String bearer(String username) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    @Test
    void adminListsAndReadsUsers() throws Exception {
        User root = createUser("root", Role.ROLE_ADMIN);
        createUser("ana", Role.ROLE_ENGINEER);
        String token = bearer("root");

        mockMvc.perform(get("/api/v1/users").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mockMvc.perform(get("/api/v1/users/" + root.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("root"));
        mockMvc.perform(get("/api/v1/users/999999").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("iam.error.user-not-found"));
    }

    @Test
    void nonAdminsAreForbiddenAndAnonymousAreUnauthorized() throws Exception {
        User ana = createUser("ana", Role.ROLE_ENGINEER);
        String token = bearer("ana");

        mockMvc.perform(get("/api/v1/users").header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/users/" + ana.getId() + "/deactivate").header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminChangesARoleAndDeactivatesAUser() throws Exception {
        createUser("root", Role.ROLE_ADMIN);
        User ana = createUser("ana", Role.ROLE_ENGINEER);
        String token = bearer("root");

        mockMvc.perform(put("/api/v1/users/" + ana.getId() + "/role").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ROLE_ACCOUNTING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ACCOUNTING"));
        mockMvc.perform(post("/api/v1/users/" + ana.getId() + "/deactivate").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post("/api/v1/users/" + ana.getId() + "/deactivate").header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    void theLastAdminCannotBeDemotedOrDeactivated() throws Exception {
        User root = createUser("root", Role.ROLE_ADMIN);
        String token = bearer("root");

        mockMvc.perform(put("/api/v1/users/" + root.getId() + "/role").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ROLE_ENGINEER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("iam.error.cannot-modify-last-admin"));
        mockMvc.perform(post("/api/v1/users/" + root.getId() + "/deactivate").header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("iam.error.cannot-modify-last-admin"));
    }

    @Test
    void unknownRoleIsABadRequest() throws Exception {
        User root = createUser("root", Role.ROLE_ADMIN);
        String token = bearer("root");

        mockMvc.perform(put("/api/v1/users/" + root.getId() + "/role").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ROLE_BOSS\"}"))
                .andExpect(status().isBadRequest());
    }
}
