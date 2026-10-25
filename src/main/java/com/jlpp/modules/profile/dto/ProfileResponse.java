package com.jlpp.modules.profile.dto;

import com.jlpp.common.constants.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String username;
    private RoleEnum role;
    private String roleDisplayName;
    private String status;
    private String currentJlptLevel;
    private String targetJlptLevel;
    private String dashboardUrl;
    private boolean isLearner;
    private String createdAtFormatted;
}
