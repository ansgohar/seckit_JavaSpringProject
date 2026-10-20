package com.gohar.platform.seckit.integration;

import com.gohar.platform.seckit.application.ports.PasswordEncoderPort;
import com.gohar.platform.seckit.infrastructure.persistence.SpringDataUserRepository;
import com.gohar.platform.seckit.infrastructure.persistence.UserJpaEntity;
import com.gohar.platform.seckit.infrastructure.ratelimit.RegistrationRateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("User Registration End-to-End Integration Tests")
class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private PasswordEncoderPort passwordEncoder;

    @Autowired
    private RegistrationRateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        rateLimitFilter.clear();
    }

    @Test
    @DisplayName("Complete end-to-end user registration flow with database persistence and PRG redirection")
    void completeUserRegistrationFlow() throws Exception {
        String testEmail = "alex.smith@example.com";
        String rawPassword = "StrongPassword2026!";

        // 1. Submit valid registration form
        mockMvc.perform(post("/register")
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr("10.10.10.10");
                    return request;
                })
                .param("name", "Alex Smith")
                .param("email", testEmail)
                .param("password", rawPassword))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"))
            .andExpect(flash().attribute("successMessage", "Registration completed successfully! Please sign in with your new account."));

        // 2. Verify database persistence
        Optional<UserJpaEntity> persisted = userRepository.findByEmailIgnoreCase(testEmail);
        assertThat(persisted).isPresent();

        UserJpaEntity entity = persisted.get();
        assertThat(entity.getName()).isEqualTo("Alex Smith");
        assertThat(entity.getEmail()).isEqualTo(testEmail);
        assertThat(entity.getStatus()).isEqualTo("ACTIVE");
        assertThat(entity.getCreatedAt()).isNotNull();

        // 3. Verify zero plain-text password leakage and BCrypt hash verification
        assertThat(entity.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(entity.getPasswordHash()).startsWith("$2a$12$");
        assertThat(passwordEncoder.matches(rawPassword, entity.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Rejects duplicate email registration case-insensitively and redisplays form")
    void rejectsDuplicateEmailRegistration() throws Exception {
        // Pre-register user
        mockMvc.perform(post("/register")
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr("10.10.10.20");
                    return request;
                })
                .param("name", "First User")
                .param("email", "duplicate.test@example.com")
                .param("password", "Password123!"))
            .andExpect(status().is3xxRedirection());

        // Attempt duplicate registration with uppercase email
        mockMvc.perform(post("/register")
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr("10.10.10.21");
                    return request;
                })
                .param("name", "Second User")
                .param("email", "DUPLICATE.TEST@EXAMPLE.COM")
                .param("password", "Password123!"))
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(model().attributeHasFieldErrors("registrationForm", "email"));

        // Confirm only 1 record exists in database
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rate limits registration attempts after 5 submissions from the same IP")
    void enforcesRateLimitingOnRegistrationEndpoint() throws Exception {
        String clientIp = "192.168.100.50";

        // Submit 5 registration attempts
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/register")
                    .with(csrf())
                    .with(request -> {
                        request.setRemoteAddr(clientIp);
                        return request;
                    })
                    .param("name", "User " + i)
                    .param("email", "user" + i + "@example.com")
                    .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection());
        }

        // 6th attempt from same IP must be throttled with HTTP 429
        mockMvc.perform(post("/register")
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                })
                .param("name", "User 6")
                .param("email", "user6@example.com")
                .param("password", "Password123!"))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "60"));
    }
}
