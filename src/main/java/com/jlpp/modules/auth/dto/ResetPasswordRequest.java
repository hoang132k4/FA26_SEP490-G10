package com.jlpp.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Mã thông báo đặt lại mật khẩu không hợp lệ.")
    private String token;

    @NotBlank(message = "Mật khẩu mới không được để trống.")
    @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d\\s]).{8,64}$",
            message = "Mật khẩu phải từ 8-64 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.")
    private String newPassword;

    @NotBlank(message = "Vui lòng xác nhận lại mật khẩu mới.")
    private String confirmPassword;

    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
