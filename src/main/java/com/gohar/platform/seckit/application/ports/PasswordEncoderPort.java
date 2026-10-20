package com.gohar.platform.seckit.application.ports;

/**
 * Port interface for cryptographic password hashing.
 * Decouples the application use cases from Spring Security infrastructure.
 */
public interface PasswordEncoderPort {
    String encode(CharSequence rawPassword);
    boolean matches(CharSequence rawPassword, String encodedPassword);
}
