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
}
