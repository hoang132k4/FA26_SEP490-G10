package com.jlpp.modules.course;

import com.jlpp.modules.course.service.CoursePriceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoursePriceCalculatorTest {

    private final CoursePriceCalculator calculator = new CoursePriceCalculator();

    @Test
    void calculatesTheAuthoritativeFinalPriceForAllValidCombinations() {
        assertPrice("100000", calculator.calculateFinalPrice(100000.0, null));
        assertPrice("100000", calculator.calculateFinalPrice(100000.0, 0.0));
        assertPrice("80000", calculator.calculateFinalPrice(100000.0, 80000.0));
        assertPrice("100000", calculator.calculateFinalPrice(100000.0, 100000.0));
        assertPrice("0", calculator.calculateFinalPrice(0.0, null));
        assertPrice("0", calculator.calculateFinalPrice(0.0, 0.0));
    }

    @Test
    void rejectsNegativeNonFiniteAndInconsistentPrices() {
        assertInvalid(-1.0, null);
        assertInvalid(Double.NaN, null);
        assertInvalid(Double.POSITIVE_INFINITY, null);
        assertInvalid(100.0, -1.0);
        assertInvalid(100.0, Double.NaN);
        assertInvalid(100.0, Double.POSITIVE_INFINITY);
        assertInvalid(100.0, 101.0);
        assertInvalid(0.0, 1.0);
        assertInvalid(null, null);
    }

    private void assertPrice(String expected, BigDecimal actual) {
        assertThat(actual).isEqualByComparingTo(expected);
    }

    private void assertInvalid(Double basePrice, Double discountPrice) {
        assertThatThrownBy(() -> calculator.calculateFinalPrice(basePrice, discountPrice))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
