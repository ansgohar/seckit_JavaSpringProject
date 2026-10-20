package com.gohar.platform.seckit.domain.exception;

/**
 * Thrown when an account registration attempt uses an email address that is already registered.
 */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}
