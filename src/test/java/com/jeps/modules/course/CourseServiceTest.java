package com.jeps.modules.course;

import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.course.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    private CourseServiceImpl courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseServiceImpl(courseRepository, null);
    }

    @Test
    @DisplayName("UC-05: View Course Details - Find existing course by ID")
    void testGetCourseByIdSuccess() {
        Course c = Course.builder().title("JLPT N4 Cap toc").level("N4").price(399000.0).build();
        c.setId(2L);

        when(courseRepository.findById(2L)).thenReturn(Optional.of(c));

        Course result = courseService.getCourseById(2L);
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("JLPT N4 Cap toc");
    }

    @Test
    @DisplayName("UC-05: View Course Details - Throw exception if not found")
    void testGetCourseByIdNotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseById(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("999");
    }
}
