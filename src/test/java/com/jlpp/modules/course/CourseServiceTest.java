package com.jlpp.modules.course;

import com.jlpp.modules.course.dto.CourseFilterRequest;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    private CourseServiceImpl courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseServiceImpl(courseRepository);
    }

    @Test
    @DisplayName("UC-04: View Course List - Search and filter courses successfully")
    void testGetCoursesWithFilter() {
        Course c1 = Course.builder().title("JLPT N5 Toan dien").level("N5").price(299000.0).status("PUBLISHED").build();
        c1.setId(1L);

        Page<Course> page = new PageImpl<>(List.of(c1));
        when(courseRepository.searchCourses(eq("N5"), eq("N5"), any(Pageable.class))).thenReturn(page);

        CourseFilterRequest filter = CourseFilterRequest.builder()
                .keyword("N5")
                .level("N5")
                .sortBy("popular")
                .build();

        Page<Course> result = courseService.getCourses(filter);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("JLPT N5 Toan dien");
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
