package com.jeps.modules.learningpath;

import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.learningpath.service.LearnerVisibleLessons;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LearnerVisibleLessonsTest {

    private Lesson lesson(long id, String status) {
        Lesson lesson = Lesson.builder().title("Bài " + id).status(status).build();
        lesson.setId(id);
        return lesson;
    }

    @Test
    @DisplayName("Hidden (INACTIVE) lessons are removed from the learner's path; ACTIVE and legacy null status stay")
    void filtersHiddenLessons() {
        List<Lesson> result = LearnerVisibleLessons.filter(List.of(
                lesson(1L, "ACTIVE"), lesson(2L, "INACTIVE"), lesson(3L, null), lesson(4L, "inactive")));

        assertThat(result).extracting(Lesson::getId).containsExactly(1L, 3L);
    }
}
