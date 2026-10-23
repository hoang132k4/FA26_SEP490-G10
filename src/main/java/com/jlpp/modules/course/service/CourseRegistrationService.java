package com.jlpp.modules.course.service;

import com.jlpp.modules.course.dto.CourseRegistrationRequest;
import com.jlpp.modules.course.entity.CourseRegistration;

import java.util.List;

public interface CourseRegistrationService {
    CourseRegistration registerCourse(Long userId, CourseRegistrationRequest request);
    boolean isUserEnrolled(Long userId, Long courseId);
    List<CourseRegistration> getUserRegistrations(Long userId);
    CourseRegistration getRegistration(Long userId, Long courseId);
}
