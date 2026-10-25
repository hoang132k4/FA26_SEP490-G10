package com.jlpp.modules.profile;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.profile.dto.EditProfileRequest;
import com.jlpp.modules.profile.dto.ProfileResponse;
import com.jlpp.modules.profile.service.ProfileService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProfileService profileService;

    private static final String LEARNER_EMAIL = "learner_prof@jeps.edu.vn";
    private static final String MANAGER_EMAIL = "manager_prof@jeps.edu.vn";
    private static final String ADMIN_EMAIL = "admin_prof@jeps.edu.vn";
    private static final String NEW_EMAIL = "new_learner_email@jeps.edu.vn";

    private User learnerUser;
    private User managerUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        userRepository.findByEmailIgnoreCase(LEARNER_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase(MANAGER_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase(NEW_EMAIL).ifPresent(userRepository::delete);

        learnerUser = userRepository.save(User.builder()
                .email(LEARNER_EMAIL)
                .username("learner_prof_user")
                .fullName("Nguyễn Văn Học Viên")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .phone("0987654321")
                .currentJlptLevel("N4")
                .targetJlptLevel("N2")
                .build());

        managerUser = userRepository.save(User.builder()
                .email(MANAGER_EMAIL)
                .username("manager_prof_user")
                .fullName("Trần Thị Quản Lý")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.MANAGER)
                .status("ACTIVE")
                .phone("0912345678")
                .build());

        adminUser = userRepository.save(User.builder()
                .email(ADMIN_EMAIL)
                .username("admin_prof_user")
                .fullName("Lê Văn Quản Trị")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.ADMIN)
                .status("ACTIVE")
                .phone("0901234567")
                .build());
    }

    // ==========================================
    // UC-09: VIEW PROFILE TESTS
    // ==========================================

    @Test
    @DisplayName("VP-01: Anonymous user viewing /profile redirects to /auth/login")
    void viewProfile_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("VP-02: Authenticated Learner can view profile with JLPT levels")
    @WithMockUser(username = LEARNER_EMAIL, roles = {"LEARNER"})
    void viewProfile_learner_success() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/profile/view"))
                .andExpect(model().attributeExists("profile"))
                .andExpect(content().string(containsString("Nguyễn Văn Học Viên")))
                .andExpect(content().string(containsString(LEARNER_EMAIL)))
                .andExpect(content().string(containsString("0987654321")))
                .andExpect(content().string(containsString("N4")))
                .andExpect(content().string(containsString("N2")))
                .andExpect(content().string(containsString("Học viên (Learner)")));
    }

    @Test
    @DisplayName("VP-03: Authenticated Manager can view profile with Manager layout")
    @WithMockUser(username = MANAGER_EMAIL, roles = {"MANAGER"})
    void viewProfile_manager_success() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/profile/view"))
                .andExpect(model().attributeExists("profile"))
                .andExpect(content().string(containsString("Trần Thị Quản Lý")))
                .andExpect(content().string(containsString(MANAGER_EMAIL)))
                .andExpect(content().string(containsString("Quản lý nội dung (Manager)")));
    }

    @Test
    @DisplayName("VP-04: Authenticated Admin can view profile with Admin layout")
    @WithMockUser(username = ADMIN_EMAIL, roles = {"ADMIN"})
    void viewProfile_admin_success() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/profile/view"))
                .andExpect(model().attributeExists("profile"))
                .andExpect(content().string(containsString("Lê Văn Quản Trị")))
                .andExpect(content().string(containsString(ADMIN_EMAIL)))
                .andExpect(content().string(containsString("Quản trị viên (Admin)")));
    }

    // ==========================================
    // UC-10: EDIT PROFILE TESTS (GET FORM)
    // ==========================================

    @Test
    @DisplayName("EP-01: Anonymous user viewing /profile/edit redirects to /auth/login")
    void editProfilePage_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    @DisplayName("EP-02: Authenticated user can view /profile/edit form with populated fields")
    @WithMockUser(username = LEARNER_EMAIL, roles = {"LEARNER"})
    void editProfilePage_authenticated_success() throws Exception {
        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(get("/profile/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/profile/edit"))
                .andExpect(model().attributeExists("profile"))
                .andExpect(model().attributeExists("editProfileRequest"))
                .andExpect(content().string(containsString(LEARNER_EMAIL)))
                .andReturn();

        EditProfileRequest editForm = (EditProfileRequest) result.getModelAndView().getModel().get("editProfileRequest");
        assertThat(editForm.getFullName()).isEqualTo("Nguyễn Văn Học Viên");
        assertThat(editForm.getEmail()).isEqualTo(LEARNER_EMAIL);
        assertThat(editForm.getPhone()).isEqualTo("0987654321");
    }

    // ==========================================
    // UC-10: EDIT PROFILE TESTS (POST FORM)
    // ==========================================

    @Test
    @DisplayName("EP-03: Valid profile update saves to database and redirects to /profile with flash message")
    @WithMockUser(username = LEARNER_EMAIL, roles = {"LEARNER"})
    void editProfile_validSubmission_success() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .with(csrf())
                        .param("fullName", "Nguyễn Văn Đã Đổi Tên")
                        .param("email", LEARNER_EMAIL)
                        .param("phone", "0911223344"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("successMessage", "Cập nhật hồ sơ cá nhân thành công!"));

        User updatedUser = userRepository.findById(learnerUser.getId()).orElseThrow();
        assertThat(updatedUser.getFullName()).isEqualTo("Nguyễn Văn Đã Đổi Tên");
        assertThat(updatedUser.getPhone()).isEqualTo("0911223344");
    }

    @Test
    @DisplayName("EP-04: Valid email update changes email in database")
    @WithMockUser(username = LEARNER_EMAIL, roles = {"LEARNER"})
    void editProfile_validEmailUpdate_success() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .with(csrf())
                        .param("fullName", "Nguyễn Văn Học Viên")
                        .param("email", NEW_EMAIL)
                        .param("phone", "0987654321"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("successMessage"));

        User updatedUser = userRepository.findById(learnerUser.getId()).orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo(NEW_EMAIL);
    }

    @Test
    @DisplayName("EP-05: Duplicate email returns error and rejects update")
    @WithMockUser(username = LEARNER_EMAIL, roles = {"LEARNER"})
    void editProfile_duplicateEmail_failsWithErrorMessage() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .with(csrf())
                        .param("fullName", "Nguyễn Văn Học Viên")
                        .param("email", MANAGER_EMAIL)
                        .param("phone", "0987654321"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/profile/edit"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", containsString("Địa chỉ email đã được sử dụng bởi một tài khoản khác")));

        User unchangedUser = userRepository.findById(learnerUser.getId()).orElseThrow();
        assertThat(unchangedUser.getEmail()).isEqualTo(LEARNER_EMAIL);
    }

    @Test
    @DisplayName("EP-06: Blank full name triggers validation error")
    @WithMockUser(username = LEARNER_EMAIL, roles = {"LEARNER"})
    void editProfile_blankFullName_validationError() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .with(csrf())
                        .param("fullName", "   ")
                        .param("email", LEARNER_EMAIL)
                        .param("phone", "0987654321"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/profile/edit"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("editProfileRequest", "fullName"));
    }

    @Test
    @DisplayName("EP-07: Invalid email format triggers validation error")
    @WithMockUser(username = LEARNER_EMAIL, roles = {"LEARNER"})
    void editProfile_invalidEmailFormat_validationError() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .with(csrf())
                        .param("fullName", "Nguyễn Văn Học Viên")
                        .param("email", "not-a-valid-email")
                        .param("phone", "0987654321"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/profile/edit"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("editProfileRequest", "email"));
    }

    @Test
    @DisplayName("EP-08: Invalid phone number format triggers validation error")
    @WithMockUser(username = LEARNER_EMAIL, roles = {"LEARNER"})
    void editProfile_invalidPhoneFormat_validationError() throws Exception {
        mockMvc.perform(post("/profile/edit")
                        .with(csrf())
                        .param("fullName", "Nguyễn Văn Học Viên")
                        .param("email", LEARNER_EMAIL)
                        .param("phone", "123456"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/profile/edit"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("editProfileRequest", "phone"));
    }

    @Test
    @DisplayName("EP-09: Tampering prevention - role, password, and JLPT levels cannot be changed via edit profile")
    @WithMockUser(username = LEARNER_EMAIL, roles = {"LEARNER"})
    void editProfile_tamperSensitiveFields_ignored() throws Exception {
        String originalPassword = learnerUser.getPassword();

        mockMvc.perform(post("/profile/edit")
                        .with(csrf())
                        .param("fullName", "Nguyễn Văn Học Viên Đã Đổi")
                        .param("email", LEARNER_EMAIL)
                        .param("phone", "0987654321")
                        .param("role", "ADMIN")
                        .param("password", "HackedPass123@")
                        .param("currentJlptLevel", "N1")
                        .param("targetJlptLevel", "N1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        User reloadedUser = userRepository.findById(learnerUser.getId()).orElseThrow();
        assertThat(reloadedUser.getRole()).isEqualTo(RoleEnum.LEARNER);
        assertThat(reloadedUser.getPassword()).isEqualTo(originalPassword);
        assertThat(reloadedUser.getCurrentJlptLevel()).isEqualTo("N4");
        assertThat(reloadedUser.getTargetJlptLevel()).isEqualTo("N2");
    }

    // ==========================================
    // SERVICE LAYER DIRECT UNIT / INTEGRATION TESTS
    // ==========================================

    @Test
    @DisplayName("SRV-01: ProfileService.getProfile maps all fields correctly")
    void profileService_getProfile_success() {
        ProfileResponse response = profileService.getProfile(LEARNER_EMAIL);

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("Nguyễn Văn Học Viên");
        assertThat(response.getEmail()).isEqualTo(LEARNER_EMAIL);
        assertThat(response.getPhone()).isEqualTo("0987654321");
        assertThat(response.getRole()).isEqualTo(RoleEnum.LEARNER);
        assertThat(response.getCurrentJlptLevel()).isEqualTo("N4");
        assertThat(response.getTargetJlptLevel()).isEqualTo("N2");
        assertThat(response.getDashboardUrl()).isEqualTo("/learning/dashboard");
        assertThat(response.isLearner()).isTrue();
    }

    @Test
    @DisplayName("SRV-02: ProfileService.getProfile throws when identifier does not exist")
    void profileService_getProfile_userNotFound_throwsException() {
        assertThatThrownBy(() -> profileService.getProfile("nonexistent@jeps.edu.vn"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy thông tin tài khoản");
    }

    @Test
    @DisplayName("SRV-03: ProfileService.updateProfile throws when duplicate email attempted")
    void profileService_updateProfile_duplicateEmail_throwsException() {
        EditProfileRequest request = EditProfileRequest.builder()
                .fullName("New Name")
                .email(ADMIN_EMAIL)
                .phone("0987654321")
                .build();

        assertThatThrownBy(() -> profileService.updateProfile(LEARNER_EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Địa chỉ email đã được sử dụng");
    }

    @Test
    @DisplayName("SRV-04: ProfileService.updateAvatar uploads image and saves avatarUrl successfully")
    void profileService_updateAvatar_success() {
        MockMultipartFile avatarFile = new MockMultipartFile(
                "avatarFile",
                "test-avatar.png",
                "image/png",
                "fake-image-bytes".getBytes()
        );

        ProfileResponse response = profileService.updateAvatar(LEARNER_EMAIL, avatarFile);

        assertThat(response).isNotNull();
        assertThat(response.getAvatarUrl()).isNotNull();
        assertThat(response.getAvatarUrl()).startsWith("/uploads/avatars/avatar_");

        User updatedUser = userRepository.findByEmailIgnoreCase(LEARNER_EMAIL).orElseThrow();
        assertThat(updatedUser.getAvatarUrl()).isEqualTo(response.getAvatarUrl());
    }

    @Test
    @DisplayName("SRV-05: ProfileService.updateAvatar rejects non-image files")
    void profileService_updateAvatar_invalidType_throwsException() {
        MockMultipartFile textFile = new MockMultipartFile(
                "avatarFile",
                "malicious.txt",
                "text/plain",
                "not an image".getBytes()
        );

        assertThatThrownBy(() -> profileService.updateAvatar(LEARNER_EMAIL, textFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chỉ chấp nhận tệp hình ảnh");
    }

    @Test
    @DisplayName("WEB-06: POST /profile/avatar handles file upload and redirects to /profile with flash message")
    void profileController_updateAvatar_success() throws Exception {
        MockMultipartFile validImage = new MockMultipartFile(
                "avatarFile",
                "my-photo.jpg",
                "image/jpeg",
                "sample-image-content".getBytes()
        );

        mockMvc.perform(multipart("/profile/avatar")
                        .file(validImage)
                        .with(csrf())
                        .with(user(LEARNER_EMAIL).roles("LEARNER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("successMessage", "Cập nhật ảnh đại diện thành công!"));

        User user = userRepository.findByEmailIgnoreCase(LEARNER_EMAIL).orElseThrow();
        assertThat(user.getAvatarUrl()).isNotNull();
        assertThat(user.getAvatarUrl()).startsWith("/uploads/avatars/avatar_");
    }
}
