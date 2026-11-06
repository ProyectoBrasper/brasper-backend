package com.brasper.platform.expenses.interfaces.rest;

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
class SuppliersControllerTest {

    private static final String PASSWORD = "clave1234";

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

    private ResultActions create(String token, String json) throws Exception {
        return mockMvc.perform(post("/api/v1/suppliers").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String company(String ruc) {
        return "{\"businessName\":\"Ferreteria Sur\",\"ruc\":\"%s\",\"type\":\"COMPANY\"}".formatted(ruc);
    }

    @Test
    void engineerCreatesUpdatesAndReadsASupplier() throws Exception {
        String token = bearer("ana", Role.ROLE_ENGINEER);

        String body = create(token, company("20123456789"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruc").value("20123456789"))
                .andExpect(jsonPath("$.type").value("COMPANY"))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");

        mockMvc.perform(put("/api/v1/suppliers/" + id).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessName\":\"Ferreteria Norte\",\"phone\":\"999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("Ferreteria Norte"))
                .andExpect(jsonPath("$.ruc").value("20123456789"));
        mockMvc.perform(get("/api/v1/suppliers/" + id).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("999"));
        mockMvc.perform(get("/api/v1/suppliers").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void aNaturalPersonNeedsNoRucAndABlankRucCountsAsAbsent() throws Exception {
        String token = bearer("ana", Role.ROLE_ACCOUNTING);

        create(token, "{\"businessName\":\"Juan\",\"type\":\"NATURAL_PERSON\"}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ruc").doesNotExist());
        create(token, "{\"businessName\":\"Pedro\",\"ruc\":\" \",\"type\":\"NATURAL_PERSON\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsDuplicateRucCompanyWithoutRucBadRucAndUnknownSupplier() throws Exception {
        String token = bearer("ana", Role.ROLE_ACCOUNTING);

        create(token, company("20123456789")).andExpect(status().isCreated());
        create(token, company("20123456789")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("expenses.error.duplicate-ruc"));
        create(token, "{\"businessName\":\"Sin ruc\",\"type\":\"COMPANY\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("expenses.error.ruc-required-for-company"));
        create(token, company("123")).andExpect(status().isBadRequest());
        create(token, "{\"businessName\":\"\",\"type\":\"COMPANY\"}").andExpect(status().isBadRequest());
        create(token, "{\"businessName\":\"Sin tipo\"}").andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/suppliers/999999").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("expenses.error.supplier-not-found"));
        mockMvc.perform(put("/api/v1/suppliers/999999").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"businessName\":\"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void managementReadsButCannotWrite() throws Exception {
        String token = bearer("gerente", Role.ROLE_MANAGEMENT);

        mockMvc.perform(get("/api/v1/suppliers").header("Authorization", token)).andExpect(status().isOk());
        create(token, company("20123456789")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/suppliers")).andExpect(status().isUnauthorized());
    }
}
