package com.jlpp.modules.profile;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.profile.dto.EditProfileRequest;
import com.jlpp.modules.profile.dto.ProfileResponse;
import com.jlpp.modules.profile.service.impl.ProfileServiceImpl;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: ProfileService (Screens 5 & 6 - Learner Profile & Profile Editing)")
class ProfileServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileServiceImpl(userRepository);
    }

    private User createSampleUser() {
        User user = User.builder()
                .fullName("Nguyen Van Hoc Vien")
                .email("learner@jeps.edu.vn")
                .phone("0912345678")
                .username("learner")
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .currentJlptLevel("N4")
                .targetJlptLevel("N3")
                .avatarUrl("/images/default-avatar.png")
                .build();
        user.setId(10L);
        return user;
    }

    @Test
    @DisplayName("Normal: Lấy thông tin Profile của học viên thành công (Screen 5)")
    void getProfile_success() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));

        ProfileResponse res = profileService.getProfile("learner@jeps.edu.vn");

        assertNotNull(res);
        assertEquals("Nguyen Van Hoc Vien", res.getFullName());
        assertEquals("learner@jeps.edu.vn", res.getEmail());
        assertEquals("0912345678", res.getPhone());
        assertEquals("learner", res.getUsername());
        assertEquals(RoleEnum.LEARNER, res.getRole());
        assertEquals("ACTIVE", res.getStatus());
        assertEquals("N4", res.getCurrentJlptLevel());
        assertEquals("N3", res.getTargetJlptLevel());
    }

    @Test
    @DisplayName("Abnormal: Lấy Profile khi không tìm thấy người dùng ném ra IllegalArgumentException")
    void getProfile_userNotFound() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> profileService.getProfile("unknown@jeps.edu.vn"));
    }

    @Test
    @DisplayName("Normal: Lấy Edit Profile Form DTO thành công (Screen 6)")
    void getEditProfileForm_success() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));

        EditProfileRequest form = profileService.getEditProfileForm("learner@jeps.edu.vn");

        assertNotNull(form);
        assertEquals("Nguyen Van Hoc Vien", form.getFullName());
        assertEquals("learner@jeps.edu.vn", form.getEmail());
        assertEquals("0912345678", form.getPhone());
    }

    @Test
    @DisplayName("Normal: Cập nhật Profile thành công không đổi avatar (Screen 6)")
    void updateProfile_success_withoutAvatar() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EditProfileRequest req = EditProfileRequest.builder()
                .fullName("Nguyen Van Hoc Vien Moi")
                .email("learner@jeps.edu.vn")
                .phone("0988776655")
                .build();

        ProfileResponse res = profileService.updateProfile("learner@jeps.edu.vn", req, null);

        assertNotNull(res);
        assertEquals("Nguyen Van Hoc Vien Moi", user.getFullName());
        assertEquals("0988776655", user.getPhone());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Abnormal: Cập nhật email trùng với người dùng khác ném ra IllegalArgumentException (Screen 6)")
    void updateProfile_duplicateEmail_throwsException() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("other@jeps.edu.vn")).thenReturn(true);

        EditProfileRequest req = EditProfileRequest.builder()
                .fullName("Nguyen Van A")
                .email("other@jeps.edu.vn")
                .build();

        assertThrows(IllegalArgumentException.class, () -> profileService.updateProfile("learner@jeps.edu.vn", req, null));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Normal: Cập nhật thông tin với đầy đủ trường hợp lệ (Screen 6)")
    void updateProfile_validFields_success() {
        User user = createSampleUser();
        when(userRepository.findByEmailIgnoreCase("learner@jeps.edu.vn")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EditProfileRequest req = EditProfileRequest.builder()
                .fullName("Le Thi B")
                .email("learner@jeps.edu.vn")
                .phone("0911223344")
                .build();

        ProfileResponse res = profileService.updateProfile("learner@jeps.edu.vn", req, null);

        assertNotNull(res);
        assertEquals("Le Thi B", res.getFullName());
        assertEquals("0911223344", res.getPhone());
        verify(userRepository).save(user);
    }
}
