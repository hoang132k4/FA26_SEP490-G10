package com.jlpp.modules.course.service.impl;

import com.jlpp.modules.course.dto.CourseRegistrationRequest;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.service.CourseRegistrationService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseRegistrationServiceImpl implements CourseRegistrationService {

    private final CourseRegistrationRepository registrationRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CourseRegistration registerCourse(Long userId, CourseRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thông tin tài khoản người dùng ID: " + userId));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khóa học với mã ID: " + request.getCourseId()));

        if (registrationRepository.existsByUserIdAndCourseId(userId, course.getId())) {
            throw new IllegalStateException("Bạn đã đăng ký khóa học này trước đó. Vui lòng vào trang học tập để tiếp tục.");
        }

        // Determine price / payment
        Double amount = (course.getDiscountPrice() != null && course.getDiscountPrice() > 0)
                ? course.getDiscountPrice()
                : course.getPrice();

        CourseRegistration registration = CourseRegistration.builder()
                .user(user)
                .course(course)
                .registeredAt(LocalDateTime.now())
                .status("ACTIVE")
                .progressPercent(0)
                .paidAmount(amount != null ? amount : 0.0)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "FREE")
                .note(request.getNote())
                .build();

        CourseRegistration saved = registrationRepository.save(registration);

        // Update enrollment count on course
        course.setEnrollmentCount((course.getEnrollmentCount() != null ? course.getEnrollmentCount() : 0) + 1);
        courseRepository.save(course);

        log.info("Successfully enrolled user ID: {} to course ID: {} ({})", user.getId(), course.getId(), course.getTitle());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserEnrolled(Long userId, Long courseId) {
        if (userId == null || courseId == null) return false;
        return registrationRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseRegistration> getUserRegistrations(Long userId) {
        return registrationRepository.findByUserIdWithCourse(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseRegistration getRegistration(Long userId, Long courseId) {
        return registrationRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
    }
}
