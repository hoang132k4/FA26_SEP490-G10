package com.jlpp.modules.course.service.impl;

import com.jlpp.modules.course.dto.CourseRegistrationRequest;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.service.CoursePriceCalculator;
import com.jlpp.modules.course.service.CourseRegistrationService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseRegistrationServiceImpl implements CourseRegistrationService {

    private final CourseRegistrationRepository registrationRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CoursePriceCalculator priceCalculator;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.jlpp.modules.learningpath.repository.LearningPathRepository learningPathRepository;

    @Override
    @Transactional
    public CourseRegistration registerCourse(Long userId, CourseRegistrationRequest request) {
        if (request == null || request.getCourseId() == null) {
            throw new IllegalArgumentException("Thiếu mã khóa học.");
        }

        User user = lockUser(userId);
        Course course = findCourse(request.getCourseId());
        BigDecimal amount = priceCalculator.calculateFinalPrice(course);
        if (amount.signum() != 0) {
            throw new IllegalStateException("Khóa học có phí cần được thanh toán trước khi kích hoạt.");
        }
        if (registrationRepository.existsByUserIdAndCourseId(userId, course.getId())) {
            throw new IllegalStateException("Bạn đã đăng ký khóa học này trước đó. Vui lòng vào trang học tập để tiếp tục.");
        }

        return createRegistration(user, course, CourseRegistration.STATUS_ACTIVE, "FREE", 0.0, request.getNote());
    }

    @Override
    @Transactional
    public CourseRegistration createPendingRegistration(Long userId, CourseRegistrationRequest request) {
        if (request == null || request.getCourseId() == null) {
            throw new IllegalArgumentException("Thiếu mã khóa học.");
        }

        User user = lockUser(userId);
        Course course = findCourse(request.getCourseId());
        BigDecimal amount = priceCalculator.calculateFinalPrice(course);
        if (amount.signum() <= 0) {
            throw new IllegalStateException("Khóa học miễn phí không cần thanh toán.");
        }

        CourseRegistration existing = registrationRepository
                .findForUpdateByUserIdAndCourseId(userId, course.getId())
                .orElse(null);
        if (existing != null) {
            if (CourseRegistration.STATUS_PENDING_PAYMENT.equalsIgnoreCase(existing.getStatus())) {
                return existing;
            }
            throw new IllegalStateException(
                    "Bạn đã đăng ký khóa học này trước đó. Vui lòng vào trang học tập để tiếp tục.");
        }

        return createRegistration(user, course, CourseRegistration.STATUS_PENDING_PAYMENT,
                "SEPAY", 0.0, request.getNote());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseRegistration getRegistrationById(Long userId, Long registrationId) {
        if (userId == null || registrationId == null) {
            return null;
        }
        return registrationRepository.findByIdAndUserIdWithCourse(registrationId, userId).orElse(null);
    }

    @Override
    @Transactional
    public CourseRegistration getPendingRegistrationForUpdate(Long userId, Long registrationId) {
        CourseRegistration registration = getRegistrationById(userId, registrationId);
        if (registration == null) {
            throw new SecurityException("Không tìm thấy đăng ký khóa học.");
        }
        registration = registrationRepository.findForUpdateByIdAndUserIdAndCourseId(
                        registrationId, userId, registration.getCourse().getId())
                .orElseThrow(() -> new SecurityException("Không tìm thấy đăng ký khóa học."));
        if (!CourseRegistration.STATUS_PENDING_PAYMENT.equalsIgnoreCase(registration.getStatus())) {
            throw new IllegalStateException("Đăng ký khóa học không còn chờ thanh toán.");
        }
        return registration;
    }

    @Override
    @Transactional
    public CourseRegistration completePaidRegistration(
            Long userId, Long courseId, Long registrationId, BigDecimal paidAmount, String paymentMethod) {
        if (paidAmount == null || paidAmount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ.");
        }
        CourseRegistration registration = registrationRepository
                .findForUpdateByIdAndUserIdAndCourseId(registrationId, userId, courseId)
                .orElseThrow(() -> new SecurityException("Không tìm thấy đăng ký thanh toán cần kích hoạt."));

        String status = registration.getStatus() == null ? "" : registration.getStatus().trim();
        if (CourseRegistration.STATUS_ACTIVE.equalsIgnoreCase(status)
                || CourseRegistration.STATUS_COMPLETED.equalsIgnoreCase(status)) {
            return registration;
        }
        if (!CourseRegistration.STATUS_PENDING_PAYMENT.equalsIgnoreCase(status)) {
            throw new IllegalStateException("Đăng ký khóa học không thể được kích hoạt từ trạng thái hiện tại.");
        }

        registration.setStatus(CourseRegistration.STATUS_ACTIVE);
        registration.setPaidAmount(paidAmount.doubleValue());
        registration.setPaymentMethod(paymentMethod);
        CourseRegistration saved = registrationRepository.save(registration);
        incrementEnrollmentCount(registration.getCourse());
        onRegistrationActivated(registration.getUser(), registration.getCourse());
        return saved;
    }

    @Override
    @Transactional
    public Optional<CourseRegistration> ensureFreeRegistrationForAccess(Long userId, Long courseId) {
        if (userId == null || courseId == null) {
            return Optional.empty();
        }

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thông tin tài khoản người dùng ID: " + userId));
        Course course = findCourse(courseId);
        CourseRegistration existing = registrationRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
        if (existing != null) {
            return isAccessEligible(existing.getStatus()) ? Optional.of(existing) : Optional.empty();
        }
        if (!priceCalculator.isFree(course)) {
            return Optional.empty();
        }

        return Optional.of(createRegistration(user, course, CourseRegistration.STATUS_ACTIVE, "FREE", 0.0, null));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserEnrolled(Long userId, Long courseId) {
        if (userId == null || courseId == null) return false;
        return registrationRepository.existsEligibleByUserIdAndCourseId(userId, courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseRegistration> getUserRegistrations(Long userId) {
        return registrationRepository.findByUserIdWithCourse(userId).stream()
                .filter(registration -> isAccessEligible(registration.getStatus()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CourseRegistration getRegistration(Long userId, Long courseId) {
        return registrationRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
    }

    private User lockUser(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy thông tin tài khoản người dùng ID: " + userId));
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khóa học với mã ID: " + courseId));
    }

    private CourseRegistration createRegistration(
            User user, Course course, String status, String paymentMethod, double paidAmount, String note) {
        CourseRegistration registration = CourseRegistration.builder()
                .user(user)
                .course(course)
                .registeredAt(LocalDateTime.now())
                .status(status)
                .progressPercent(0)
                .paidAmount(paidAmount)
                .paymentMethod(paymentMethod)
                .note(note)
                .build();
        CourseRegistration saved = registrationRepository.save(registration);
        if (CourseRegistration.STATUS_ACTIVE.equals(status)) {
            incrementEnrollmentCount(course);
            onRegistrationActivated(user, course);
        }
        return saved;
    }

    private void incrementEnrollmentCount(Course course) {
        course.setEnrollmentCount((course.getEnrollmentCount() != null ? course.getEnrollmentCount() : 0) + 1);
        courseRepository.save(course);
    }

    private void onRegistrationActivated(User user, Course course) {
        if (learningPathRepository != null) {
            var existingLp = learningPathRepository
                    .findFirstByUserIdAndCourseIdOrderByScheduledDateDescIdDesc(user.getId(), course.getId());
            if (existingLp.isEmpty()) {
                String courseLevel = course.getLevel() != null && !course.getLevel().isBlank()
                        ? course.getLevel() : "N4";
                String currentLevel = user.getCurrentJlptLevel() != null && !user.getCurrentJlptLevel().isBlank()
                        ? user.getCurrentJlptLevel() : "N5";
                com.jlpp.modules.learningpath.entity.LearningPath newLearningPath =
                        com.jlpp.modules.learningpath.entity.LearningPath.builder()
                                .userId(user.getId())
                                .courseId(course.getId())
                                .dailyHoursSetup(BigDecimal.valueOf(1.5))
                                .targetLevel(courseLevel)
                                .currentLevel(currentLevel)
                                .pathStatus("ACTIVE")
                                .scheduledDate(java.time.LocalDate.now())
                                .build();
                learningPathRepository.save(newLearningPath);
                log.info("Auto-generated LearningPath for user ID: {} upon registering course ID: {}",
                        user.getId(), course.getId());
            }
        }
        if (course.getLevel() != null && !course.getLevel().isBlank()) {
            user.setTargetJlptLevel(course.getLevel());
            userRepository.save(user);
        }
    }

    private boolean isAccessEligible(String status) {
        return CourseRegistration.STATUS_ACTIVE.equalsIgnoreCase(status)
                || CourseRegistration.STATUS_COMPLETED.equalsIgnoreCase(status);
    }
}
