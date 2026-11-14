package com.brasper.platform.iam.infrastructure.pipeline;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.brasper.platform.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class)
class CorsTest {
    private static final String FRONTEND = "https://brasper-frontend.vercel.app";
    @Autowired MockMvc mvc;

    @Test void preflightFromTheFrontendIsAnsweredWithoutAToken() throws Exception {
        mvc.perform(options("/api/v1/invoices").header("Origin", FRONTEND)
                .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test void theBrowserMayReadThePageTotalHeader() throws Exception {
        mvc.perform(post("/api/v1/auth/sign-in").header("Origin", FRONTEND)
                .contentType("application/json").content("{\"username\":\"nadie\",\"password\":\"x\"}"))
                .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND))
                .andExpect(header().string("Access-Control-Expose-Headers", "X-Total-Count, Location"));
    }

    @Test void anotherOriginIsRejected() throws Exception {
        mvc.perform(options("/api/v1/invoices").header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
