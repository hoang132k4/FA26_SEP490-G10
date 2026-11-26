package com.jeps.modules.course;

import com.jeps.common.constants.RoleEnum;
import com.jeps.modules.course.dto.CourseRegistrationRequest;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.entity.CourseRegistration;
import com.jeps.modules.course.repository.CourseRegistrationRepository;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.course.service.impl.CourseRegistrationServiceImpl;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import com.jeps.modules.course.service.CoursePriceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private CoursePriceCalculator priceCalculator;

    private CourseRegistrationServiceImpl registrationService;

    private User testUser;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        priceCalculator = new CoursePriceCalculator();
        registrationService = new CourseRegistrationServiceImpl(
                registrationRepository, courseRepository, userRepository, priceCalculator);

        testUser = User.builder()
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .role(RoleEnum.LEARNER)
                .build();
        testUser.setId(10L);

        testCourse = Course.builder()
                .title("JLPT N5 Toan dien")
                .level("N5")
                .status("PUBLISHED")
                .price(299000.0)
                .enrollmentCount(10)
                .build();
        testCourse.setId(100L);
    }

    @Test
    @DisplayName("UC-19: Register for Course - Success enrollment")
    void testRegisterCourseSuccess() {
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .paymentMethod("FREE")
                .termsAccepted(true)
                .build();

        testCourse.setPrice(0.0);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(false);
        when(registrationRepository.save(any(CourseRegistration.class))).thenAnswer(i -> i.getArgument(0));

        CourseRegistration result = registrationService.registerCourse(10L, req);

        assertThat(result).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(10L);
        assertThat(result.getCourse().getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(CourseRegistration.STATUS_ACTIVE);
        assertThat(result.getPaymentMethod()).isEqualTo("FREE");
        assertThat(result.getPaidAmount()).isZero();
        assertThat(testCourse.getEnrollmentCount()).isEqualTo(11);

        verify(registrationRepository).save(any(CourseRegistration.class));
        verify(courseRepository).save(testCourse);
    }

    @Test
    @DisplayName("UC-19: Register for Course - Duplicate registration throws IllegalStateException")
    void testRegisterCourseDuplicate() {
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .build();

        testCourse.setPrice(0.0);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bạn đã đăng ký khóa học này");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void createsPendingRegistrationForPaidCourseWithoutGrantingEnrollment() {
        CourseRegistrationRequest request = CourseRegistrationRequest.builder()
                .courseId(100L)
                .paymentMethod("MOMO")
                .note("requested")
                .build();

        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.findForUpdateByUserIdAndCourseId(10L, 100L)).thenReturn(Optional.empty());
        when(registrationRepository.save(any(CourseRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseRegistration registration = registrationService.createPendingRegistration(10L, request);

        assertThat(registration.getStatus()).isEqualTo(CourseRegistration.STATUS_PENDING_PAYMENT);
        assertThat(registration.getPaymentMethod()).isEqualTo("SEPAY");
        assertThat(registration.getPaidAmount()).isZero();
        assertThat(testCourse.getEnrollmentCount()).isEqualTo(10);
        verify(courseRepository, never()).save(testCourse);
    }

    @Test
    void paidCourseCannotBeActivatedThroughImmediateRegistration() {
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));

        assertThatThrownBy(() -> registrationService.registerCourse(10L,
                CourseRegistrationRequest.builder().courseId(100L).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cần được thanh toán");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void freeCourseAccessCreatesOneActiveFreeRegistration() {
        testCourse.setPrice(0.0);
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.findByUserIdAndCourseId(10L, 100L)).thenReturn(Optional.empty());
        when(registrationRepository.save(any(CourseRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseRegistration registration =
                registrationService.ensureFreeRegistrationForAccess(10L, 100L).orElseThrow();

        assertThat(registration.getStatus()).isEqualTo(CourseRegistration.STATUS_ACTIVE);
        assertThat(registration.getPaymentMethod()).isEqualTo("FREE");
        assertThat(testCourse.getEnrollmentCount()).isEqualTo(11);
        verify(registrationRepository, times(1)).save(any(CourseRegistration.class));
    }

    @Test
    void freeCourseAccessDoesNotCreateAnotherRegistrationForExistingRows() {
        testCourse.setPrice(0.0);
        CourseRegistration pending = CourseRegistration.builder()
                .status(CourseRegistration.STATUS_PENDING_PAYMENT)
                .build();
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.findByUserIdAndCourseId(10L, 100L)).thenReturn(Optional.of(pending));

        assertThat(registrationService.ensureFreeRegistrationForAccess(10L, 100L)).isEmpty();
        verify(registrationRepository, never()).save(any());
        assertThat(testCourse.getEnrollmentCount()).isEqualTo(10);
    }

    @Test
    void freeCourseAccessReturnsAnExistingActiveRegistrationWithoutIncrementingEnrollment() {
        testCourse.setPrice(0.0);
        CourseRegistration active = CourseRegistration.builder()
                .status(CourseRegistration.STATUS_ACTIVE)
                .build();
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.findByUserIdAndCourseId(10L, 100L)).thenReturn(Optional.of(active));

        assertThat(registrationService.ensureFreeRegistrationForAccess(10L, 100L))
                .containsSame(active);
        verify(registrationRepository, never()).save(any());
        assertThat(testCourse.getEnrollmentCount()).isEqualTo(10);
    }

    @Test
    void freeCourseAccessNeverAutoEnrollsPaidCourses() {
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.findByUserIdAndCourseId(10L, 100L)).thenReturn(Optional.empty());

        assertThat(registrationService.ensureFreeRegistrationForAccess(10L, 100L)).isEmpty();
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void freeCourseAccessDoesNotAutoEnrollUnpublishedCourses() {
        testCourse.setPrice(0.0);
        testCourse.setStatus("DRAFT");
        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.findByUserIdAndCourseId(10L, 100L)).thenReturn(Optional.empty());

        assertThat(registrationService.ensureFreeRegistrationForAccess(10L, 100L)).isEmpty();
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void completesTheExactPendingRegistrationOnlyOnce() {
        CourseRegistration pending = CourseRegistration.builder()
                .user(testUser)
                .course(testCourse)
                .status(CourseRegistration.STATUS_PENDING_PAYMENT)
                .paidAmount(0.0)
                .build();
        pending.setId(500L);
        when(registrationRepository.findForUpdateByIdAndUserIdAndCourseId(500L, 10L, 100L))
                .thenReturn(Optional.of(pending));
        when(registrationRepository.save(pending)).thenReturn(pending);

        CourseRegistration activated = registrationService.completePaidRegistration(
                10L, 100L, 500L, new BigDecimal("299000"), "SEPAY");
        CourseRegistration duplicate = registrationService.completePaidRegistration(
                10L, 100L, 500L, new BigDecimal("299000"), "SEPAY");

        assertThat(activated).isSameAs(pending);
        assertThat(duplicate).isSameAs(pending);
        assertThat(pending.getStatus()).isEqualTo(CourseRegistration.STATUS_ACTIVE);
        assertThat(pending.getPaidAmount()).isEqualTo(299000.0);
        verify(courseRepository, times(1)).save(testCourse);
    }

    @Test
    @DisplayName("UC-19: Register for Course - Reject DRAFT or ARCHIVED course")
    void testRegisterCourseNotPublishedThrows() {
        testCourse.setStatus("DRAFT");
        CourseRegistrationRequest req = CourseRegistrationRequest.builder()
                .courseId(100L)
                .fullName("Nguyen Van A")
                .email("learner@jeps.edu.vn")
                .build();

        when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chưa được xuất bản hoặc đã lưu trữ");

        verify(registrationRepository, never()).save(any());
    }
}
