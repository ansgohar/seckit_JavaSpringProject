package com.gohar.platform.seckit.slice.presentation;

import com.gohar.platform.seckit.application.result.RegisterUserResult;
import com.gohar.platform.seckit.application.usecases.RegisterUserUseCase;
import com.gohar.platform.seckit.domain.exception.DuplicateEmailException;
import com.gohar.platform.seckit.infrastructure.ratelimit.RegistrationRateLimitFilter;
import com.gohar.platform.seckit.presentation.controller.RegistrationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RegistrationController MVC Slice Tests")
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private RegistrationRateLimitFilter rateLimitFilter;

    @Test
    @DisplayName("GET /register displays registration form with empty backing bean")
    void getRegisterDisplaysForm() throws Exception {
        mockMvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(model().attributeExists("registrationForm"));
    }

    @Test
    @DisplayName("POST /register with valid data redirects to /login with flash success message (PRG)")
    void postRegisterSuccessRedirectsToLogin() throws Exception {
        when(registerUserUseCase.execute(any())).thenReturn(
            new RegisterUserResult(UUID.randomUUID(), "Jane Doe", "jane.doe@example.com", Instant.now())
        );

        mockMvc.perform(post("/register")
                .with(csrf())
                .param("name", "Jane Doe")
                .param("email", "jane.doe@example.com")
                .param("password", "SecretPassword123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"))
            .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("POST /register with blank inputs redisplays form with validation errors")
    void postRegisterBlankInputsRedisplaysFormWithErrors() throws Exception {
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("name", "")
                .param("email", "")
                .param("password", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(model().attributeHasFieldErrors("registrationForm", "name", "email", "password"));
    }

    @Test
    @DisplayName("POST /register with password too short redisplays form with password error")
    void postRegisterShortPasswordRedisplaysForm() throws Exception {
        mockMvc.perform(post("/register")
                .with(csrf())
                .param("name", "Jane Doe")
                .param("email", "jane.doe@example.com")
                .param("password", "12345"))
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(model().attributeHasFieldErrors("registrationForm", "password"));
    }

    @Test
    @DisplayName("POST /register with duplicate email redisplays form with email error and clears password")
    void postRegisterDuplicateEmailRedisplaysFormWithError() throws Exception {
        when(registerUserUseCase.execute(any())).thenThrow(
            new DuplicateEmailException("This email address is already in use.")
        );

        mockMvc.perform(post("/register")
                .with(csrf())
                .param("name", "Jane Doe")
                .param("email", "existing@example.com")
                .param("password", "ValidPassword123"))
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));
    }
}
