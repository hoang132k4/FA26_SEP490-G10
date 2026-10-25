package com.jlpp.modules.profile.service;

import com.jlpp.modules.profile.dto.EditProfileRequest;
import com.jlpp.modules.profile.dto.ProfileResponse;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {

    ProfileResponse getProfile(String identifier);

    EditProfileRequest getEditProfileForm(String identifier);

    ProfileResponse updateProfile(String identifier, EditProfileRequest request);

    ProfileResponse updateAvatar(String identifier, MultipartFile avatarFile);
}
