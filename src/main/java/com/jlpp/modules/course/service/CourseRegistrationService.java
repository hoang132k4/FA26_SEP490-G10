package com.jlpp.modules.course.service;

import com.jlpp.modules.course.dto.CourseRegistrationRequest;
import com.jlpp.modules.course.entity.CourseRegistration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CourseRegistrationService {
    CourseRegistration registerCourse(Long userId, CourseRegistrationRequest request);
    CourseRegistration createPendingRegistration(Long userId, CourseRegistrationRequest request);
    CourseRegistration getRegistrationById(Long userId, Long registrationId);
    CourseRegistration getPendingRegistrationForUpdate(Long userId, Long registrationId);
    CourseRegistration completePaidRegistration(
            Long userId, Long courseId, Long registrationId, BigDecimal paidAmount, String paymentMethod);
    Optional<CourseRegistration> ensureFreeRegistrationForAccess(Long userId, Long courseId);
    boolean isUserEnrolled(Long userId, Long courseId);
    List<CourseRegistration> getUserRegistrations(Long userId);
    CourseRegistration getRegistration(Long userId, Long courseId);
}
