-- ============================================================================
-- Migration V2: Add lesson_progress JSONB column to learning_paths
-- Idempotent & Non-destructive
-- Purpose: Store granular, lesson-level completion states and timestamps
-- Structure:
--   {
--     "version": 1,
--     "lessons": {
--       "<lesson_id>": {
--         "status": "COMPLETED",
--         "startedAt": "2026-11-11T08:00:00Z",
--         "completedAt": "2026-11-11T08:35:00Z"
--       }
--     }
--   }
-- ============================================================================

BEGIN;

ALTER TABLE learning_paths 
ADD COLUMN IF NOT EXISTS lesson_progress JSONB;

COMMENT ON COLUMN learning_paths.lesson_progress IS 'Stores granular per-lesson completion status, startedAt, and completedAt in JSONB format.';

COMMIT;

