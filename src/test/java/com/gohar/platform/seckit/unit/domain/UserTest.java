package com.gohar.platform.seckit.unit.domain;

import com.gohar.platform.seckit.domain.exception.DomainValidationException;
import com.gohar.platform.seckit.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User Aggregate Root Unit Tests")
class UserTest {

    private final UserId validId = UserId.of(UUID.randomUUID());
    private final Email validEmail = Email.of("jane.doe@example.com");
    private final HashedPassword validHash = HashedPassword.of("$2a$12$e8Y4c/y5qE6l0mG4I1aK1Ou2A9fLz9wN1b6h8d0j4k2m6n8p0r2t4");

    @Test
    @DisplayName("Creates valid User aggregate root in ACTIVE status")
    void createsValidUserInActiveStatus() {
        User user = User.create(validId, "Jane Doe", validEmail, validHash, Instant.now());

        assertThat(user.getId()).isEqualTo(validId);
        assertThat(user.getName()).isEqualTo("Jane Doe");
        assertThat(user.getEmail()).isEqualTo(validEmail);
        assertThat(user.getPasswordHash()).isEqualTo(validHash);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Trims whitespace from valid user name")
    void trimsWhitespaceFromUserName() {
        User user = User.create(validId, "   Jane Doe   ", validEmail, validHash, Instant.now());
        assertThat(user.getName()).isEqualTo("Jane Doe");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("Rejects blank user name with DomainValidationException")
    void rejectsBlankUserName(String blankName) {
        assertThatThrownBy(() -> User.create(validId, blankName, validEmail, validHash, Instant.now()))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Name cannot be null or blank");
    }

    @Test
    @DisplayName("Rejects null user name with DomainValidationException")
    void rejectsNullUserName() {
        assertThatThrownBy(() -> User.create(validId, null, validEmail, validHash, Instant.now()))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Name cannot be null or blank");
    }

    @Test
    @DisplayName("Rejects user name exceeding 100 characters with DomainValidationException")
    void rejectsUserNameExceeding100Characters() {
        String longName = "A".repeat(101);
        assertThatThrownBy(() -> User.create(validId, longName, validEmail, validHash, Instant.now()))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Name cannot exceed 100 characters");
    }

    @Test
    @DisplayName("Rejects null email with DomainValidationException")
    void rejectsNullEmail() {
        assertThatThrownBy(() -> User.create(validId, "Jane Doe", null, validHash, Instant.now()))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Email cannot be null");
    }

    @Test
    @DisplayName("Rejects null password hash with DomainValidationException")
    void rejectsNullPasswordHash() {
        assertThatThrownBy(() -> User.create(validId, "Jane Doe", validEmail, null, Instant.now()))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Password hash cannot be null");
    }
}
