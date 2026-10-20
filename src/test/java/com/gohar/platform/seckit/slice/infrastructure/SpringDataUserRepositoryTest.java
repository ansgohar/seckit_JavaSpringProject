package com.gohar.platform.seckit.slice.infrastructure;

import com.gohar.platform.seckit.infrastructure.persistence.SpringDataUserRepository;
import com.gohar.platform.seckit.infrastructure.persistence.UserJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@DisplayName("SpringDataUserRepository JPA Slice Tests (PostgreSQL Mode)")
class SpringDataUserRepositoryTest {

    @Autowired
    private SpringDataUserRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("existsByEmailIgnoreCase returns true when email exists and false when it does not")
    void existsByEmailChecksCaseInsensitiveUniqueness() {
        UserJpaEntity entity = new UserJpaEntity(
            UUID.randomUUID(),
            "John Doe",
            "john.doe@example.com",
            "$2a$12$e8Y4c/y5qE6l0mG4I1aK1Ou2A9fLz9wN1b6h8d0j4k2m6n8p0r2t4",
            "ACTIVE",
            Instant.now()
        );
        entityManager.persistAndFlush(entity);

        assertThat(repository.existsByEmailIgnoreCase("john.doe@example.com")).isTrue();
        assertThat(repository.existsByEmailIgnoreCase("JOHN.DOE@EXAMPLE.COM")).isTrue();
        assertThat(repository.existsByEmailIgnoreCase("unknown@example.com")).isFalse();
    }

    @Test
    @DisplayName("Persists user entity and successfully retrieves by ID")
    void persistsAndRetrievesUser() {
        UUID id = UUID.randomUUID();
        UserJpaEntity entity = new UserJpaEntity(
            id,
            "Jane Smith",
            "jane.smith@example.com",
            "$2a$12$e8Y4c/y5qE6l0mG4I1aK1Ou2A9fLz9wN1b6h8d0j4k2m6n8p0r2t4",
            "ACTIVE",
            Instant.now()
        );

        UserJpaEntity saved = repository.save(entity);
        entityManager.flush();

        assertThat(saved.getId()).isEqualTo(id);
        assertThat(repository.findById(id)).isPresent();
    }
}
