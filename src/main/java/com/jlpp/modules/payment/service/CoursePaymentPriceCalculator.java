package com.jlpp.modules.payment.service;

import com.jlpp.modules.course.entity.Course;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CoursePaymentPriceCalculator {

    public BigDecimal calculateFinalPrice(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course is required to calculate payment price.");
        }

        Double discountPrice = course.getDiscountPrice();
        Double selectedPrice = discountPrice != null && discountPrice > 0
                ? discountPrice
                : course.getPrice();
        return selectedPrice == null ? BigDecimal.ZERO : BigDecimal.valueOf(selectedPrice);
    }
}
