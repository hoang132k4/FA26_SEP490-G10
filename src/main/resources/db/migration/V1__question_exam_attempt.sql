-- ============================================================================
-- Migration V1: Question Bank, Exam-Question linkage, Attempts
-- Idempotent: an toàn khi chạy lại nếu lần trước bị lỗi giữa chừng
-- CHỈ thay đổi: questions (thêm cột) + tạo exam_questions, attempts
-- KHÔNG ảnh hưởng: users, courses, lessons, exercises, exercise_questions,
--                  course_registrations, exams, hoặc bất kỳ bảng nào khác
-- ============================================================================

BEGIN;

-- ============================================================================
-- PHẦN 0: Dọn constraint cũ nếu còn sót từ lần chạy trước
-- ============================================================================

ALTER TABLE questions DROP CONSTRAINT IF EXISTS chk_questions_question_type;
ALTER TABLE questions DROP CONSTRAINT IF EXISTS chk_questions_jlpt_level;
ALTER TABLE questions DROP CONSTRAINT IF EXISTS chk_questions_skill_type;
ALTER TABLE questions DROP CONSTRAINT IF EXISTS chk_questions_difficulty;
ALTER TABLE questions DROP CONSTRAINT IF EXISTS chk_questions_options_is_array;
ALTER TABLE questions DROP CONSTRAINT IF EXISTS chk_questions_answer_key_is_array;
ALTER TABLE questions DROP CONSTRAINT IF EXISTS fk_questions_parent;
ALTER TABLE questions DROP CONSTRAINT IF EXISTS chk_questions_parent_not_self;
ALTER TABLE questions DROP CONSTRAINT IF EXISTS chk_questions_status;
ALTER TABLE questions DROP CONSTRAINT IF EXISTS chk_questions_type_options_consistency;

DROP TABLE IF EXISTS attempts;
DROP TABLE IF EXISTS exam_questions;

-- ============================================================================
-- PHẦN 1: Thêm cột mới vào bảng questions
-- ============================================================================

ALTER TABLE questions ADD COLUMN IF NOT EXISTS question_type VARCHAR(20) NOT NULL DEFAULT 'SINGLE_CHOICE';
ALTER TABLE questions ADD COLUMN IF NOT EXISTS jlpt_level VARCHAR(5);
ALTER TABLE questions ADD COLUMN IF NOT EXISTS skill_type VARCHAR(20);
ALTER TABLE questions ADD COLUMN IF NOT EXISTS difficulty VARCHAR(10);
ALTER TABLE questions ADD COLUMN IF NOT EXISTS options JSONB;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS answer_key JSONB;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE questions ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT now();
ALTER TABLE questions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT now();

-- Fix data cũ: row chưa có options/answer_key → gán placeholder
UPDATE questions
SET options = '[{"key":"A","text":""},{"key":"B","text":""},{"key":"C","text":""},{"key":"D","text":""}]'::jsonb,
    answer_key = '["A"]'::jsonb
WHERE options IS NULL AND answer_key IS NULL AND question_type <> 'PASSAGE';

-- Thêm constraint
ALTER TABLE questions ADD CONSTRAINT chk_questions_question_type
    CHECK (question_type IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'PASSAGE'));

ALTER TABLE questions ADD CONSTRAINT chk_questions_jlpt_level
    CHECK (jlpt_level IS NULL OR jlpt_level IN ('N1', 'N2', 'N3', 'N4', 'N5'));

ALTER TABLE questions ADD CONSTRAINT chk_questions_skill_type
    CHECK (skill_type IS NULL OR skill_type IN ('VOCABULARY', 'GRAMMAR', 'READING', 'LISTENING'));

ALTER TABLE questions ADD CONSTRAINT chk_questions_difficulty
    CHECK (difficulty IS NULL OR difficulty IN ('EASY', 'MEDIUM', 'HARD'));

ALTER TABLE questions ADD CONSTRAINT chk_questions_options_is_array
    CHECK (options IS NULL OR jsonb_typeof(options) = 'array');

ALTER TABLE questions ADD CONSTRAINT chk_questions_answer_key_is_array
    CHECK (answer_key IS NULL OR jsonb_typeof(answer_key) = 'array');

ALTER TABLE questions ADD CONSTRAINT fk_questions_parent
    FOREIGN KEY (parent_id) REFERENCES questions(id);

ALTER TABLE questions ADD CONSTRAINT chk_questions_parent_not_self
    CHECK (parent_id IS NULL OR parent_id <> id);

ALTER TABLE questions ADD CONSTRAINT chk_questions_status
    CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'));

ALTER TABLE questions ADD CONSTRAINT chk_questions_type_options_consistency
    CHECK (
        (question_type = 'PASSAGE' AND options IS NULL AND answer_key IS NULL)
        OR
        (question_type IN ('SINGLE_CHOICE', 'MULTI_CHOICE') AND options IS NOT NULL AND answer_key IS NOT NULL)
    );

CREATE INDEX IF NOT EXISTS idx_questions_parent_id ON questions(parent_id);

-- ============================================================================
-- PHẦN 2: Tạo exam_questions + migrate data cũ
-- ============================================================================

CREATE TABLE exam_questions (
    exam_id     BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    section     VARCHAR(20) NOT NULL DEFAULT 'LANGUAGE',
    order_index INT NOT NULL DEFAULT 1,
    points      INT NOT NULL DEFAULT 1,

    PRIMARY KEY (exam_id, question_id),

    CONSTRAINT fk_eq_exam
        FOREIGN KEY (exam_id) REFERENCES exams(exam_id) ON DELETE CASCADE,
    CONSTRAINT fk_eq_question
        FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT chk_eq_section
        CHECK (section IN ('LANGUAGE', 'READING', 'LISTENING')),
    CONSTRAINT chk_eq_order_index
        CHECK (order_index > 0),
    CONSTRAINT chk_eq_points
        CHECK (points >= 0),
    CONSTRAINT uq_eq_exam_section_order
        UNIQUE (exam_id, section, order_index)
);

-- Migrate: chuyển exam_id/order_index/points cũ sang exam_questions
INSERT INTO exam_questions (exam_id, question_id, section, order_index, points)
SELECT
    q.exam_id,
    q.id,
    'LANGUAGE',
    COALESCE(q.order_index, ROW_NUMBER() OVER (PARTITION BY q.exam_id ORDER BY q.id)),
    COALESCE(q.points, 1)
FROM questions q
WHERE q.exam_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- ============================================================================
-- PHẦN 3: Xóa cột cũ khỏi questions
-- ============================================================================

ALTER TABLE questions DROP COLUMN IF EXISTS exam_id;
ALTER TABLE questions DROP COLUMN IF EXISTS order_index;
ALTER TABLE questions DROP COLUMN IF EXISTS points;

-- ============================================================================
-- PHẦN 4: Tạo bảng attempts
-- ============================================================================

CREATE TABLE attempts (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    exam_id          BIGINT,
    exercise_id      BIGINT,
    answers          JSONB NOT NULL DEFAULT '{}',
    grading_snapshot JSONB,
    score            INT,
    max_score        INT,
    passed           BOOLEAN,
    status           VARCHAR(15) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    submitted_at     TIMESTAMPTZ,

    CONSTRAINT fk_attempts_user
        FOREIGN KEY (user_id) REFERENCES users,
    CONSTRAINT fk_attempts_exam
        FOREIGN KEY (exam_id) REFERENCES exams(exam_id),
    CONSTRAINT fk_attempts_exercise
        FOREIGN KEY (exercise_id) REFERENCES exercises(exercise_id),
    CONSTRAINT chk_attempts_target
        CHECK ((exam_id IS NULL) <> (exercise_id IS NULL)),
    CONSTRAINT chk_attempts_status
        CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'EXPIRED', 'ABANDONED')),
    CONSTRAINT chk_attempts_score
        CHECK (score IS NULL OR score >= 0),
    CONSTRAINT chk_attempts_max_score
        CHECK (max_score IS NULL OR max_score >= 0),
    CONSTRAINT chk_attempts_score_le_max
        CHECK (score IS NULL OR max_score IS NULL OR score <= max_score),
    CONSTRAINT chk_attempts_submitted_complete
        CHECK (
            status <> 'SUBMITTED'
            OR (
                submitted_at IS NOT NULL
                AND grading_snapshot IS NOT NULL
                AND score IS NOT NULL
                AND max_score IS NOT NULL
                AND passed IS NOT NULL
            )
        )
);

CREATE INDEX IF NOT EXISTS idx_attempts_user_exam
    ON attempts(user_id, exam_id) WHERE exam_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_attempts_user_exercise
    ON attempts(user_id, exercise_id) WHERE exercise_id IS NOT NULL;

COMMIT;
