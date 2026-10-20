package com.gohar.platform.seckit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository interface for UserJpaEntity.
 */
@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    /**
     * Case-insensitive email existence check.
     *
     * @param email email to query
     * @return true if an account exists with this email (case-insensitive)
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Case-insensitive email lookup.
     *
     * @param email email to query
     * @return Optional containing entity if found
     */
    Optional<UserJpaEntity> findByEmailIgnoreCase(String email);
}
