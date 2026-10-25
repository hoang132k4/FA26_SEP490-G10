package com.jlpp.modules.course.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRegistrationRequest {

    @NotNull(message = "ID khóa học không hợp lệ.")
    private Long courseId;

    @NotBlank(message = "Họ và tên không được để trống.")
    private String fullName;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không đúng định dạng.")
    private String email;

    private String phone;

    @Builder.Default
    private String paymentMethod = "FREE"; // FREE, VNPAY, MOMO, BANK_TRANSFER

    private String note;

    @AssertTrue(message = "Bạn cần đồng ý với điều khoản sử dụng khóa học của JEPS.")
    @Builder.Default
    private boolean termsAccepted = true;
}
