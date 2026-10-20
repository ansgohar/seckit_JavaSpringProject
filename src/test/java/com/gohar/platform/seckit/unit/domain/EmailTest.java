package com.gohar.platform.seckit.unit.domain;

import com.gohar.platform.seckit.domain.exception.DomainValidationException;
import com.gohar.platform.seckit.domain.model.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Email Value Object Unit Tests")
class EmailTest {

    @Test
    @DisplayName("Normalizes email address by trimming whitespace and converting to lowercase")
    void normalizesEmailToLowercaseAndTrimmed() {
        Email email = Email.of("   User.NAME@Example.COM   ");
        assertThat(email.value()).isEqualTo("user.name@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "simple@example.com",
        "very.common@example.com",
        "disposable.style.email.with+symbol@example.com",
        "other.email-with-hyphen@example.com",
        "user.name+tag+sorting@example.com",
        "x@example.com",
        "example-indeed@strange-example.com"
    })
    @DisplayName("Accepts RFC 5322 valid email patterns")
    void acceptsValidEmails(String validEmail) {
        Email email = Email.of(validEmail);
        assertThat(email.value()).isEqualTo(validEmail.toLowerCase());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "plainaddress",
        "#@%^%#$@#$@#.com",
        "@example.com",
        "Joe Smith <email@example.com>",
        "email.example.com",
        "email@example@example.com",
        "email@example",
        "email@111.222.333.44444"
    })
    @DisplayName("Rejects malformed email patterns with DomainValidationException")
    void rejectsInvalidEmails(String invalidEmail) {
        assertThatThrownBy(() -> Email.of(invalidEmail))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Invalid email format");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Rejects blank email with DomainValidationException")
    void rejectsBlankEmail(String blankEmail) {
        assertThatThrownBy(() -> Email.of(blankEmail))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Email cannot be null or blank");
    }

    @Test
    @DisplayName("Rejects null email with DomainValidationException")
    void rejectsNullEmail() {
        assertThatThrownBy(() -> Email.of(null))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Email cannot be null or blank");
    }

    @Test
    @DisplayName("Rejects email exceeding RFC 5321 length of 254 characters")
    void rejectsEmailExceeding254Characters() {
        String longLocal = "a".repeat(243);
        String longEmail = longLocal + "@example.com"; // 243 + 12 = 255 chars
        assertThat(longEmail.length()).isEqualTo(255);

        assertThatThrownBy(() -> Email.of(longEmail))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("Email length cannot exceed 254 characters");
    }

    @Test
    @DisplayName("Enforces value object structural equality based on normalized value")
    void enforcesStructuralEquality() {
        Email email1 = Email.of("john.doe@example.com");
        Email email2 = Email.of("  JOHN.DOE@EXAMPLE.COM  ");
        Email email3 = Email.of("jane.doe@example.com");

        assertThat(email1).isEqualTo(email2);
        assertThat(email1.hashCode()).isEqualTo(email2.hashCode());
        assertThat(email1).isNotEqualTo(email3);
    }
}
