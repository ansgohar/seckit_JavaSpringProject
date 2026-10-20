package com.gohar.platform.seckit.domain.model;

import com.gohar.platform.seckit.domain.exception.DomainValidationException;
import java.util.UUID;

/**
 * Value object representing a unique User identifier.
 */
public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new DomainValidationException("UserId value cannot be null");
        }
    }

    public static UserId of(UUID uuid) {
        return new UserId(uuid);
    }

    public static UserId random() {
        return new UserId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
