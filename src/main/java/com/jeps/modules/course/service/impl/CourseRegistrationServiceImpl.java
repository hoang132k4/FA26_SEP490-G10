package com.jeps.modules.course.service.impl;

import com.jeps.modules.course.dto.CourseRegistrationRequest;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.entity.CourseRegistration;
import com.jeps.modules.course.repository.CourseRegistrationRepository;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.course.service.CourseRegistrationService;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.jeps.modules.learningpath.repository.LearningPathRepository learningPathRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.jeps.modules.order.repository.OrderRepository orderRepository;

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

        // Auto-create corresponding Order & OrderItem so it appears in Order History
        try {
            if (orderRepository != null) {
                java.math.BigDecimal price = java.math.BigDecimal.valueOf(amount != null ? amount : 0.0);
                String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + String.format("%04d", (int)(Math.random() * 10000));
                com.jeps.modules.order.entity.Order order = com.jeps.modules.order.entity.Order.builder()
                        .orderNumber(orderNumber)
                        .user(user)
                        .totalAmount(price)
                        .orderStatus(com.jeps.modules.order.entity.OrderStatus.COMPLETED)
                        .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "FREE")
                        .paymentStatus("PAID")
                        .transactionReference("TXN-" + System.currentTimeMillis())
                        .build();

                com.jeps.modules.order.entity.OrderItem orderItem = com.jeps.modules.order.entity.OrderItem.builder()
                        .order(order)
                        .courseId(course.getId())
                        .itemName(course.getTitle())
                        .price(price)
                        .quantity(1)
                        .subtotal(price)
                        .build();

                order.addItem(orderItem);
                orderRepository.save(order);
                log.info("Auto-created Order {} for user ID: {} upon course registration ID: {}", orderNumber, user.getId(), course.getId());
            }
        } catch (Exception ex) {
            log.warn("Could not create Order record for course registration: {}", ex.getMessage());
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

                    com.jeps.modules.learningpath.entity.LearningPath newLp = com.jeps.modules.learningpath.entity.LearningPath.builder()
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
