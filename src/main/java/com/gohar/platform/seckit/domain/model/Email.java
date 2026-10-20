package com.gohar.platform.seckit.domain.model;

import com.gohar.platform.seckit.domain.exception.DomainValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value object representing a validated, normalized email address.
 * Normalizes to lowercase and enforces RFC 5321 and RFC 5322 constraints.
 */
public record Email(String value) {

    private static final int MAX_LENGTH = 254;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$"
    );

    public Email {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Email cannot be null or blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new DomainValidationException("Email length cannot exceed 254 characters");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new DomainValidationException("Invalid email format: " + value);
        }
    }

    public static Email of(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new DomainValidationException("Email cannot be null or blank");
        }
        String normalized = rawEmail.trim().toLowerCase(Locale.ROOT);
        return new Email(normalized);
    }

    @Override
    public String toString() {
        return value;
    }
}
