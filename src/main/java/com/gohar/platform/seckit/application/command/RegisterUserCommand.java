package com.gohar.platform.seckit.application.command;

/**
 * Immutable command record encapsulating raw visitor registration input.
 */
public record RegisterUserCommand(
    String name,
    String email,
    String rawPassword
) {}
