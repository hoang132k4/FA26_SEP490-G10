package com.jlpp.modules.user.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.jlpp.common.constants.RoleEnum;

public class AdminUserForm {

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 120, message = "Họ và tên không được vượt quá 120 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Vui lòng nhập email hợp lệ.")
    @Size(max = 254, message = "Email không được vượt quá 254 ký tự.")
    private String email;

    @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự.")
    @Pattern(regexp = "^$|^[+0-9() .-]{7,30}$", message = "Số điện thoại không hợp lệ.")
    private String phone;

    @NotNull(message = "Vui lòng chọn vai trò.")
    private RoleEnum role;

    @NotNull(message = "Vui lòng chọn trạng thái.")
    private AdminUserStatus status;

    public static AdminUserForm from(AdminUser user) {
        AdminUserForm form = new AdminUserForm();
        form.setFullName(user.fullName());
        form.setEmail(user.email());
        form.setPhone(user.phone());
        form.setRole(user.role());
        form.setStatus(user.status());
        return form;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public RoleEnum getRole() {
        return role;
    }

    public void setRole(RoleEnum role) {
        this.role = role;
    }

    public AdminUserStatus getStatus() {
        return status;
    }

    public void setStatus(AdminUserStatus status) {
        this.status = status;
    }
}
