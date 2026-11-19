package com.jlpp.modules.course;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.course.dto.CourseRegistrationRequest;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.service.impl.CourseRegistrationServiceImpl;
import com.jlpp.modules.payment.entity.Payment;
import com.jlpp.modules.payment.repository.PaymentRepository;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseRegistrationServiceTest {

    @Mock
    private CourseRegistrationRepository registrationRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private CourseRegistrationServiceImpl registrationService;

    private User testUser;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        registrationService = new CourseRegistrationServiceImpl(registrationRepository, courseRepository, userRepository, paymentRepository);

        testUser = User.builder()
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .role(RoleEnum.LEARNER)
                .build();
        testUser.setId(10L);

        testCourse = Course.builder()
                .title("JLPT N5 Toan dien")
                .level("N5")
                .price(299000.0)
                .enrollmentCount(10)
                .build();
        testCourse.setId(100L);
    }

    @Test
    @DisplayName("1. Free course registration succeeds without Payment")
    void testFreeCourseRegistrationSucceedsWithoutPayment() {
        testCourse.setPrice(0.0);
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("FREE")
                .termsAccepted(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        when(registrationRepository.save(any(CourseRegistration.class))).thenAnswer(i -> i.getArgument(0));

        CourseRegistration result = registrationService.registerCourse(10L, req);

        assertThat(result).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(10L);
        assertThat(result.getCourse().getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(testCourse.getEnrollmentCount()).isEqualTo(11);

        verify(paymentRepository, never())
                .findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(any(), any(), any());
        verify(registrationRepository).save(any(CourseRegistration.class));
        verify(courseRepository).save(testCourse);
    }

    @Test
    @DisplayName("2. Free course stores paidAmount = 0 and paymentMethod = FREE regardless of frontend input")
    void testFreeCourseStoresZeroPaidAmountAndFreeMethodRegardlessOfFrontend() {
        testCourse.setPrice(0.0);
        // Frontend attempts to submit paymentMethod="MOMO"
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("MOMO")
                .termsAccepted(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        when(registrationRepository.save(any(CourseRegistration.class))).thenAnswer(i -> i.getArgument(0));

        CourseRegistration result = registrationService.registerCourse(10L, req);

        assertThat(result).isNotNull();
        assertThat(result.getPaidAmount()).isEqualTo(0.0);
        assertThat(result.getPaymentMethod()).isEqualTo("FREE");
    }

    @Test
    @DisplayName("3. Paid course registration without Payment is rejected")
    void testPaidCourseWithoutPaymentRejected() {
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("SEPAY")
                .termsAccepted(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yêu cầu thanh toán");

        verify(registrationRepository, never()).save(any());
        verify(courseRepository, never()).save(testCourse);
    }

    @Test
    @DisplayName("4. Paid course with PENDING Payment is rejected")
    void testPaidCourseWithPendingPaymentRejected() {
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("SEPAY")
                .termsAccepted(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        // Only "PAID" status is queried; PENDING returns empty
        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yêu cầu thanh toán");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Paid course with FAILED Payment is rejected")
    void testPaidCourseWithFailedPaymentRejected() {
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("SEPAY")
                .termsAccepted(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yêu cầu thanh toán");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Paid course with EXPIRED Payment is rejected")
    void testPaidCourseWithExpiredPaymentRejected() {
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("SEPAY")
                .termsAccepted(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yêu cầu thanh toán");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Paid course with PAID Payment belonging to another user is rejected")
    void testPaidCourseWithPaidPaymentOfAnotherUserRejected() {
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("SEPAY")
                .termsAccepted(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        // User 10 has no paid payment (payment belonged to another user)
        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yêu cầu thanh toán");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("8. Paid course with PAID Payment belonging to another course is rejected")
    void testPaidCourseWithPaidPaymentOfAnotherCourseRejected() {
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("SEPAY")
                .termsAccepted(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        // Course 100 has no paid payment (payment belonged to another course)
        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yêu cầu thanh toán");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("9. Valid PAID Payment for exact user/course allows registration")
    void testPaidCourseWithValidPaidPaymentAllowed() {
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("SEPAY")
                .termsAccepted(true)
                .build();

        Payment paidPayment = Payment.builder()
                .amount(BigDecimal.valueOf(299000.0))
                .currency("VND")
                .paymentMethod("SEPAY")
                .status("PAID")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.of(paidPayment));
        when(registrationRepository.save(any(CourseRegistration.class))).thenAnswer(i -> i.getArgument(0));

        CourseRegistration result = registrationService.registerCourse(10L, req);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getPaidAmount()).isEqualTo(299000.0);
        assertThat(result.getPaymentMethod()).isEqualTo("SEPAY");
        assertThat(testCourse.getEnrollmentCount()).isEqualTo(11);
        verify(registrationRepository).save(any(CourseRegistration.class));
        verify(courseRepository).save(testCourse);
    }

    @Test
    @DisplayName("10. Registration uses actual Payment.amount, not current course price")
    void testRegistrationUsesActualPaymentAmountNotCurrentCoursePrice() {
        // Course price is 299,000, but learner had a voucher/discount payment of 199,000
        testCourse.setPrice(500000.0);
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("SEPAY")
                .termsAccepted(true)
                .build();

        Payment paidPayment = Payment.builder()
                .amount(BigDecimal.valueOf(199000.0))
                .currency("VND")
                .paymentMethod("SEPAY")
                .status("PAID")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.of(paidPayment));
        when(registrationRepository.save(any(CourseRegistration.class))).thenAnswer(i -> i.getArgument(0));

        CourseRegistration result = registrationService.registerCourse(10L, req);

        assertThat(result.getPaidAmount()).isEqualTo(199000.0);
        assertThat(result.getPaidAmount()).isNotEqualTo(testCourse.getPrice());
    }

    @Test
    @DisplayName("11. Registration uses verified Payment paymentMethod, not frontend-supplied payment method")
    void testRegistrationUsesVerifiedPaymentMethodNotFrontendMethod() {
        // Frontend attempts to submit paymentMethod="FREE" for a paid course
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("FREE")
                .termsAccepted(true)
                .build();

        Payment paidPayment = Payment.builder()
                .amount(BigDecimal.valueOf(299000.0))
                .currency("VND")
                .paymentMethod("SEPAY")
                .status("PAID")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.of(paidPayment));
        when(registrationRepository.save(any(CourseRegistration.class))).thenAnswer(i -> i.getArgument(0));

        CourseRegistration result = registrationService.registerCourse(10L, req);

        assertThat(result.getPaymentMethod()).isEqualTo("SEPAY");
        assertThat(result.getPaymentMethod()).isNotEqualTo("FREE");
    }

    @Test
    @DisplayName("12. Invalid amount or currency is rejected")
    void testPaidCourseWithInvalidAmountOrCurrencyRejected() {
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("SEPAY")
                .termsAccepted(true)
                .build();

        // 12a: Zero amount payment
        Payment zeroAmountPayment = Payment.builder()
                .amount(BigDecimal.ZERO)
                .currency("VND")
                .paymentMethod("SEPAY")
                .status("PAID")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.of(zeroAmountPayment));

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("số tiền thanh toán không đúng");

        // 12b: Wrong currency (USD instead of VND)
        Payment wrongCurrencyPayment = Payment.builder()
                .amount(BigDecimal.valueOf(299000.0))
                .currency("USD")
                .paymentMethod("SEPAY")
                .status("PAID")
                .build();

        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.of(wrongCurrencyPayment));

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đơn vị tiền tệ không hợp lệ");
    }

    @Test
    @DisplayName("13. Duplicate registration is rejected (both pre-check and unique constraint)")
    void testDuplicateRegistrationRejected() {
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .build();

        // Pre-check existsByUserIdAndCourseId
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bạn đã đăng ký khóa học này trước đó");

        // Concurrency race: pre-check passed, but save throws DataIntegrityViolationException
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        Payment paidPayment = Payment.builder()
                .amount(BigDecimal.valueOf(299000.0))
                .currency("VND")
                .paymentMethod("SEPAY")
                .status("PAID")
                .build();
        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.of(paidPayment));
        when(registrationRepository.save(any(CourseRegistration.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint course_registrations_user_id_course_id_key"));

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bạn đã đăng ký khóa học này trước đó");
    }

    @Test
    @DisplayName("14. Failed registration does not increment enrollment count")
    void testFailedRegistrationDoesNotIncrementEnrollmentCount() {
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("SEPAY")
                .termsAccepted(true)
                .build();

        int initialEnrollmentCount = testCourse.getEnrollmentCount();

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        when(paymentRepository.findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(10L, 100L, "PAID"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class);

        assertThat(testCourse.getEnrollmentCount()).isEqualTo(initialEnrollmentCount);
        verify(courseRepository, never()).save(any(Course.class));
    }
}
