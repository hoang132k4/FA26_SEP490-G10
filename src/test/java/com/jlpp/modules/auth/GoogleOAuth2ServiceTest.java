package com.jlpp.modules.auth;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.auth.service.GoogleOAuth2Service;
import com.jlpp.modules.auth.service.impl.GoogleOAuth2ServiceImpl;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GoogleOAuth2ServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private GoogleOAuth2Service googleOAuth2Service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);
        googleOAuth2Service = new GoogleOAuth2ServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Case 1: Genuinely new Google user registers as Learner with ROLE_LEARNER")
    void testNewGoogleUser_CreatesLearnerAccount() {
        String sub = "google_sub_123456";
        String email = "newlearner@gmail.com";
        String name = "Google Learner";
        String picture = "https://lh3.googleusercontent.com/avatar.jpg";

        when(userRepository.findByProviderId(sub)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("newlearner")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(500L);
            return u;
        });

        User result = googleOAuth2Service.processGoogleUser(sub, email, true, name, picture);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("newlearner@gmail.com");
        assertThat(result.getFullName()).isEqualTo("Google Learner");
        assertThat(result.getRole()).isEqualTo(RoleEnum.LEARNER);
        assertThat(result.getAuthProvider()).isEqualTo("GOOGLE");
        assertThat(result.getProviderId()).isEqualTo(sub);
        assertThat(result.getAvatarUrl()).isEqualTo(picture);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getPhone()).isNull(); // Do not fabricate phone number

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getRole()).isEqualTo(RoleEnum.LEARNER);
        assertThat(saved.getPassword()).isNotNull(); // Unguessable random hashed password
    }

    @Test
    @DisplayName("Case 2: Repeated Google login reuses existing linked account")
    void testRepeatedGoogleLogin_ReusesLinkedAccount() {
        String sub = "google_sub_123456";
        String email = "existing@gmail.com";

        User existingUser = User.builder()
                .fullName("Existing Student")
                .email(email)
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .authProvider("GOOGLE")
                .providerId(sub)
                .phone("0912345678")
                .build();
        existingUser.setId(200L);

        when(userRepository.findByProviderId(sub)).thenReturn(Optional.of(existingUser));

        User result = googleOAuth2Service.processGoogleUser(sub, email, true, "Existing Student", null);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getEmail()).isEqualTo(email);
        verify(userRepository, never()).save(any(User.class)); // No duplication
    }

    @Test
    @DisplayName("Case 3: Existing account with same verified email is safely linked without role escalation")
    void testGoogleLogin_ExistingAccount_SafelyLinksAccountPreservingRole() {
        String sub = "google_new_sub_789";
        String email = "manager@jeps.edu.vn";

        User existingManager = User.builder()
                .fullName("Manager User")
                .email(email)
                .role(RoleEnum.MANAGER)
                .status("ACTIVE")
                .authProvider("LOCAL")
                .build();
        existingManager.setId(10L);

        when(userRepository.findByProviderId(sub)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(existingManager));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User linked = googleOAuth2Service.processGoogleUser(sub, email, true, "Manager User", "https://avatar.url/img.png");

        assertThat(linked).isNotNull();
        assertThat(linked.getId()).isEqualTo(10L);
        assertThat(linked.getRole()).isEqualTo(RoleEnum.MANAGER);
        assertThat(linked.getProviderId()).isEqualTo(sub);
        verify(userRepository).save(existingManager);
    }

    @Test
    @DisplayName("Case 3b: Existing Learner with same verified email is safely linked to Google identity")
    void testGoogleLogin_ExistingLearner_SafelyLinksAccount() {
        String sub = "google_new_sub_456";
        String email = "learner@gmail.com";

        User existingLearner = User.builder()
                .fullName("Local Learner")
                .email(email)
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .authProvider("LOCAL")
                .build();
        existingLearner.setId(101L);

        when(userRepository.findByProviderId(sub)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(existingLearner));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User linked = googleOAuth2Service.processGoogleUser(sub, email, true, "Local Learner Updated", "https://avatar.url/img.png");

        assertThat(linked).isNotNull();
        assertThat(linked.getId()).isEqualTo(101L);
        assertThat(linked.getProviderId()).isEqualTo(sub);
        assertThat(linked.getAuthProvider()).isEqualTo("GOOGLE");
        assertThat(linked.getAvatarUrl()).isEqualTo("https://avatar.url/img.png");
        verify(userRepository).save(existingLearner);
    }

    @Test
    @DisplayName("Case 4: Unverified Google email is rejected")
    void testGoogleLogin_UnverifiedEmail_ThrowsException() {
        String sub = "google_unverified_sub";
        String email = "unverified@gmail.com";

        assertThatThrownBy(() -> googleOAuth2Service.processGoogleUser(sub, email, false, "Unverified User", null))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("chưa được xác minh email");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Case 5: Disabled or Locked linked user is rejected")
    void testGoogleLogin_DisabledUser_ThrowsException() {
        String sub = "google_sub_disabled";
        String email = "disabled@gmail.com";

        User disabledUser = User.builder()
                .fullName("Disabled User")
                .email(email)
                .role(RoleEnum.LEARNER)
                .status("DISABLED")
                .authProvider("GOOGLE")
                .providerId(sub)
                .build();

        when(userRepository.findByProviderId(sub)).thenReturn(Optional.of(disabledUser));

        assertThatThrownBy(() -> googleOAuth2Service.processGoogleUser(sub, email, true, "Disabled User", null))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("bị khóa hoặc vô hiệu hóa");
    }

    @Test
    @DisplayName("Case 6: Missing sub or email throws invalid token exception")
    void testGoogleLogin_MissingSubOrEmail_ThrowsException() {
        assertThatThrownBy(() -> googleOAuth2Service.processGoogleUser(null, "test@gmail.com", true, "Name", null))
                .isInstanceOf(OAuth2AuthenticationException.class);

        assertThatThrownBy(() -> googleOAuth2Service.processGoogleUser("sub123", "", true, "Name", null))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
