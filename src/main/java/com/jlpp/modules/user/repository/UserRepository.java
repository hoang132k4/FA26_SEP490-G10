package com.jlpp.modules.user.repository;

import com.jlpp.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);
    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByProviderId(String providerId);

    Optional<User> findByAuthProviderAndProviderId(String authProvider, String providerId);

    boolean existsByPhone(String phone);

    boolean existsByUsername(String username);
}
