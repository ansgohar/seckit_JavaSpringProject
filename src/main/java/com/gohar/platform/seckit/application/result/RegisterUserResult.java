package com.gohar.platform.seckit.application.result;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable result record returned upon successful user registration.
 */
public record RegisterUserResult(
    UUID userId,
    String name,
    String email,
    Instant createdAt
) {}
