package com.jlpp.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ForeignKeyFixRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ForeignKeyFixRunner.class);
    private final JdbcTemplate jdbcTemplate;

    public ForeignKeyFixRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Boolean exists = jdbcTemplate.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM information_schema.table_constraints
                        WHERE constraint_name = 'exercise_questions_question_id_fkey'
                          AND table_name = 'exercise_questions'
                    )
                    """, Boolean.class);
            if (Boolean.TRUE.equals(exists)) {
                jdbcTemplate.execute("ALTER TABLE exercise_questions DROP CONSTRAINT exercise_questions_question_id_fkey");
                jdbcTemplate.execute("ALTER TABLE exercise_questions ADD CONSTRAINT exercise_questions_question_id_fkey FOREIGN KEY (question_id) REFERENCES questions(id)");
                log.info("Fixed exercise_questions FK: now references questions(id) instead of question_bank");
            }
        } catch (Exception e) {
            log.warn("FK fix skipped: {}", e.getMessage());
        }

        try {
            String seq = jdbcTemplate.queryForObject("SELECT pg_get_serial_sequence('lessons', 'lesson_id')", String.class);
            if (seq != null) {
                var courseIds = jdbcTemplate.queryForList("""
                        SELECT c.course_id
                        FROM courses c
                        WHERE NOT EXISTS (
                            SELECT 1 FROM lessons l
                            WHERE l.course_id = c.course_id
                              AND (
                                  UPPER(COALESCE(l.lesson_type, '')) IN ('EXAM', 'MILESTONE')
                                  OR UPPER(COALESCE(l.skill_type, '')) IN ('EXAM', 'MILESTONE')
                                  OR LOWER(COALESCE(l.title, l.lesson_title, '')) LIKE '%giữa kì%'
                                  OR LOWER(COALESCE(l.title, l.lesson_title, '')) LIKE '%giữa kỳ%'
                                  OR LOWER(COALESCE(l.title, l.lesson_title, '')) LIKE '%milestone%'
                              )
                        )
                        """, Long.class);
                for (Long courseId : courseIds) {
                    Long lessonId = jdbcTemplate.queryForObject("SELECT nextval(pg_get_serial_sequence('lessons', 'lesson_id'))", Long.class);
                    Integer maxOrder = jdbcTemplate.queryForObject(
                            "SELECT COALESCE(MAX(order_index), 0) FROM lessons WHERE course_id = ?", Integer.class, courseId);
                    int nextOrder = (maxOrder == null ? 0 : maxOrder) + 1;
                    String title = "Bài thi giữa kì: Đánh giá năng lực giai đoạn 1";
                    String section = "Kiểm tra & Đánh giá định kỳ";
                    String desc = "Bài kiểm tra cột mốc định kỳ bắt buộc để đánh giá năng lực học viên.";
                    String content = "Hoàn thành bài kiểm tra cột mốc để đánh giá mức độ tiếp thu kiến thức.";
                    jdbcTemplate.update("""
                            INSERT INTO lessons (lesson_id, id, course_id,
                                                 lesson_title, title,
                                                 skill_type, lesson_type,
                                                 order_index, lesson_order,
                                                 description, content, section_name,
                                                 duration_minutes, is_free_preview, status,
                                                 created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, NULL, 'EXAM', ?, ?, ?, ?, ?, 35, FALSE, 'ACTIVE', NOW(), NOW())
                            """,
                            lessonId, lessonId, courseId,
                            title, title,
                            nextOrder, nextOrder,
                            desc, content, section);
                    log.info("Auto-provisioned Milestone lesson (id={}) for courseId={}", lessonId, courseId);
                }
            }
        } catch (Exception e) {
            log.debug("Milestone lesson auto-provisioning skipped: {}", e.getMessage());
        }
    }
}
