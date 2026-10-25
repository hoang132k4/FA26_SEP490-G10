package com.jlpp.modules.admin.dto;

import com.jlpp.common.constants.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank(message = "Họ tên không được để trống.")
    @Size(max = 150, message = "Họ tên không được vượt quá 150 ký tự.")
    private String fullName;

    @NotBlank(message = "Tên đăng nhập không được để trống.")
    @Size(max = 100, message = "Tên đăng nhập không được vượt quá 100 ký tự.")
    private String username;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không hợp lệ.")
    @Size(max = 254, message = "Email không được vượt quá 254 ký tự.")
    private String email;

    @NotNull(message = "Vui lòng chọn vai trò.")
    private RoleEnum role;

    @NotBlank(message = "Vui lòng chọn trạng thái tài khoản.")
    private String accountStatus;
}
