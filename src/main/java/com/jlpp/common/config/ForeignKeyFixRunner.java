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
    }
}
