package com.jeps.modules.quiz;

import com.jeps.modules.quiz.dto.LessonOption;
import com.jeps.modules.quiz.repository.ManagerQuizRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Rows the H2 schema cannot produce (lessons.course_id has a foreign key, courses.level is NOT NULL). */
@ExtendWith(MockitoExtension.class)
class ManagerQuizRepositoryTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @InjectMocks private ManagerQuizRepository repository;

    @Test
    void lessonWithoutSectionCourseOrLevelIsGroupedUnderFallback() throws SQLException {
        assertEquals("Khác", mapLesson(null, null, null).group());
    }

    @Test
    void lessonWithBlankSectionAndNoLevelIsGroupedByCourseTitle() throws SQLException {
        assertEquals("Khoá A", mapLesson("  ", "Khoá A", null).group());
    }

    @Test
    void lessonWithSectionAndLevelIsGroupedByBoth() throws SQLException {
        LessonOption lesson = mapLesson("Chương 1", "Khoá A", "N5");

        assertEquals("N5 · Chương 1", lesson.group());
        assertEquals(7L, lesson.id());
        assertEquals("Bài 7", lesson.title());
    }

    @SuppressWarnings("unchecked")
    private LessonOption mapLesson(String section, String course, String level) throws SQLException {
        repository.findLessons();
        ArgumentCaptor<RowMapper<LessonOption>> mapper = ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbcTemplate).query(anyString(), mapper.capture());

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("section_name")).thenReturn(section);
        when(rs.getString("course_title")).thenReturn(course);
        when(rs.getString("jlpt_level")).thenReturn(level);
        when(rs.getLong("lesson_id")).thenReturn(7L);
        when(rs.getString("lesson_title")).thenReturn("Bài 7");
        return mapper.getValue().mapRow(rs, 0);
    }
}
