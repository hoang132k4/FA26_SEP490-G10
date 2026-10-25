package com.jlpp.modules.course;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.course.dto.CourseRegistrationRequest;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.service.impl.CourseRegistrationServiceImpl;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private CourseRegistrationServiceImpl registrationService;

    private User testUser;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        registrationService = new CourseRegistrationServiceImpl(registrationRepository, courseRepository, userRepository);

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
    @DisplayName("UC-19: Register for Course - Success enrollment")
    void testRegisterCourseSuccess() {
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

        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(testCourse));
        when(registrationRepository.existsByUserIdAndCourseId(10L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> registrationService.registerCourse(10L, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bạn đã đăng ký khóa học này");

        verify(registrationRepository, never()).save(any());
    }
}
