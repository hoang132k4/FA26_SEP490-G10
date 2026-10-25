package com.jlpp.modules.course;

import com.jlpp.modules.course.dto.RecommendedCourseItem;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.service.impl.CourseRecommendationServiceImpl;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRecommendationServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseRegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseRecommendationServiceImpl recommendationService;

    private User learner;
    private Course n4Course;
    private Course n5Course;

    @BeforeEach
    void setUp() {
        learner = new User();
        learner.setId(1L);
        learner.setEmail("learner@jeps.edu.vn");
        learner.setFullName("Nguyen Van Hoc");
        learner.setCurrentJlptLevel("N5");
        learner.setTargetJlptLevel("N4");

        n4Course = Course.builder()
                .code("JLPT-N4")
                .title("Khóa học JLPT N4 Cấp tốc")
                .level("N4")
                .status("PUBLISHED")
                .rating(4.8)
                .enrollmentCount(120)
                .build();
        n4Course.setId(101L);

        n5Course = Course.builder()
                .code("JLPT-N5")
                .title("Khóa học JLPT N5 Toàn diện")
                .level("N5")
                .status("PUBLISHED")
                .rating(4.9)
                .enrollmentCount(250)
                .build();
        n5Course.setId(100L);
    }

    @Test
    void testGetRecommendationsForLearner_ReturnsRankedCourses() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(learner));
        when(courseRepository.findByLevelAndStatus("N4", "PUBLISHED")).thenReturn(List.of(n4Course));
        when(courseRepository.findByLevelAndStatus("N5", "PUBLISHED")).thenReturn(List.of(n5Course));
        when(registrationRepository.existsByUserIdAndCourseId(1L, 101L)).thenReturn(false);
        when(registrationRepository.existsByUserIdAndCourseId(1L, 100L)).thenReturn(true);

        List<RecommendedCourseItem> items = recommendationService.getRecommendationsForLearner(1L);

        assertNotNull(items);
        assertEquals(2, items.size());

        // The target level N4 should match the top recommendation
        RecommendedCourseItem topMatch = items.get(0);
        assertEquals(101L, topMatch.getCourse().getId());
        assertEquals(98, topMatch.getMatchPercentage());
        assertTrue(topMatch.getMatchReason().contains("N4"));
        assertFalse(topMatch.isEnrolled());

        // The already enrolled N5 course
        RecommendedCourseItem second = items.get(1);
        assertEquals(100L, second.getCourse().getId());
        assertEquals(92, second.getMatchPercentage());
        assertTrue(second.isEnrolled());
    }

    @Test
    void testGetRecommendationsForLearner_FallbackWhenNoDirectMatches() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(learner));
        when(courseRepository.findByLevelAndStatus(anyString(), anyString())).thenReturn(List.of());
        when(courseRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED")).thenReturn(List.of(n4Course));
        when(registrationRepository.existsByUserIdAndCourseId(1L, 101L)).thenReturn(false);

        List<RecommendedCourseItem> items = recommendationService.getRecommendationsForLearner(1L);

        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals(85, items.get(0).getMatchPercentage());
    }
}
