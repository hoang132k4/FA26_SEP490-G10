package com.jlpp.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Mật khẩu hiện tại không được để trống.")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống.")
    @Size(min = 8, max = 64, message = "Mật khẩu mới phải có độ dài từ 8 đến 64 ký tự.")
    private String newPassword;

    @NotBlank(message = "Vui lòng xác nhận lại mật khẩu mới.")
    private String confirmPassword;

    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }

    public boolean isNewPasswordDifferent() {
        return newPassword != null && !newPassword.equals(currentPassword);
    }
}
