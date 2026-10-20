package com.jlpp.common.config;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUserIfNotExists("learner@jeps.edu.vn", "learner", "Nguyễn Văn Học Viên", "Password123@", RoleEnum.LEARNER, "ACTIVE");
        seedUserIfNotExists("manager@jeps.edu.vn", "manager", "Trần Văn Quản Lý", "Password123@", RoleEnum.MANAGER, "ACTIVE");
        seedUserIfNotExists("admin@jeps.edu.vn", "admin", "Lê Văn Quản Trị", "Password123@", RoleEnum.ADMIN, "ACTIVE");
        seedUserIfNotExists("disabled@jeps.edu.vn", "disabled_user", "Tài Khoản Vô Hiệu", "Password123@", RoleEnum.LEARNER, "DISABLED");
    }

    private void seedUserIfNotExists(String email, String username, String fullName, String rawPassword, RoleEnum role, String status) {
        if (userRepository.findByEmailIgnoreCase(email).isEmpty() && userRepository.findByUsername(username).isEmpty()) {
            User user = User.builder()
                    .email(email)
                    .username(username)
                    .fullName(fullName)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .status(status)
                    .build();
            userRepository.save(user);
            log.info("Initialized default account: {} ({}) with role {}", email, username, role);
        }
    }
}
