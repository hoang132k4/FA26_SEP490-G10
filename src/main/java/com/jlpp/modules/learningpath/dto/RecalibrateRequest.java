package com.jlpp.modules.learningpath.dto;

import org.springframework.format.annotation.DateTimeFormat;
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
    @Max(value = 105, message = "Số giờ học tối đa mỗi tuần là 105 giờ để tránh quá tải")
    private Integer weeklyHours;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
}
