package com.jlpp.modules.course.service;

import com.jlpp.modules.course.entity.Course;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CoursePriceCalculator {

    public BigDecimal calculateFinalPrice(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course is required to calculate its price.");
        }

        return calculateFinalPrice(course.getPrice(), course.getDiscountPrice());
    }

    public BigDecimal calculateFinalPrice(Double basePrice, Double discountPrice) {
        if (basePrice == null || !Double.isFinite(basePrice) || basePrice < 0) {
            throw new IllegalArgumentException("Course base price is invalid.");
        }
        if (discountPrice != null
                && (!Double.isFinite(discountPrice) || discountPrice < 0 || discountPrice > basePrice)) {
            throw new IllegalArgumentException("Course discount price is invalid.");
        }

        if (basePrice == 0) {
            if (discountPrice != null && discountPrice > 0) {
                throw new IllegalArgumentException("A free course cannot have a positive discount price.");
            }
            return BigDecimal.ZERO;
        }

        if (discountPrice != null && discountPrice > 0) {
            return BigDecimal.valueOf(discountPrice);
        }
        return BigDecimal.valueOf(basePrice);
    }

    public boolean isFree(Course course) {
        return calculateFinalPrice(course).signum() == 0;
    }

    public boolean isFree(Double basePrice, Double discountPrice) {
        return calculateFinalPrice(basePrice, discountPrice).signum() == 0;
    }
}
