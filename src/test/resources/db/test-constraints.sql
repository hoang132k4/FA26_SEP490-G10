-- Mirrors constraints of the shared Supabase database that Hibernate does not generate from the entities,
-- so tests fail the same way production does. Keep in sync when the real schema changes.

-- questions
ALTER TABLE IF EXISTS questions ADD CONSTRAINT IF NOT EXISTS chk_questions_question_type
    CHECK (question_type IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'PASSAGE'));
ALTER TABLE IF EXISTS questions ADD CONSTRAINT IF NOT EXISTS chk_questions_status
    CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'));
ALTER TABLE IF EXISTS questions ADD CONSTRAINT IF NOT EXISTS chk_questions_skill_type
    CHECK (skill_type IS NULL OR skill_type IN ('VOCABULARY', 'GRAMMAR', 'READING', 'LISTENING'));
ALTER TABLE IF EXISTS questions ADD CONSTRAINT IF NOT EXISTS chk_questions_jlpt_level
    CHECK (jlpt_level IS NULL OR jlpt_level IN ('N1', 'N2', 'N3', 'N4', 'N5'));
ALTER TABLE IF EXISTS questions ADD CONSTRAINT IF NOT EXISTS chk_questions_difficulty
    CHECK (difficulty IS NULL OR difficulty IN ('EASY', 'MEDIUM', 'HARD'));
ALTER TABLE IF EXISTS questions ADD CONSTRAINT IF NOT EXISTS chk_questions_type_options_consistency
    CHECK ((question_type = 'PASSAGE' AND options IS NULL AND answer_key IS NULL)
        OR (question_type IN ('SINGLE_CHOICE', 'MULTI_CHOICE') AND options IS NOT NULL AND answer_key IS NOT NULL));

-- exams
ALTER TABLE IF EXISTS exams ALTER COLUMN total_points SET NOT NULL;
ALTER TABLE IF EXISTS exams ALTER COLUMN exam_type SET NOT NULL;
ALTER TABLE IF EXISTS exams ALTER COLUMN jlpt_level SET NOT NULL;
ALTER TABLE IF EXISTS exams ALTER COLUMN passing_score SET NOT NULL;
ALTER TABLE IF EXISTS exams ALTER COLUMN duration_minutes SET NOT NULL;

-- exam_questions
ALTER TABLE IF EXISTS exam_questions ADD CONSTRAINT IF NOT EXISTS chk_eq_section CHECK (section IN ('LANGUAGE', 'READING', 'LISTENING'));
ALTER TABLE IF EXISTS exam_questions ADD CONSTRAINT IF NOT EXISTS chk_eq_order_index CHECK (order_index > 0);
ALTER TABLE IF EXISTS exam_questions ADD CONSTRAINT IF NOT EXISTS chk_eq_points CHECK (points >= 0);
ALTER TABLE IF EXISTS exam_questions ADD CONSTRAINT IF NOT EXISTS uq_eq_exam_section_order UNIQUE (exam_id, section, order_index);

-- exercises
ALTER TABLE IF EXISTS exercises ADD CONSTRAINT IF NOT EXISTS exercises_status_check CHECK (status IN ('DRAFT', 'PUBLISHED'));
ALTER TABLE IF EXISTS exercises ADD CONSTRAINT IF NOT EXISTS exercises_lesson_id_key UNIQUE (lesson_id);
ALTER TABLE IF EXISTS exercises ALTER COLUMN exercise_title SET NOT NULL;
ALTER TABLE IF EXISTS exercises ALTER COLUMN total_questions SET NOT NULL;
ALTER TABLE IF EXISTS exercises ALTER COLUMN passing_score SET NOT NULL;
ALTER TABLE IF EXISTS exercises ALTER COLUMN status SET NOT NULL;

-- exercise_questions
ALTER TABLE IF EXISTS exercise_questions ALTER COLUMN question_order SET DEFAULT 1;
ALTER TABLE IF EXISTS exercise_questions ALTER COLUMN question_order SET NOT NULL;
