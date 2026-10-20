package com.gohar.platform.seckit.domain.exception;

/**
 * Thrown when a domain entity, value object invariant, or business rule is violated.
 */
public class DomainValidationException extends RuntimeException {
    public DomainValidationException(String message) {
        super(message);
    }
}
