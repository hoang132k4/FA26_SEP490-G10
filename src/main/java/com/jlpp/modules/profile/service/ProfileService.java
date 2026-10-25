package com.jlpp.modules.profile.service;

import com.jlpp.modules.profile.dto.EditProfileRequest;
import com.jlpp.modules.profile.dto.ProfileResponse;

public interface ProfileService {

    ProfileResponse getProfile(String identifier);

    EditProfileRequest getEditProfileForm(String identifier);

    ProfileResponse updateProfile(String identifier, EditProfileRequest request);
}
