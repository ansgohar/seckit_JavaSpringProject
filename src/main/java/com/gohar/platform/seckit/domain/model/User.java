package com.gohar.platform.seckit.domain.model;

import com.gohar.platform.seckit.domain.exception.DomainValidationException;

import java.time.Instant;
import java.util.Objects;

/**
 * User Aggregate Root entity in the core Domain layer.
 * Contains business logic and invariants with zero framework annotations.
 */
public class User {

    private final UserId id;
    private final String name;
    private final Email email;
    private final HashedPassword passwordHash;
    private final UserStatus status;
    private final Instant createdAt;

    private User(UserId id, String name, Email email, HashedPassword passwordHash, UserStatus status, Instant createdAt) {
        if (id == null) {
            throw new DomainValidationException("User ID cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("Name cannot be null or blank");
        }
        String trimmedName = name.trim();
        if (trimmedName.length() > 100) {
            throw new DomainValidationException("Name cannot exceed 100 characters");
        }
        if (email == null) {
            throw new DomainValidationException("Email cannot be null");
        }
        if (passwordHash == null) {
            throw new DomainValidationException("Password hash cannot be null");
        }
        if (status == null) {
            throw new DomainValidationException("User status cannot be null");
        }
        if (createdAt == null) {
            throw new DomainValidationException("Creation timestamp cannot be null");
        }

        this.id = id;
        this.name = trimmedName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static User create(UserId id, String name, Email email, HashedPassword passwordHash, Instant createdAt) {
        return new User(id, name, email, passwordHash, UserStatus.ACTIVE, createdAt);
    }

    public static User reconstruct(UserId id, String name, Email email, HashedPassword passwordHash, UserStatus status, Instant createdAt) {
        return new User(id, name, email, passwordHash, status, createdAt);
    }

    public UserId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public HashedPassword getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email=" + email +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
