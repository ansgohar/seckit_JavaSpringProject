package com.gohar.platform.seckit.unit.application;

import com.gohar.platform.seckit.application.command.RegisterUserCommand;
import com.gohar.platform.seckit.application.ports.PasswordEncoderPort;
import com.gohar.platform.seckit.application.ports.UserRepositoryPort;
import com.gohar.platform.seckit.application.result.RegisterUserResult;
import com.gohar.platform.seckit.application.usecases.RegisterUserUseCase;
import com.gohar.platform.seckit.domain.exception.DomainValidationException;
import com.gohar.platform.seckit.domain.exception.DuplicateEmailException;
import com.gohar.platform.seckit.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserUseCase Unit Tests (Zero Spring Context)")
class RegisterUserUseCaseTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoderPort passwordEncoder;

    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Successfully registers a new user with normalized email and hashed password")
    void successfullyRegistersUser() {
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPassword123")).thenReturn("$2a$12$e8Y4c/y5qE6l0mG4I1aK1Ou2A9fLz9wN1b6h8d0j4k2m6n8p0r2t4");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterUserCommand command = new RegisterUserCommand(
            "Jane Doe",
            "  JANE.DOE@EXAMPLE.COM  ",
            "ValidPassword123"
        );

        RegisterUserResult result = useCase.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isNotNull();
        assertThat(result.name()).isEqualTo("Jane Doe");
        assertThat(result.email()).isEqualTo("jane.doe@example.com");
        assertThat(result.createdAt()).isNotNull();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo("Jane Doe");
        assertThat(savedUser.getEmail().value()).isEqualTo("jane.doe@example.com");
        assertThat(savedUser.getPasswordHash().value()).startsWith("$2a$12$");
    }

    @Test
    @DisplayName("Throws DuplicateEmailException when email is already registered")
    void throwsDuplicateEmailExceptionWhenEmailExists() {
        when(userRepository.existsByEmail("jane.doe@example.com")).thenReturn(true);

        RegisterUserCommand command = new RegisterUserCommand(
            "Jane Doe",
            "jane.doe@example.com",
            "ValidPassword123"
        );

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(DuplicateEmailException.class)
            .hasMessageContaining("already in use");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Throws DomainValidationException when raw password is shorter than 8 characters")
    void throwsValidationExceptionWhenPasswordTooShort() {
        RegisterUserCommand command = new RegisterUserCommand(
            "Jane Doe",
            "jane.doe@example.com",
            "short"
        );

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("at least 8 characters");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Throws DomainValidationException when raw password exceeds 128 characters")
    void throwsValidationExceptionWhenPasswordTooLong() {
        RegisterUserCommand command = new RegisterUserCommand(
            "Jane Doe",
            "jane.doe@example.com",
            "a".repeat(129)
        );

        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(DomainValidationException.class)
            .hasMessageContaining("cannot exceed 128 characters");

        verify(userRepository, never()).save(any());
    }
}
