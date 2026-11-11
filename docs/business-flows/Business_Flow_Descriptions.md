# Business Flow Descriptions

This document provides formal, end-to-end descriptions of the three primary business processes implemented in the **Japanese Exam Preparation System (JEPS)**, based on actual source code verification across Spring Boot controllers, services, database repositories, and presentation templates.

---

## 2. Main Business Flow

### 2.1 Take the Assessment, Receive a Recommended Study Path, and Register for the Course (BF-01)

#### Description
This business flow represents the learner's journey through three consecutive business phases: Diagnostic Assessment & Level Determination (Phase 1), Recommended Study Path & Adaptive Recalibration (Phase 2), and Course Recommendation & Registration (Phase 3).

**Trigger:** A learner accesses the JEPS platform and initiates the placement assessment process via `/exams/placement` to assess their current Japanese language proficiency and establish an individualized study plan.

**End Condition:** The learner completes the diagnostic assessment, reviews their proficiency results and skill breakdown, receives an AI-aligned recommended study path (with optional intensity recalibration), selects an appropriate JLPT preparatory course, and successfully registers to gain immediate access to the curriculum and lesson materials.

#### Primary Actors
- **Learner:** The student who undergoes diagnostic testing, reviews performance analytics, configures study intensity, and enrolls in courses.
- **System:** Automated backend engine managing authentication, adaptive level assignment (-1 foundation rule), question retrieval from Supabase, timer persistence, scoring, skill weakness analysis, study path generation, and registration processing.

#### Detailed Process Sequence by Business Phase

##### Phase 1: Diagnostic Assessment & Level Determination
1. **Access Assessment Portal:**
   The learner navigates to `/exams/placement`. The System checks whether the user is authenticated. If not, the System redirects them to `/auth/login` (or `/auth/register`) with a return URL, authenticating the learner and creating a valid user session.
2. **Diagnostic Guidelines & Survey:**
   The System displays the diagnostic overview explaining guidelines (30-minute duration, non-penalizing diagnostic grading). The Learner opens the survey popup (`#placementSurveyModal`) and selects their target JLPT level (N5, N4, N3) along with their prior study history.
3. **Adaptive Level Determination:**
   The System evaluates the survey input. If the learner has not studied the target level previously, the System automatically assigns a foundation diagnostic test (-1 level, e.g., target N4 with no prior study receives an N5 diagnostic test) to ensure an accurate baseline measurement.
4. **Initiate Examination:**
   The Learner clicks "Bắt đầu làm bài" (`/exams/placement/take?assignedLevel=...`). The System queries the database for active placement test questions matching the assigned level (`examService.getPlacementExamForTaking`).
5. **Question Availability Validation:**
   - *Alternative Path (Empty Question Set):* If no questions are configured in the database (`totalQuestions == 0`), the System renders an empty state alert warning the learner that diagnostic content is unavailable, terminating the attempt safely.
   - *Main Path (Questions Ready):* The System initializes the Focus Mode exam room (`pages/exam/take-exam.html`) with a 30-minute countdown timer and question palette.
6. **Focus Mode Examination & Local Persistence:**
   The Learner answers multiple-choice questions. Selected answers are auto-saved to browser `localStorage` continuously to prevent data loss. The timer countdown is tracked in `sessionStorage`.
7. **Submission Processing:**
   The Learner clicks "Nộp bài thi", reviews completed question counts in `#confirmSubmitModal`, and confirms submission. The System compares submitted answers against database answer keys, calculates total score, percentage, correct/incorrect/unanswered counts, and categorizes performance by skill. The System persists master results to `exam_results` and details to `exam_result_details`, updates the user's `current_jlpt_level`, and redirects to `/exams/result/{id}`.
8. **Result & Solution Review:**
   The Learner reviews their score, circular accuracy gauge, skill breakdown, AI recommendation, and detailed question-by-question explanations.

##### Phase 2: Recommended Study Path & Adaptive Recalibration
9. **Generate & Retrieve Recommended Study Path:**
   The Learner navigates to `/learning-path`. The System generates or retrieves the personalized study timeline based on the learner's determined level and identified skill weaknesses.
10. **Study Intensity Decision:**
    The Learner inspects the study timeline and decides whether to adjust study intensity:
    - *No Adjustment:* The Learner proceeds directly to course recommendations.
    - *Adjustment Requested:* The Learner navigates to `/learning-path/recalibrate` and enters new weekly study hours.
11. **Simulation Preview Mode:**
    The System calculates the projected completion date and redistributed schedule in memory (`POST /learning-path/recalibrate`) without writing changes to the database, allowing the Learner to safely review the simulation.
12. **Apply Proposed Schedule Decision:**
    The Learner decides whether to commit the proposed schedule:
    - *Cancel / Decline:* The Learner discards the preview, keeping the existing schedule, and proceeds to course recommendations.
    - *Confirm:* The Learner submits confirmation (`POST /learning-path/recalibrate/apply`). The System commits the new weekly hours and updated schedule to the database.

##### Phase 3: Course Recommendation & Registration
13. **Course Recommendation & Catalog Browsing:**
    The Learner accesses `/learning/courses/recommended`. The System matches available published courses: target level courses are tagged with 100% match ("Mục tiêu trọng tâm"), while foundation level courses are tagged with 92% match ("Nền tảng bổ trợ"). The Learner selects a course to view its curriculum details (`/courses/{id}`).
14. **Enrollment Check:**
    The System verifies whether the learner is already enrolled:
    - *Alternative Path (Already Enrolled):* The System displays an active registration notice, allowing the Learner to access lessons directly.
    - *Main Path (New Registration):* The Learner clicks "Đăng ký khóa học" (`/learning/courses/{id}/register`).
15. **Enrollment Submission & Payment Recording:**
    The System pre-fills learner details. The Learner submits the registration form (`POST /learning/courses/{id}/register`). The System evaluates course pricing:
    - *Free Course:* Recorded with `paymentMethod = 'FREE'` and `paidAmount = 0.0`.
    - *Paid Course:* Recorded with `paymentMethod = 'VNPAY'` with immediate direct activation in the current implementation (external gateway verification is currently not integrated).
16. **Access Granted:**
    The System creates an active `CourseRegistration` record (status `ACTIVE`), increments the course's `enrollment_count`, and redirects the learner to start learning lessons (`/learning/lessons/{id}`).

---

### 2.2 JLPT Mock Exam Registration and Assessment (BF-02)

#### Description
This business flow represents the full operational cycle of Japanese Language Proficiency Test (JLPT) mock examinations, including catalog browsing, level filtering, eligibility verification, standardized timed test execution under Focus Mode, automated multi-skill scoring, and in-depth answer explanation review.

**Trigger:** A learner accesses the JLPT Mock Exam catalog (`/mock-exam` publicly or `/exams/mock` in the learner portal) and chooses to participate in a mock examination.

**End Condition:** The examination is submitted, automatically graded against database answer keys, stored in permanent exam history with skill group analytics, and made available for immediate result review and post-examination retake or historical tracking.

#### Primary Actors
- **Learner:** The student taking the standardized JLPT mock exam under strict timed conditions.
- **System:** Automated backend engine managing published exam retrieval, access control, Focus Mode room rendering, local answer caching, automated grading, AI weakness diagnostics, Supabase persistence, and ownership-protected history retrieval.

#### Detailed Process Sequence
1. **Catalog Exploration & Filtering:**
   The Learner accesses the JLPT Mock Exam catalog (`/mock-exam` or `/exams/mock`). The System queries the database for all published exams with `exam_type = 'JLPT_MOCK'`. The Learner filters exams by JLPT level (ALL, N5, N4, N3, N2, N1).
2. **Exam Selection & Start Request:**
   The Learner selects a specific mock exam (e.g., "Đề thi thử JLPT N3 - Chiến lược bứt phá") to view its metadata card: JLPT level, duration, question count, and passing score (`/mock-exam/{id}`). The Learner clicks "Start Exam" (`/exams/{id}/start`).
3. **Authentication Verification & Automatic Return:**
   The System checks user authentication. If unauthenticated, the System redirects to `/auth/register?continue=/exams/{id}/start`. After registering or logging in, the System automatically returns the learner directly to the selected exam start URL.
4. **Question Bank Verification:**
   The System loads the exam via `examService.getExamForTaking(id)`.
   - *Alternative Path (Empty Question Set):* If the exam contains zero questions (`totalQuestions == 0`), the System renders an alert page explaining that test content is being finalized by administrators, preventing an invalid test run.
   - *Main Path (Questions Ready):* The System initializes the Focus Mode exam room (`pages/exam/take-exam.html`).
5. **Focus Mode Examination Room:**
   The System locks down the interface into Focus Mode, displaying:
   - Header with JLPT level pill badge, Focus Mode indicator, and live countdown timer (`exam.durationMinutes * 60` seconds).
   - Question palette ("Lưới câu hỏi") with real-time answered count (`answered / total`) and progress bar.
   - Skill tabs allowing the Learner to isolate questions by category (Từ vựng, Ngữ pháp, Đọc hiểu, Nghe hiểu).
6. **Active Answering & State Persistence:**
   As the Learner selects radio options, answers are instantly synchronized to `localStorage` under `exam_answers_{id}`. The remaining time is stored in `sessionStorage` (`exam_timer_{id}`) to prevent loss in case of accidental page refreshes.
7. **Submission Decision & Processing:**
   The submission is executed through one of two mechanisms:
   - *Manual Submission:* The Learner clicks "Nộp bài thi". The System opens `#confirmSubmitModal` showing answered vs unanswered counts.
     - *Cancel:* The Learner clicks "Làm bài tiếp" and returns to answering questions.
     - *Confirm:* The Learner clicks "Nộp bài ngay" and submits.
   - *Automatic Timeout:* When the countdown timer reaches 00:00, client-side JavaScript intercepts the turn, alerts the learner, and automatically submits the form.
8. **Automated Grading & Analysis:**
   The form data is submitted to `POST /exams/{id}/submit`. The System:
   - Compares each submitted answer against correct options in the database.
   - Computes total score, percentage accuracy, and pass/fail outcome (`score >= passingScore`).
   - Calculates skill category breakdowns (e.g., Vocabulary score vs. Reading score).
   - Formulates AI heuristic recommendations based on the weakest skill group.
   - Inserts records into `exam_results` and `exam_result_details`.
   - Saves the result DTO into session and redirects to `/exams/result/{id}?resultId={id}`.
9. **Result Dashboard & Solutions Inspection:**
   The Learner reviews their score, circular percentage gauge, pass/fail badge, 4 key counters (Correct, Incorrect, Unanswered, Time Taken), skill breakdown progress bars, and AI advice. The Learner inspects question solutions comparing selected answers against correct options with detailed explanations.
10. **Post-Exam Progression:**
    The Learner chooses their next action:
    - *Retake Exam:* Clicks "Làm lại đề thi này" to restart the test room.
    - *Review History:* Clicks "Lịch sử thi" (`/exams/history`) to inspect previous attempts. The System validates record ownership before rendering historical results (`/exams/history/{resultId}`) to prevent IDOR vulnerabilities.
    - *Finish:* Returns to the Exam Center (`/exams`) or Learner Dashboard, reaching the consolidated terminal state: **Mock Exam Completed & Results Available**.

---

### 2.3 Course and Assessment Management (BF-03)

#### Description
This business flow represents the management and publishing lifecycle for academic content within JEPS. It encompasses two structured management domains executed by authorized Managers or Administrators: Course & Curriculum Management Lifecycle (Domain 1) and Assessment & Question Bank Management Lifecycle (Domain 2).

**Trigger:** An authorized Manager or Administrator logs into the JEPS management portal and selects Course Management (`/manager/courses`) or Exam Management (`/manager/exams`).

**End Condition:** Courses, lessons, materials, and assessments are successfully created, validated, populated with learning content or linked questions, and published to the public catalog and learner portals.

#### Primary Actors
- **Manager / Administrator:** Authorized staff member responsible for curriculum creation, lesson authoring, question authoring, exam configuration, and publishing.
- **System:** Backend management platform validating data consistency, enforcing unique course codes, managing disk/cloud image and audio uploads, parsing batch Excel question spreadsheets, linking questions, and enforcing backend publishing prerequisites.

#### Detailed Process Sequence

##### Domain 1: Course & Curriculum Management Lifecycle
1. **Manager Authentication & Role Verification:**
   The Manager logs into JEPS (`/auth/login`). The System verifies role authorization (`hasAnyRole('MANAGER', 'ADMIN')`) before granting access to `/manager/**` routes.
2. **Course Catalog Inspection:**
   The Manager accesses the Course Management dashboard (`/manager/courses`). The System lists all courses with level, code, enrollment count, and current status (`DRAFT`, `PUBLISHED`, `ARCHIVED`).
3. **Course Action Decision:**
   - *Status Toggle Cycle:* The Manager requests status change via `POST /manager/courses/{id}/status`. The System cycles status: `DRAFT` &rarr; `PUBLISHED` &rarr; `ARCHIVED` &rarr; `DRAFT`, updates the database record, and displays the updated course list/details.
   - *Create or Edit Course:* The Manager inputs Title, Code, Level, Price, Descriptions, Objectives, and uploads/links a thumbnail.
4. **Course Validation & Saving (DRAFT):**
   The System validates course attributes (title not empty, unique course code, non-negative price, valid image).
   - *Validation Failure:* Field errors are displayed on the form for correction.
   - *Validation Success:* The System stores the thumbnail to disk storage and saves the course record in `DRAFT` status.
5. **Lesson Workspace Configuration:**
   The Manager opens `/manager/lessons?courseId={id}`. The System displays existing lessons structured into chapters and 5 skill columns (Từ vựng, Ngữ pháp, Hán tự, Đọc hiểu, Nghe hiểu).
6. **Lesson Authoring & Content Attachment:**
   The Manager authors lessons, uploads audio files, attaches PDF documents and notes (`StudyMaterialService`), configures flashcards (`ManagerFlashcardService`), and attaches practice quizzes (`ManagerQuizService`). The System commits the lesson hierarchy and assets to the database.
7. **Course Publishing Decision:**
   The Manager decides whether to publish the course:
   - *Save Draft:* The course remains in `DRAFT` for ongoing editing and returns to the course dashboard.
   - *Publish:* The System updates course status to `PUBLISHED`, making the course and all attached lessons publicly visible in the course catalog and learner hub.

##### Domain 2: Assessment & Question Bank Management Lifecycle
8. **Exam Management Access:**
   The Manager accesses `/manager/exams`. The System queries and displays existing exams filtered by type (`PLACEMENT_TEST`, `JLPT_MOCK`, `MILESTONE`), JLPT level, and status.
9. **Exam Creation & Configuration:**
   The Manager configures exam metadata (Title, Type, JLPT Level, Duration, Passing Score, Description). The System validates fields and saves the exam in `DRAFT` status (`POST /manager/exams`).
10. **Question Association Workspace:**
    The Manager opens `/manager/exams/{id}/questions`. The System displays current linked questions and total points. The Manager selects from three supported question association methods:
    - **Method 1: Link from Question Bank:** The Manager searches available questions by JLPT level and skill type, selects questions, assigns section and points, and submits (`POST /manager/exams/{id}/questions/add`). The Manager can re-order questions using Move Up / Move Down buttons.
    - **Method 2: Batch Excel Import:** The Manager downloads the standardized Excel template (`GET /manager/exams/import/template`), fills in question stems, options, correct answers, and points, and uploads the file (`POST /manager/exams/{id}/import`). The System validates all rows. If errors exist, it throws `QuestionImportException`, rolls back, and displays row errors. If valid, it batch-inserts and links all questions.
    - **Method 3: Standalone Question Bank CRUD:** The Manager creates questions in `/manager/questions` and then associates them with the exam via the question link interface.
11. **Backend Publishing Validation (Strict Enforcement in Code):**
    The Manager reviews total points and requests status transition to `PUBLISHED` (`POST /manager/exams/{id}`).
    - **Backend Validation Rule (`validatePublishReady` in `ManagerExamService.java`):**
      The System checks whether `linked questions >= totalQuestions`.
      - *Rule Violation (`linked < totalQuestions`):* The System throws `IllegalArgumentException: Không thể chuyển sang PUBLISHED: đề thi cần X câu hỏi nhưng mới gắn Y câu`, rejecting publication and keeping the exam in `DRAFT`.
      - *Rule Satisfied (`linked >= totalQuestions`):* The System transitions status to `PUBLISHED`, activating the exam across public mock exam catalogs, the learner exam center, and milestone lists.
