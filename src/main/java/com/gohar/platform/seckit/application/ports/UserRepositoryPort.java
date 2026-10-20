package com.gohar.platform.seckit.application.ports;

import com.gohar.platform.seckit.domain.model.User;
import com.gohar.platform.seckit.domain.model.UserId;

import java.util.Optional;

/**
 * Persistence port interface for User aggregate management.
 * Inverts dependency so Application layer is decoupled from database infrastructure.
 */
public interface UserRepositoryPort {

    /**
     * Checks if a user already exists with the given email address (case-insensitive).
     *
     * @param email normalized or raw email address
     * @return true if an account exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Persists a User aggregate entity.
     *
     * @param user domain User aggregate to save
     * @return persisted User aggregate
     */
    User save(User user);

    /**
     * Finds a user by their unique User ID.
     *
     * @param id the user ID
     * @return Optional containing the User if found
     */
    Optional<User> findById(UserId id);
}
