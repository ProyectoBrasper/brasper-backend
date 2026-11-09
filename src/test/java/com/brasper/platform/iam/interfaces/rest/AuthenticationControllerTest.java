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
class AuthenticationControllerTest {

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

    private String signIn(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void signInReturnsATokenThatAuthenticatesLaterRequests() throws Exception {
        createUser("ana", Role.ROLE_ENGINEER);

        String body = signIn("Ana", PASSWORD);
        String token = JsonPath.read(body, "$.token");

        // 404 = got past security (authenticated) and found no such route; 401 = anonymous.
        mockMvc.perform(get("/api/v1/nothing").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/nothing")).andExpect(status().isUnauthorized());
        org.assertj.core.api.Assertions.assertThat((String) JsonPath.read(body, "$.role"))
                .isEqualTo("ROLE_ENGINEER");
    }

    private org.springframework.test.web.servlet.ResultActions changePassword(String token, String current, String next)
            throws Exception {
        var request = put("/api/v1/auth/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(current, next));
        return mockMvc.perform(token == null ? request : request.header("Authorization", "Bearer " + token));
    }

    @Test
    void aSignedInUserChangesTheirOwnPasswordAndSignsInWithTheNewOne() throws Exception {
        createUser("ana", Role.ROLE_ENGINEER);
        String token = JsonPath.read(signIn("ana", PASSWORD), "$.token");

        changePassword(token, PASSWORD, "nueva-clave99").andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"nueva-clave99\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"%s\"}".formatted(PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theAuditLogNamesTheUserWhoChangedSomething() throws Exception {
        createUser("ana", Role.ROLE_ENGINEER);
        String token = JsonPath.read(signIn("ana", PASSWORD), "$.token");
        var audit = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("audit");
        var lines = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        lines.start();
        audit.addAppender(lines);
        try {
            changePassword(token, PASSWORD, "nueva-clave99").andExpect(status().isNoContent());
        } finally {
            audit.detachAppender(lines);
        }

        org.assertj.core.api.Assertions.assertThat(lines.list)
                .extracting(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
                .containsExactly("ana PUT /api/v1/auth/password -> 204");
    }

    @Test
    void changingThePasswordNeedsTheCurrentOneAStrongNewOneAndASignedInUser() throws Exception {
        createUser("ana", Role.ROLE_ENGINEER);
        String token = JsonPath.read(signIn("ana", PASSWORD), "$.token");

        changePassword(token, "otra-clave1", "nueva-clave99").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("iam.error.invalid-current-password"));
        changePassword(token, PASSWORD, "corta1").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("iam.error.weak-password"));
        mockMvc.perform(put("/api/v1/auth/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"\"}"))
                .andExpect(status().isBadRequest());
        changePassword(null, PASSWORD, "nueva-clave99").andExpect(status().isUnauthorized());
    }

    @Test
    void signInDoesNotExposeThePasswordHash() throws Exception {
        createUser("ana", Role.ROLE_ENGINEER);

        org.assertj.core.api.Assertions.assertThat(signIn("ana", PASSWORD)).doesNotContain("hash", "$2a$", "$2b$");
    }

    @Test
    void wrongPasswordUnknownUserAndMalformedNameAreIndistinguishable() throws Exception {
        createUser("ana", Role.ROLE_ENGINEER);

        for (String[] attempt : new String[][] {{"ana", "otra-clave1"}, {"nadie", PASSWORD}, {"a b", PASSWORD}}) {
            mockMvc.perform(post("/api/v1/auth/sign-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(attempt[0], attempt[1])))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("iam.error.invalid-credentials"));
        }
    }

    private org.springframework.test.web.servlet.ResultActions signInFrom(String address, String username,
            String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password))
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                }));
    }

    @Test
    void guessingFromOneAddressLocksOnlyThatAddressNotTheRealUser() throws Exception {
        createUser("carla", Role.ROLE_ADMIN);
        for (int attempt = 0; attempt < 5; attempt++) {
            signInFrom("203.0.113.9", "carla", "otra-clave1").andExpect(status().isUnauthorized());
        }

        signInFrom("203.0.113.9", "carla", PASSWORD).andExpect(status().isTooManyRequests());
        signInFrom("10.0.0.7", "carla", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void inactiveUserWithTheRightPasswordIsForbidden() throws Exception {
        createUser("ana", Role.ROLE_ENGINEER).deactivate();

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"%s\"}".formatted(PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("iam.error.user-inactive"));
    }

    @Test
    void blankFieldsAreABadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forgedOrGarbageTokensStayAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/nothing").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/nothing").header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aDeactivatedUsersTokenStopsWorkingImmediately() throws Exception {
        User ana = createUser("ana", Role.ROLE_ENGINEER);
        String token = JsonPath.read(signIn("ana", PASSWORD), "$.token");

        ana.deactivate();
        users.save(ana);

        mockMvc.perform(get("/api/v1/nothing").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions signUp(String bearer, String username, String password,
            String role) throws Exception {
        var request = post("/api/v1/auth/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\",\"fullName\":\"Nuevo\",\"role\":\"%s\"}"
                        .formatted(username, password, role));
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return mockMvc.perform(request);
    }

    private String tokenOf(String username, Role role) throws Exception {
        createUser(username, role);
        return JsonPath.read(signIn(username, PASSWORD), "$.token");
    }

    @Test
    void adminCreatesAUserWhoCanThenSignIn() throws Exception {
        String admin = tokenOf("root", Role.ROLE_ADMIN);

        signUp(admin, "luis", "clave5678", "ROLE_ACCOUNTING")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("luis"))
                .andExpect(jsonPath("$.role").value("ROLE_ACCOUNTING"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        org.assertj.core.api.Assertions.assertThat(signIn("luis", "clave5678")).contains("token");
    }

    @Test
    void onlyAnAdminMayCreateUsers() throws Exception {
        String engineer = tokenOf("ana", Role.ROLE_ENGINEER);

        signUp(engineer, "luis", "clave5678", "ROLE_ENGINEER").andExpect(status().isForbidden());
        signUp(null, "luis", "clave5678", "ROLE_ENGINEER").andExpect(status().isUnauthorized());
    }

    @Test
    void signUpRejectsDuplicatesWeakPasswordsAndUnknownRoles() throws Exception {
        String admin = tokenOf("root", Role.ROLE_ADMIN);

        signUp(admin, "root", "clave5678", "ROLE_ENGINEER").andExpect(status().isConflict());
        signUp(admin, "luis", "corta", "ROLE_ENGINEER").andExpect(status().isUnprocessableEntity());
        signUp(admin, "luis", "clave5678", "ROLE_BOSS").andExpect(status().isBadRequest());
        signUp(admin, "a b", "clave5678", "ROLE_ENGINEER").andExpect(status().isBadRequest());
    }

    @Test
    void healthStaysPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
