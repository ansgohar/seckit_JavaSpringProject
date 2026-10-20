package com.gohar.platform.seckit.domain.model;

import com.gohar.platform.seckit.domain.exception.DomainValidationException;

/**
 * Value object encapsulating a cryptographically validated BCrypt password hash.
 */
public record HashedPassword(String value) {

    public HashedPassword {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("Password hash cannot be null or blank");
        }
        if (!value.startsWith("$2a$") && !value.startsWith("$2b$") && !value.startsWith("$2y$")) {
            throw new DomainValidationException("Invalid BCrypt hash format");
        }
        if (value.length() != 60) {
            throw new DomainValidationException("BCrypt hash must be exactly 60 characters");
        }
    }

    public static HashedPassword of(String hash) {
        return new HashedPassword(hash);
    }

    @Override
    public String toString() {
        return "HashedPassword[PROTECTED]";
    }
}
