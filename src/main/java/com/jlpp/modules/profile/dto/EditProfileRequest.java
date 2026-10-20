package com.jlpp.modules.profile.dto;

import jakarta.validation.constraints.Email;
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
public class EditProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống.")
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2 đến 100 ký tự.")
    private String fullName;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Địa chỉ email không đúng định dạng.")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự.")
    private String email;

    @Pattern(
            regexp = "^(0[0-9]{9})?$",
            message = "Số điện thoại không hợp lệ (phải bắt đầu bằng số 0 và bao gồm đúng 10 chữ số)."
    )
    private String phone;
}
