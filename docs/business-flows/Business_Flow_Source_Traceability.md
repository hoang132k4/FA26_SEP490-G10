# Business Flow Source Traceability Matrix

This matrix establishes end-to-end traceability between each business activity, decision, and system operation represented in the refined **JEPS Business Flow Swimlane Diagrams (BF-01, BF-02, BF-03)** and the underlying implementation in the codebase.

---

## 1. Verified Implementation Findings & Technical Discrepancies

1. **Placement Assessment & Adaptive Level Assignment (BF-01):**
   - **Source:** [`placement-intro.html`](file:///c:/Users/hoang/Downloads/JEPS/src/main/resources/templates/pages/exam/placement-intro.html) and [`ExamController.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/exam/controller/ExamController.java) (`takePlacementExam`).
   - **Behavior:** The learner selects their target JLPT level and prior study history. If `hasStudied` is false, the system automatically assigns a foundation diagnostic test (-1 level).
2. **Learning Path Adaptive Recalibration (BF-01):**
   - **Source:** [`LearningPathController.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/learningpath/controller/LearningPathController.java) (`simulateRecalibration` and `applyRecalibratedSchedule`).
   - **Behavior:** Supports an explicit two-step pattern: a Simulation Preview mode (`POST /learning-path/recalibrate`) calculates completion dates in memory without database modifications, followed by a confirmed commit (`POST /learning-path/recalibrate/apply`). If the learner declines, the preview is discarded and the existing schedule is retained.
3. **Course Registration & Payment Gateway (BF-01):**
   - **Source:** [`CourseRegistrationServiceImpl.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/course/service/impl/CourseRegistrationServiceImpl.java) (`registerCourse`).
   - **Behavior & Discrepancy:** The system differentiates free versus paid courses. Free courses are recorded with `paymentMethod = 'FREE'`. For paid courses, the system records `paymentMethod = 'VNPAY'` with immediate direct activation (`status = 'ACTIVE'`). There is currently no active redirection or webhook callback with an external VNPay banking gateway (`com.jlpp.integration.payment` is an architectural placeholder).
4. **JLPT Mock Exam Access & "Registration" (BF-02):**
   - **Source:** [`PublicCourseController.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/course/controller/PublicCourseController.java) and [`ExamController.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/exam/controller/ExamController.java) (`startExam`).
   - **Behavior & Discrepancy:** In JEPS, mock exams do not require a separate paid registration application or fee. Authenticated learners can take any published mock exam immediately via `/exams/{id}/start`. If accessed via the public catalog, clicking "Start Exam" redirects unauthenticated users to `/auth/register?continue=/exams/{id}/start`, returning them automatically to the exam after authentication.
5. **Exam Room Focus Mode & Local Answer Caching (BF-01, BF-02):**
   - **Source:** [`take-exam.html`](file:///c:/Users/hoang/Downloads/JEPS/src/main/resources/templates/pages/exam/take-exam.html).
   - **Behavior:** Radio options are cached in `localStorage` (`exam_answers_{id}`), the timer countdown is maintained in `sessionStorage` (`exam_timer_{id}`), and automatic submission is triggered when the timer reaches 00:00. In manual submission, `#confirmSubmitModal` provides "Làm bài tiếp" (Cancel) and "Nộp bài ngay" (Confirm) options.
6. **Backend Exam Publishing Rule Verification (BF-03 - Critical Code Evidence):**
   - **Source:** [`ManagerExamService.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/exam/service/ManagerExamService.java) lines 285–296 (`validatePublishReady`).
   - **Behavior:** The backend strictly enforces that an exam **cannot be published** without linked questions. The service executes `long linked = examQuestionRepository.countByExamId(examId); int required = form.getTotalQuestions();`. If `linked < required`, it throws `IllegalArgumentException: Không thể chuyển sang PUBLISHED: đề thi cần X câu hỏi nhưng mới gắn Y câu`.
7. **Course Status Toggle Cycle (BF-03):**
   - **Source:** [`ManagerCourseController.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/manager/courses/controller/ManagerCourseController.java) (`transitionCourseStatus`).
   - **Behavior:** Clicking "Change Status" cycles the course lifecycle: `DRAFT` &rarr; `PUBLISHED` &rarr; `ARCHIVED` &rarr; `DRAFT`, persists to DB, and redirects to display updated course details.

---

## 2. Traceability Matrix

### 2.1 BF-01: Take Placement Assessment, Receive Recommended Study Path, and Register for Course

| Phase | Activity / Decision | Responsible Actor | Source File | Method / Endpoint / Query | Implementation Status | Technical Notes |
|---|---|---|---|---|---|---|
| **Phase 1** | Access Placement Assessment Portal | Learner | `ExamController.java` | `GET /exams/placement` | Implemented | Direct entry route. |
| **Phase 1** | Is User Authenticated? | System | `ExamController.java` | `isAuthenticated(...)` | Implemented | Redirects to `/auth/login?continue=...` if false. |
| **Phase 1** | Register or Log In | Learner | `AuthController.java` | `POST /auth/login`, `POST /auth/register` | Implemented | Authenticates and establishes session. |
| **Phase 1** | Display Guidelines & Criteria | System | `placement-intro.html` | Template rendering | Implemented | Outlines diagnostic duration and scoring rules. |
| **Phase 1** | Select Target Level & Prior Study | Learner | `placement-intro.html` | `#placementSurveyModal` | Implemented | Captures target level and prior study history. |
| **Phase 1** | Determine Assigned Level | System | `ExamController.java` | `GET /exams/placement/take` | Implemented | Evaluates `hasStudied`; assigns -1 level if new. |
| **Phase 1** | Initiate Diagnostic Test | Learner | `ExamController.java` | `GET /exams/placement/take` | Implemented | Requests exam take view. |
| **Phase 1** | Retrieve Diagnostic Questions | System | `ExamServiceImpl.java` | `getPlacementExamForTaking(...)` | Implemented | Queries database for level diagnostic questions. |
| **Phase 1** | Questions Available? | System | `take-exam.html` | `th:if="${exam.totalQuestions > 0}"` | Implemented | Checks if questions exist in database. |
| **Phase 1** | Display Question Empty Alert | System | `take-exam.html` | `th:if="${exam.totalQuestions == 0}"` | Implemented | Renders alert if questions not yet configured. |
| **Phase 1** | Enter Focus Room & Answer Questions | Learner | `take-exam.html` | Form inputs | Implemented | Timed Focus Mode room. |
| **Phase 1** | Save Exam Progress | System | `take-exam.html` | `localStorage` / `sessionStorage` | Implemented | Auto-saves selections and tracks timer. |
| **Phase 1** | Review Answers in Modal & Submit | Learner | `take-exam.html` | `#confirmSubmitModal` | Implemented | Displays answered count and submits form. |
| **Phase 1** | Evaluate Placement Answers | System | `ExamServiceImpl.java` | `evaluateAndSaveSubmission(...)` | Implemented | Grades answers against database answer keys. |
| **Phase 1** | Save Result & Update User Level | System | `JdbcLearnerExamResultWriter.java` | `INSERT INTO exam_results` | Implemented | Persists results and updates user profile. |
| **Phase 1** | Review Assessment Results | Learner | `result.html` | `GET /exams/result/{id}` | Implemented | Displays score, gauges, and solutions. |
| **Phase 2** | Generate / Retrieve Recommended Path | System | `LearningPathServiceImpl.java` | `getLearningPath(...)` | Implemented | Generates study timeline from level and skills. |
| **Phase 2** | Inspect Study Path & Milestones | Learner | `LearningPathController.java` | `GET /learning-path` | Implemented | Renders personal study path timeline. |
| **Phase 2** | Adjust Study Intensity? | Learner | `LearningPathController.java` | UI Navigation | Implemented | Optional navigation to `/recalibrate`. |
| **Phase 2** | Input Weekly Hours (Simulation) | Learner | `LearningPathController.java` | `POST /learning-path/recalibrate` | Implemented | Submits simulation parameters. |
| **Phase 2** | Compute Projected Timeline | System | `LearningPathServiceImpl.java` | `getRecalibratePreview(...)` | Implemented | In-memory recalculation without DB writes. |
| **Phase 2** | Apply Proposed Schedule? | Learner | `recalibrate.html` | Confirmation Form | Implemented | Choice to commit or discard simulation. |
| **Phase 2** | Commit New Schedule to DB | System | `LearningPathServiceImpl.java` | `applyRecalibratedSchedule(...)` | Implemented | Commits new weekly hours and schedule to DB. |
| **Phase 3** | Match Level Courses | System | `CourseRecommendationServiceImpl.java` | `getRecommendationsForLearner(...)` | Implemented | Matches target level (100%) and foundation (92%). |
| **Phase 3** | Browse Recommended Courses | Learner | `LearnerCourseController.java` | `GET /courses/recommended` | Implemented | Displays matched course cards. |
| **Phase 3** | Select Course & View Syllabus | Learner | `PublicCourseController.java` | `GET /courses/{id}` | Implemented | Unified course syllabus screen. |
| **Phase 3** | Already Enrolled? | System | `CourseRegistrationServiceImpl.java` | `isUserEnrolled(...)` | Implemented | Prevents duplicate registration. |
| **Phase 3** | Direct Study Access | Learner | `LearnerCourseController.java` | `GET /learning/lessons/{id}` | Implemented | Enrolled users access lessons directly. |
| **Phase 3** | Open Registration Form | Learner | `LearnerCourseController.java` | `GET /courses/{id}/register` | Implemented | Pre-fills learner details. |
| **Phase 3** | Validate Registration & Price | System | `CourseRegistrationServiceImpl.java` | `registerCourse(...)` | Implemented | Checks price and duplicate enrollment. |
| **Phase 3** | Course Price? (Free / Paid) | System | `CourseRegistrationServiceImpl.java` | `course.getPrice() == 0` | Implemented | Evaluates course price. |
| **Phase 3** | Record Payment = FREE | System | `CourseRegistrationServiceImpl.java` | Set `paymentMethod = 'FREE'` | Implemented | Paid amount = 0.0. |
| **Phase 3** | Record Method = VNPAY | System | `CourseRegistrationServiceImpl.java` | Set `paymentMethod = 'VNPAY'` | Implemented | Direct activation (no external gateway call). |
| **Phase 3** | Save Registration (ACTIVE) | System | `CourseRegistrationRepository.java` | `save(registration)` | Implemented | Creates registration and increments count. |

---

### 2.2 BF-02: JLPT Mock Exam Registration and Assessment

| Activity / Decision | Responsible Actor | Source File | Method / Endpoint / Query | Implementation Status | Technical Notes |
|---|---|---|---|---|---|
| Browse JLPT Mock Catalog | Learner | `PublicCourseController.java` | `GET /mock-exam` | Implemented | Public mock exam catalog. |
| Filter by JLPT Level | Learner | `ExamController.java` | `GET /exams/mock?level=...` | Implemented | Level filter buttons (N5-N1). |
| Retrieve Published Mock Exams | System | `ExamServiceImpl.java` | `getLearnerExams(...)` | Implemented | Filters where `exam_type = 'JLPT_MOCK'`. |
| Select Exam & View Details | Learner | `PublicCourseController.java` | `GET /mock-exam/{id}` | Implemented | Card with duration, questions, passing score. |
| Initiate Exam Start | Learner | `ExamController.java` | `GET /exams/{id}/start` | Implemented | Requests exam take room. |
| Is Learner Authenticated? | System | `ExamController.java` | `isAuthenticated(...)` | Implemented | If false, redirects to `/auth/register?continue=...`. |
| Return User to Selected Exam | System | `CustomAuthenticationSuccessHandler.java` | Target URL Redirection | Implemented | Resumes `/exams/{id}/start` upon login. |
| Retrieve Exam Questions | System | `ExamServiceImpl.java` | `getExamForTaking(...)` | Implemented | Queries database for exam question set. |
| Questions Configured? | System | `take-exam.html` | `th:if="${exam.totalQuestions > 0}"` | Implemented | Verifies linked questions exist. |
| Display Warning Alert | System | `take-exam.html` | `th:if="${exam.totalQuestions == 0}"` | Implemented | Explains questions are being prepared. |
| Initialize Focus Room | System | `take-exam.html` | Template rendering | Implemented | Focus banner, timer, question palette. |
| Answer Exam Questions | Learner | `take-exam.html` | Radio inputs | Implemented | Selects answers and filters by skill tabs. |
| Save Exam Progress | System | `take-exam.html` | `localStorage` / `sessionStorage` | Implemented | Caches selections and tracks remaining time. |
| How Is Exam Submitted? | System | `take-exam.html` | Event Listeners | Implemented | Choice: Manual button click vs. Timer timeout. |
| Open Submit Modal | Learner | `take-exam.html` | `#confirmSubmitModal` | Implemented | Opens confirmation dialog with question counts. |
| Confirm Submission? | Learner | `take-exam.html` | Modal Action Buttons | Implemented | Cancel returns to exam; Confirm submits form. |
| Auto-submit on Timeout | System | `take-exam.html` | `timerInterval` handler | Implemented | Triggers automatic submission when timer hits 0. |
| Receive Exam Submission | System | `ExamController.java` | `POST /exams/{id}/submit` | Implemented | Receives answers map and `timeSpentSeconds`. |
| Evaluate Answers & Scores | System | `ExamServiceImpl.java` | `evaluateAndSaveSubmission(...)` | Implemented | Computes score, accuracy, and pass/fail. |
| Generate AI Breakdown | System | `ExamServiceImpl.java` | `generateAiFeedback(...)` | Implemented | Analyzes skill weakness and suggests courses. |
| Save Exam Results | System | `JdbcLearnerExamResultWriter.java` | `INSERT INTO exam_results` | Implemented | Persists master and details; caches in session. |
| Display Exam Results | Learner / System | `result.html` | `GET /exams/result/{id}` | Implemented | Displays score, gauges, pass/fail, and metrics. |
| Review Question Solutions | Learner | `result.html` | Solutions block | Implemented | Displays student choices, correct key, explanations. |
| Post-Exam Action? | Learner | `result.html` | Action buttons | Implemented | Choices: Retake exam, view history, or finish. |
| View Exam History | Learner | `ExamController.java` | `GET /exams/history` | Implemented | Lists historical attempts. |
| Verify Ownership & History | System | `ExamController.java` | `GET /exams/history/{resultId}` | Implemented | IDOR check: verifies `userId` owns the attempt. |

---

### 2.3 BF-03: Course and Assessment Management

| Domain | Activity / Decision | Responsible Actor | Source File | Method / Endpoint / Query | Implementation Status | Technical Notes |
|---|---|---|---|---|---|---|
| **Auth** | Manager Login | Manager | `AuthController.java` | `POST /auth/login` | Implemented | Authenticates manager credentials. |
| **Auth** | Verify Role Authority | System | `SecurityConfig.java` | `.hasAnyRole("MANAGER", "ADMIN")` | Implemented | Protects `/manager/**` routes. |
| **Auth** | Management Domain? | Manager | `sidebar-manager.html` | Navigation Menu | Implemented | Choices: Courses vs. Exams. |
| **Course** | Access Course List | Manager | `ManagerCourseController.java` | `GET /manager/courses` | Implemented | Course management dashboard. |
| **Course** | Retrieve Course List | System | `CourseService.java` | `findCourses()` | Implemented | Queries courses with statuses. |
| **Course** | Course Action? | Manager | `ManagerCourseController.java` | UI Actions | Implemented | Choices: Toggle status vs. Create/Edit course. |
| **Course** | Request Course Status Cycle | Manager | `ManagerCourseController.java` | `POST /manager/courses/{id}/status` | Implemented | Requests status transition. |
| **Course** | Cycle Course Status in DB | System | `CourseService.java` | `transitionCourseStatus(...)` | Implemented | Cycles DRAFT &rarr; PUB &rarr; ARCH &rarr; DRAFT; redirects to course view. |
| **Course** | Input Course Metadata | Manager | `ManagerCourseController.java` | `POST /manager/courses` | Implemented | Inputs title, code, level, price, image. |
| **Course** | Validate Course Data | System | `ManagerCourseController.java` | `validateCourseForm(...)` | Implemented | Validates title, unique code, non-negative price. |
| **Course** | Course Data Valid? | System | `ManagerCourseController.java` | `errors.isEmpty()` | Implemented | If invalid, redisplays form with field errors. |
| **Course** | Save Course (DRAFT) | System | `CourseImageStorageService.java` | `createCourse(...)` / `updateCourse(...)` | Implemented | Stores image to disk; saves record as `DRAFT`. |
| **Course** | Open Lesson Workspace | Manager | `ManagerLessonController.java` | `GET /manager/lessons?courseId=...` | Implemented | Opens lesson management grid for course. |
| **Course** | Group Lessons by Skill | System | `LessonDetailService.java` | `getChapterGroups(...)` | Implemented | Organizes lessons by chapter and 5 skills. |
| **Course** | Author Lessons & Assets | Manager | `ManagerLessonController.java` | `POST /manager/lessons` | Implemented | Creates lessons, attaches audio, notes, quizzes. |
| **Course** | Commit Lesson Assets | System | `LessonRepository.java` | Database Transactions | Implemented | Commits lessons and all assets to database. |
| **Course** | Publish Course? | Manager | `ManagerCourseController.java` | UI Selection | Implemented | Choices: Save Draft vs. Publish Course. |
| **Course** | Publish Course in Catalog | System | `CourseService.java` | `transitionCourseStatus(..., "PUBLISHED")` | Implemented | Makes course and lessons publicly visible. |
| **Exam** | Access Exam Dashboard | Manager | `ManagerExamController.java` | `GET /manager/exams` | Implemented | Lists Placement, Mock, and Milestone exams. |
| **Exam** | Retrieve Exams List | System | `ManagerExamService.java` | `search(...)` | Implemented | Queries exams by type and status. |
| **Exam** | Exam Action? | Manager | `ManagerExamController.java` | UI Actions | Implemented | Choices: Create new exam vs. Edit existing. |
| **Exam** | Configure Exam Metadata | Manager | `ManagerExamController.java` | `POST /manager/exams` | Implemented | Configures title, type, level, duration, score. |
| **Exam** | Save Exam Metadata (DRAFT) | System | `ManagerExamService.java` | `create(...)` / `update(...)` | Implemented | Validates metadata; saves record as `DRAFT`. |
| **Exam** | Open Question Workspace | Manager | `ManagerExamController.java` | `GET /manager/exams/{id}/questions` | Implemented | Displays linked questions and total points. |
| **Exam** | Question Link Method? | Manager | `ManagerExamController.java` | UI Selection | Implemented | Choices: Q-Bank, Excel Import, or Manual CRUD. |
| **Exam** | Link from Question Bank | Manager | `ManagerExamController.java` | `POST /manager/exams/{id}/questions/add` | Implemented | Selects questions, sets section, points, reorders. |
| **Exam** | Batch Excel Import | Manager | `ManagerExamController.java` | `POST /manager/exams/{id}/import` | Implemented | Downloads template; uploads filled spreadsheet. |
| **Exam** | Create in Question Bank | Manager | `ManagerQuestionController.java` | `POST /manager/questions` | Implemented | Creates questions; then links them to exam. |
| **Exam** | Process & Validate Questions | System | `QuestionImportService.java` | `importQuestions(...)` | Implemented | Parses spreadsheet and validates syntax/options. |
| **Exam** | Questions Data Valid? | System | `QuestionImportService.java` | Syntax checks | Implemented | If invalid, rolls back and displays row errors. |
| **Exam** | Request Exam Publishing | Manager | `ManagerExamController.java` | `POST /manager/exams/{id}` | Implemented | Requests status transition to `PUBLISHED`. |
| **Exam** | Linked Qs >= Total Qs? | System | `ManagerExamService.java` | `validatePublishReady(...)` (L285–296) | Implemented | Strict backend validation: blocks publish if questions < totalQuestions. |
| **Exam** | Reject Publishing | System | `ManagerExamService.java` | `throw IllegalArgumentException` | Implemented | Rejects publish request; keeps exam in `DRAFT`. |
| **Exam** | Publish Exam Record | System | `ManagerExamService.java` | Set `status = 'PUBLISHED'` | Implemented | Activates exam for students across portals. |
