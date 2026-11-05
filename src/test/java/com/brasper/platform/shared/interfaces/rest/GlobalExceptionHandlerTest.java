package com.brasper.platform.shared.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler(messages))
                .build();
    }

    @Test
    void illegalArgumentIsBadRequestInSpanish() throws Exception {
        mockMvc.perform(get("/bad-argument").header("Accept-Language", "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.invalid-argument"))
                .andExpect(jsonPath("$.detail").value("Valor inválido: negative amount"));
    }

    @Test
    void illegalStateIsConflictInEnglish() throws Exception {
        mockMvc.perform(get("/bad-state").header("Accept-Language", "en"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("error.invalid-state"))
                .andExpect(jsonPath("$.detail").value("The operation is not allowed in the current state: already closed"));
    }

    @Test
    void invalidBodyListsTheFailingFields() throws Exception {
        mockMvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.validation"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @RestController
    static class ThrowingController {

        record Body(@NotBlank String name) {
        }

        @GetMapping("/bad-argument")
        void badArgument() {
            throw new IllegalArgumentException("negative amount");
        }

        @GetMapping("/bad-state")
        void badState() {
            throw new IllegalStateException("already closed");
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody Body body) {
        }
    }
}
