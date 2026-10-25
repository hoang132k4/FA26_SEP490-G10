package com.jlpp.modules.auth;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.auth.dto.RegisterRequest;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RegisterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /auth/register should return register page with empty form")
    void testRegisterPage_DisplaysForm() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeExists("registerRequest"));
    }

    @Test
    @DisplayName("UC-01 Normal Flow: Valid registration creates Learner account, hashes password, and redirects to login")
    void testRegister_Success() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Nguyễn Văn A")
                        .param("email", "nguyenvana@example.com")
                        .param("phone", "0987654321")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"))
                .andExpect(flash().attributeExists("successMessage"));

        Optional<User> savedOpt = userRepository.findByEmailIgnoreCase("nguyenvana@example.com");
        assertThat(savedOpt).isPresent();

        User saved = savedOpt.get();
        assertThat(saved.getFullName()).isEqualTo("Nguyễn Văn A");
        assertThat(saved.getPhone()).isEqualTo("0987654321");
        assertThat(saved.getRole()).isEqualTo(RoleEnum.LEARNER);
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getUsername()).isNotBlank();
        // Rule: Password Security (BCrypt)
        assertThat(saved.getPassword()).isNotEqualTo("SecurePass123!");
        assertThat(passwordEncoder.matches("SecurePass123!", saved.getPassword())).isTrue();
    }

    @Test
    @DisplayName("UC-01 A1: Empty fields return validation errors")
    void testRegister_A1_EmptyFields() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "")
                        .param("email", "")
                        .param("phone", "")
                        .param("password", "")
                        .param("confirmPassword", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("registerRequest", "fullName"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "email"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "phone"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "password"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "confirmPassword"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("UC-01 A2: Name contains only spaces or is shorter than 2 characters")
    void testRegister_A2_InvalidName() throws Exception {
        // Name with only spaces
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "   ")
                        .param("email", "testname@example.com")
                        .param("phone", "0912345678")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "fullName"));

        // Name too short (1 character)
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "A")
                        .param("email", "testname@example.com")
                        .param("phone", "0912345678")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "fullName"));

        // Name with leading/trailing spaces resulting in 1 character
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "  A  ")
                        .param("email", "testname@example.com")
                        .param("phone", "0912345678")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "fullName"));
    }

    @Test
    @DisplayName("UC-01 A3: Invalid email format (missing @ or domain, or contains spaces)")
    void testRegister_A3_InvalidEmail() throws Exception {
        // Missing @
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Trần Văn B")
                        .param("email", "notanemail.com")
                        .param("phone", "0912345678")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "email"));

        // Missing domain
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Trần Văn B")
                        .param("email", "user@")
                        .param("phone", "0912345678")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "email"));

        // Contains spaces
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Trần Văn B")
                        .param("email", "user name@domain.com")
                        .param("phone", "0912345678")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "email"));
    }

    @Test
    @DisplayName("UC-01 A4: Invalid phone number format (not 10 digits starting with 0)")
    void testRegister_A4_InvalidPhone() throws Exception {
        // Does not start with 0
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Lê Văn C")
                        .param("email", "levanc@example.com")
                        .param("phone", "1912345678")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "phone"));

        // 9 digits
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Lê Văn C")
                        .param("email", "levanc@example.com")
                        .param("phone", "091234567")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "phone"));

        // 11 digits
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Lê Văn C")
                        .param("email", "levanc@example.com")
                        .param("phone", "09123456789")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "phone"));

        // Contains non-digit
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Lê Văn C")
                        .param("email", "levanc@example.com")
                        .param("phone", "091234567a")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "phone"));
    }

    @Test
    @DisplayName("UC-01 A5: Password complexity validation (missing uppercase, lowercase, digit, or special character)")
    void testRegister_A5_InvalidPasswordComplexity() throws Exception {
        // Missing uppercase
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Phạm Văn D")
                        .param("email", "phamvand@example.com")
                        .param("phone", "0912345678")
                        .param("password", "password123!")
                        .param("confirmPassword", "password123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "password"));

        // Missing lowercase
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Phạm Văn D")
                        .param("email", "phamvand@example.com")
                        .param("phone", "0912345678")
                        .param("password", "PASSWORD123!")
                        .param("confirmPassword", "PASSWORD123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "password"));

        // Missing number
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Phạm Văn D")
                        .param("email", "phamvand@example.com")
                        .param("phone", "0912345678")
                        .param("password", "PasswordSpecial!")
                        .param("confirmPassword", "PasswordSpecial!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "password"));

        // Missing special character
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Phạm Văn D")
                        .param("email", "phamvand@example.com")
                        .param("phone", "0912345678")
                        .param("password", "Password123")
                        .param("confirmPassword", "Password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "password"));

        // Less than 8 characters
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Phạm Văn D")
                        .param("email", "phamvand@example.com")
                        .param("phone", "0912345678")
                        .param("password", "Pass1!")
                        .param("confirmPassword", "Pass1!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "password"));
    }

    @Test
    @DisplayName("UC-01 A6: Confirm password does not match password")
    void testRegister_A6_PasswordMismatch() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "Hoàng Văn E")
                        .param("email", "hoangvane@example.com")
                        .param("phone", "0912345678")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "DifferentPass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "confirmPassword"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("UC-01 A7: Duplicate email should reject registration")
    void testRegister_A7_DuplicateEmail() throws Exception {
        // Pre-create an existing user
        userRepository.save(User.builder()
                .email("existing@example.com")
                .username("existing_user")
                .fullName("Existing User")
                .phone("0911111111")
                .password(passwordEncoder.encode("ExistingPass123!"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build());

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "New User")
                        .param("email", "existing@example.com")
                        .param("phone", "0922222222")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "email"));

        // Only the pre-existing user exists
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("UC-01 A7: Duplicate phone number should reject registration")
    void testRegister_A7_DuplicatePhone() throws Exception {
        // Pre-create an existing user
        userRepository.save(User.builder()
                .email("user1@example.com")
                .username("user1")
                .fullName("User One")
                .phone("0988888888")
                .password(passwordEncoder.encode("User1Pass123!"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build());

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "User Two")
                        .param("email", "user2@example.com")
                        .param("phone", "0988888888")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "phone"));

        // Only the pre-existing user exists
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Data Business Rule: Unique username is properly generated and deduplicated")
    void testRegister_UniqueUsernameDeduplication() throws Exception {
        // User 1
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "User One")
                        .param("email", "learner@example.com")
                        .param("phone", "0911111111")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().is3xxRedirection());

        // User 2 with same base email prefix
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .param("fullName", "User Two")
                        .param("email", "learner@domain.org")
                        .param("phone", "0922222222")
                        .param("password", "SecurePass123!")
                        .param("confirmPassword", "SecurePass123!"))
                .andExpect(status().is3xxRedirection());

        User u1 = userRepository.findByEmailIgnoreCase("learner@example.com").orElseThrow();
        User u2 = userRepository.findByEmailIgnoreCase("learner@domain.org").orElseThrow();

        assertThat(u1.getUsername()).isEqualTo("learner");
        assertThat(u2.getUsername()).isEqualTo("learner1");
        assertThat(u1.getUsername()).isNotEqualTo(u2.getUsername());
    }
}
