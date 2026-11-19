package com.jlpp.modules.course.service.impl;

import com.jlpp.modules.course.dto.CourseRegistrationRequest;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.service.CourseRegistrationService;
import com.jlpp.modules.payment.entity.Payment;
import com.jlpp.modules.payment.repository.PaymentRepository;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final PaymentRepository paymentRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.jlpp.modules.learningpath.repository.LearningPathRepository learningPathRepository;

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

        // Determine whether course is free or paid using existing pricing rules
        Double effectivePrice = (course.getDiscountPrice() != null && course.getDiscountPrice() > 0)
                ? course.getDiscountPrice()
                : course.getPrice();
        boolean isPaid = effectivePrice != null && effectivePrice > 0;

        Double paidAmount;
        String paymentMethod;
        if (isPaid) {
            Payment payment = paymentRepository
                    .findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(
                            userId, course.getId(), "PAID")
                    .orElseThrow(() -> new IllegalStateException(
                            "Khóa học này yêu cầu thanh toán. Vui lòng hoàn tất thanh toán trước khi đăng ký."));

            if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Giao dịch thanh toán không hợp lệ (số tiền thanh toán không đúng).");
            }
            if (payment.getCurrency() == null || !"VND".equalsIgnoreCase(payment.getCurrency().trim())) {
                throw new IllegalStateException("Giao dịch thanh toán không hợp lệ (đơn vị tiền tệ không hợp lệ).");
            }

            paidAmount = payment.getAmount().doubleValue();
            paymentMethod = (payment.getPaymentMethod() != null && !payment.getPaymentMethod().isBlank())
                    ? payment.getPaymentMethod()
                    : "SEPAY";
        } else {
            paidAmount = 0.0;
            paymentMethod = "FREE";
        }

        CourseRegistration registration = CourseRegistration.builder()
                .user(user)
                .course(course)
                .registeredAt(LocalDateTime.now())
                .status("ACTIVE")
                .progressPercent(0)
                .paidAmount(paidAmount)
                .paymentMethod(paymentMethod)
                .note(request.getNote())
                .build();

        CourseRegistration saved;
        try {
            saved = registrationRepository.save(registration);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate registration caught by unique constraint for userId={} courseId={}: {}",
                    userId, course.getId(), ex.getMessage());
            throw new IllegalStateException("Bạn đã đăng ký khóa học này trước đó. Vui lòng vào trang học tập để tiếp tục.", ex);
        }

        // Update enrollment count on course
        course.setEnrollmentCount((course.getEnrollmentCount() != null ? course.getEnrollmentCount() : 0) + 1);
        courseRepository.save(course);

        // Auto-generate or activate personalized Learning Path for this user & course
        try {
            if (learningPathRepository != null) {
                var existingLp = learningPathRepository.findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(user.getId(), course.getId());
                if (existingLp.isEmpty()) {
                    String courseLevel = (course.getLevel() != null && !course.getLevel().isBlank()) ? course.getLevel() : "N4";
                    String currentLvl = (user.getCurrentJlptLevel() != null && !user.getCurrentJlptLevel().isBlank()) ? user.getCurrentJlptLevel() : "N5";

                    com.jlpp.modules.learningpath.entity.LearningPath newLp = com.jlpp.modules.learningpath.entity.LearningPath.builder()
                            .userId(user.getId())
                            .courseId(course.getId())
                            .dailyHoursSetup(java.math.BigDecimal.valueOf(1.5))
                            .targetLevel(courseLevel)
                            .currentLevel(currentLvl)
                            .pathStatus("ACTIVE")
                            .scheduledDate(java.time.LocalDate.now())
                            .build();
                    learningPathRepository.save(newLp);
                    log.info("Auto-generated LearningPath for user ID: {} upon registering course ID: {}", user.getId(), course.getId());
                }
            }
            if (course.getLevel() != null && !course.getLevel().isBlank()) {
                user.setTargetJlptLevel(course.getLevel());
                userRepository.save(user);
            }
        } catch (Exception ex) {
            log.warn("Could not auto-generate LearningPath on course registration: {}", ex.getMessage());
        }

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
