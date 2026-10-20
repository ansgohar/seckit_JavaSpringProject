package com.gohar.platform.seckit.infrastructure.persistence;

import com.gohar.platform.seckit.application.ports.UserRepositoryPort;
import com.gohar.platform.seckit.domain.model.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Adapter implementing UserRepositoryPort using Spring Data JPA.
 * Translates between domain models and JPA entities.
 */
@Repository
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository springDataUserRepository;

    public UserRepositoryAdapter(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return springDataUserRepository.existsByEmailIgnoreCase(email.trim());
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = new UserJpaEntity(
            user.getId().value(),
            user.getName(),
            user.getEmail().value(),
            user.getPasswordHash().value(),
            user.getStatus().name(),
            user.getCreatedAt()
        );

        springDataUserRepository.save(entity);
        return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return springDataUserRepository.findById(id.value())
            .map(entity -> User.reconstruct(
                id,
                entity.getName(),
                Email.of(entity.getEmail()),
                HashedPassword.of(entity.getPasswordHash()),
                UserStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt()
            ));
    }
}
