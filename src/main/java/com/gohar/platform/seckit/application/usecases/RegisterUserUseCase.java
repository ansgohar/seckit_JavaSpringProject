package com.gohar.platform.seckit.application.usecases;

import com.gohar.platform.seckit.application.command.RegisterUserCommand;
import com.gohar.platform.seckit.application.ports.PasswordEncoderPort;
import com.gohar.platform.seckit.application.ports.UserRepositoryPort;
import com.gohar.platform.seckit.application.result.RegisterUserResult;
import com.gohar.platform.seckit.domain.exception.DomainValidationException;
import com.gohar.platform.seckit.domain.exception.DuplicateEmailException;
import com.gohar.platform.seckit.domain.model.Email;
import com.gohar.platform.seckit.domain.model.HashedPassword;
import com.gohar.platform.seckit.domain.model.User;
import com.gohar.platform.seckit.domain.model.UserId;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Single-method use case interactor orchestrating new user account registration.
 * Pure application logic decoupled from HTTP, frameworks, and storage.
 */
@Service
public class RegisterUserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;

    public RegisterUserUseCase(UserRepositoryPort userRepository, PasswordEncoderPort passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Executes the registration use case.
     *
     * @param command input data containing name, email, and raw password
     * @return result record with registered user details
     * @throws DuplicateEmailException if email is already in use
     * @throws DomainValidationException if validation invariants fail
     */
    public RegisterUserResult execute(RegisterUserCommand command) {
        if (command == null) {
            throw new DomainValidationException("Registration command cannot be null");
        }

        // Validate raw password bounds before hashing
        if (command.rawPassword() == null || command.rawPassword().isBlank()) {
            throw new DomainValidationException("Password is required and cannot be blank");
        }
        if (command.rawPassword().length() < 8) {
            throw new DomainValidationException("Password must contain at least 8 characters");
        }
        if (command.rawPassword().length() > 128) {
            throw new DomainValidationException("Password length cannot exceed 128 characters");
        }

        // Validate and normalize email address
        Email normalizedEmail = Email.of(command.email());

        // Enforce application-layer email uniqueness
        if (userRepository.existsByEmail(normalizedEmail.value())) {
            throw new DuplicateEmailException("This email address is already in use.");
        }

        // Hash password securely via port
        String rawHash = passwordEncoder.encode(command.rawPassword());
        HashedPassword hashedPassword = HashedPassword.of(rawHash);

        // Create domain aggregate root
        User user = User.create(
            UserId.random(),
            command.name(),
            normalizedEmail,
            hashedPassword,
            Instant.now()
        );

        // Persist through port
        User savedUser = userRepository.save(user);

        return new RegisterUserResult(
            savedUser.getId().value(),
            savedUser.getName(),
            savedUser.getEmail().value(),
            savedUser.getCreatedAt()
        );
    }
}
