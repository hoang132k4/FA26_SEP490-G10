package com.jlpp.modules.learningpath.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecalibrateRequest {

    @NotNull(message = "Số giờ học mỗi tuần không được để trống")
    @Min(value = 1, message = "Số giờ học tối thiểu mỗi tuần là 1 giờ")
    @Max(value = 40, message = "Số giờ học tối đa mỗi tuần là 40 giờ để tránh quá tải")
    private Integer weeklyHours;

    private LocalDate startDate;
}
