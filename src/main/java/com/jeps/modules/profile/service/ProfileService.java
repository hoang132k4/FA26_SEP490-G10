package com.jeps.modules.profile.service;

import com.jeps.modules.profile.dto.EditProfileRequest;
import com.jeps.modules.profile.dto.ProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {

    ProfileResponse getProfile(String identifier);

    EditProfileRequest getEditProfileForm(String identifier);

    ProfileResponse updateProfile(String identifier, EditProfileRequest request, MultipartFile avatarFile);

    default ProfileResponse updateProfile(String identifier, EditProfileRequest request) {
        return updateProfile(identifier, request, null);
    }

    ProfileResponse updateAvatar(String identifier, MultipartFile avatarFile);

    ProfileResponse verifyEmailChange(String token);

    ProfileResponse cancelPendingEmailChange(String identifier);
}
