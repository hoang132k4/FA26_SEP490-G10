package com.jlpp.modules.learningpath;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.entity.Lesson;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.repository.LessonRepository;
import com.jlpp.modules.learningpath.dto.LearningPathProgressDto;
import com.jlpp.modules.learningpath.dto.LearningPathResponse;
import com.jlpp.modules.learningpath.entity.LearningPath;
import com.jlpp.modules.learningpath.repository.LearningPathRepository;
import com.jlpp.modules.learningpath.service.LearningPathService;
import com.jlpp.modules.learningpath.service.LessonProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class LearningPathLegacyAndConcurrencyRegressionTest {

    @Autowired
    private LessonProgressService lessonProgressService;

    @Autowired
    private LearningPathService learningPathService;

    @Autowired
    private LearningPathRepository learningPathRepository;

    @Autowired
    private CourseRegistrationRepository courseRegistrationRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User testUser;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        learningPathRepository.deleteAll();
        courseRegistrationRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM lessons WHERE course_id = 9999");
        courseRepository.findById(9999L).ifPresent(courseRepository::delete);
        userRepository.findByEmailIgnoreCase("defect_learner@jeps.edu.vn").ifPresent(userRepository::delete);

        testUser = userRepository.save(User.builder()
                .email("defect_learner@jeps.edu.vn")
                .username("defect_learner")
                .fullName("Defect Test Learner")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .targetJlptLevel("N3")
                .build());

        testCourse = courseRepository.save(Course.builder()
                .id(9999L)
                .title("Khóa học 10 bài kiểm thử khuyết tật")
                .code("JLPT-N3-REGRESSION")
                .level("N3")
                .price(0.0)
                .status("PUBLISHED")
                .durationHours(20)
                .lessonCount(10)
                .build());

        for (int i = 1; i <= 10; i++) {
            jdbcTemplate.update(
                    "INSERT INTO lessons (id, course_id, title, order_index, section_name, duration_minutes, lesson_type, skill_type) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    99900L + i,
                    testCourse.getId(),
                    "Bài " + i + ": Nội dung bài " + i,
                    i,
                    "Chương 1",
                    30,
                    "VIDEO",
                    (i % 2 == 0 ? "GRAMMAR" : "VOCABULARY")
            );
        }

        courseRegistrationRepository.save(CourseRegistration.builder()
                .user(testUser)
                .course(testCourse)
                .registeredAt(LocalDateTime.now().minusDays(1))
                .status("ACTIVE")
                .progressPercent(0)
                .build());

        learningPathRepository.save(LearningPath.builder()
                .userId(testUser.getId())
                .courseId(testCourse.getId())
                .dailyHoursSetup(BigDecimal.valueOf(1.5))
                .targetLevel("N3")
                .currentLevel("N4")
                .pathStatus("ACTIVE")
                .scheduledDate(LocalDate.now().minusDays(1))
                .build());
    }

    @Test
    @DisplayName("Critical Issue 1 Reproduction: Completing Lesson 6 out-of-order must NOT mark Lesson 1 as completed")
    void testOutOfOrderCompletionDoesNotContaminateEarlierLessons() {
        // Step 1: Initial state - 0 lessons completed, progress = 0%
        LearningPathProgressDto initialDto = lessonProgressService.getLessonProgress(testUser.getId(), testCourse.getId());
        assertThat(initialDto.isLessonCompleted(99901L)).isFalse();
        assertThat(initialDto.isLessonCompleted(99906L)).isFalse();

        // Step 2: Learner completes Lesson 6 (ID: 99906L, order_index: 6)
        lessonProgressService.markLessonCompleted(testUser.getId(), testCourse.getId(), 99906L);

        // Step 3: Verify JSONB contains ONLY Lesson 6
        LearningPathProgressDto afterStep2 = lessonProgressService.getLessonProgress(testUser.getId(), testCourse.getId());
        assertThat(afterStep2.isLessonCompleted(99906L)).isTrue();
        assertThat(afterStep2.isLessonCompleted(99901L)).isFalse();

        // Step 4: Verify course registration progress_percent reflects exactly 1 lesson (10%)
        CourseRegistration reg = courseRegistrationRepository.findByUserIdAndCourseId(testUser.getId(), testCourse.getId()).orElseThrow();
        assertThat(reg.getProgressPercent()).isEqualTo(10);

        // Step 5: Check whether Lesson 1 is considered completed by isLessonCompleted
        // DEFECT: If legacy fallback erroneously uses current progress_percent (10%), it infers Lesson 1 is completed!
        boolean lesson1Completed = lessonProgressService.isLessonCompleted(
                testUser.getId(), testCourse.getId(), 99901L, 1, 10, reg.getProgressPercent());

        assertThat(lesson1Completed)
                .as("Lesson 1 must NOT be marked completed when user only completed Lesson 6 out-of-order")
                .isFalse();

        // Step 6: Verify in Learning Path response: completed count must be 1, NOT 2
        LearningPathResponse lpResponse = learningPathService.getLearningPath("defect_learner@jeps.edu.vn");
        assertThat(lpResponse.getCompletedLessons()).isEqualTo(1);
        assertThat(lpResponse.getRemainingLessons()).isEqualTo(9);

        // Timeline check: Lesson 1 (order_index 1) must NOT be COMPLETED
        assertThat(lpResponse.getItems().get(0).getStatus()).isNotEqualTo("COMPLETED");
        assertThat(lpResponse.getItems().get(0).getActionText()).isNotEqualTo("Ôn lại");

        // Lesson 6 (order_index 6) MUST be COMPLETED
        assertThat(lpResponse.getItems().get(5).getStatus()).isEqualTo("COMPLETED");
        assertThat(lpResponse.getItems().get(5).getActionText()).isEqualTo("Ôn lại");
    }

    @Test
    @DisplayName("Critical Issue 2: Concurrent Complete Lesson requests must both be saved without lost updates")
    void testConcurrentCompletionUpdates() throws InterruptedException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        // Task A: completes Lesson 2 (ID: 99902L)
        executor.submit(() -> {
            try {
                startGate.await();
                lessonProgressService.markLessonCompleted(testUser.getId(), testCourse.getId(), 99902L);
                successCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endGate.countDown();
            }
        });

        // Task B: completes Lesson 3 (ID: 99903L)
        executor.submit(() -> {
            try {
                startGate.await();
                lessonProgressService.markLessonCompleted(testUser.getId(), testCourse.getId(), 99903L);
                successCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(2);

        // Verify JSONB contains BOTH Lesson 2 and Lesson 3
        LearningPathProgressDto finalProgress = lessonProgressService.getLessonProgress(testUser.getId(), testCourse.getId());
        assertThat(finalProgress.isLessonCompleted(99902L))
                .as("Lesson 2 must be recorded in final JSONB")
                .isTrue();
        assertThat(finalProgress.isLessonCompleted(99903L))
                .as("Lesson 3 must be recorded in final JSONB")
                .isTrue();

        // Verify aggregate progress reflects both (2 of 10 = 20%)
        CourseRegistration reg = courseRegistrationRepository.findByUserIdAndCourseId(testUser.getId(), testCourse.getId()).orElseThrow();
        assertThat(reg.getProgressPercent()).isEqualTo(20);
    }

    @Test
    @DisplayName("Critical Issue 1 & 5: Preexisting 50% legacy progress is preserved without inventing historical timestamps")
    void testPreservePreexistingLegacyProgressWithoutInventingTimestamps() {
        // Setup a legacy learner who already had 50% course progress before migration
        User legacyUser = userRepository.save(User.builder()
                .email("legacy_learner@jeps.edu.vn")
                .username("legacy_learner")
                .fullName("Legacy Learner")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .targetJlptLevel("N3")
                .build());

        CourseRegistration reg = courseRegistrationRepository.save(CourseRegistration.builder()
                .user(legacyUser)
                .course(testCourse)
                .registeredAt(LocalDateTime.now().minusDays(30))
                .status("ACTIVE")
                .progressPercent(50) // 5 of 10 lessons completed historically
                .build());

        learningPathRepository.save(LearningPath.builder()
                .userId(legacyUser.getId())
                .courseId(testCourse.getId())
                .scheduledDate(LocalDate.now())
                .dailyHoursSetup(BigDecimal.valueOf(2.0))
                .targetLevel("N3")
                .pathStatus("ACTIVE")
                .lessonProgress(null) // legacy row without jsonb progress yet
                .build());

        // Step 1: Query initial progress - Lessons 1..5 must be inferred completed, 6..10 not completed
        for (long i = 1; i <= 5; i++) {
            boolean isCompleted = lessonProgressService.isLessonCompleted(
                    legacyUser.getId(), testCourse.getId(), 99900L + i, (int) i, 10, 50);
            assertThat(isCompleted).as("Legacy lesson " + i + " must be completed").isTrue();
        }
        for (long i = 6; i <= 10; i++) {
            boolean isCompleted = lessonProgressService.isLessonCompleted(
                    legacyUser.getId(), testCourse.getId(), 99900L + i, (int) i, 10, 50);
            assertThat(isCompleted).as("Unfinished lesson " + i + " must NOT be completed").isFalse();
        }

        // Verify JSONB contains NO invented timestamps for lessons 1..5
        LearningPathProgressDto initialDto = lessonProgressService.getLessonProgress(legacyUser.getId(), testCourse.getId());
        assertThat(initialDto.getLessons()).isEmpty();
        assertThat(initialDto.getLegacyInferredCount()).isEqualTo(5);

        // Step 2: Legacy learner completes Lesson 7 (ID: 99907L)
        lessonProgressService.markLessonCompleted(legacyUser.getId(), testCourse.getId(), 99907L);

        // Step 3: Verify new state
        // - Lessons 1..5 still completed (via frozen legacy count)
        // - Lesson 6 NOT completed
        // - Lesson 7 COMPLETED (via verified JSONB)
        // - Lesson 8..10 NOT completed
        // Total completed = 6 of 10 (60%)
        LearningPathProgressDto afterStep2 = lessonProgressService.getLessonProgress(legacyUser.getId(), testCourse.getId());
        assertThat(afterStep2.isLessonCompleted(99907L)).isTrue();
        assertThat(afterStep2.getLessonProgress(99907L).getCompletedAt()).isNotNull();

        // Ensure lessons 1..5 do NOT have fake timestamps in JSONB
        assertThat(afterStep2.getLessons()).hasSize(1);
        assertThat(afterStep2.getLessons().containsKey("99907")).isTrue();

        // Verify CourseRegistration progressPercent updated accurately to 60%
        CourseRegistration updatedReg = courseRegistrationRepository.findByUserIdAndCourseId(legacyUser.getId(), testCourse.getId()).orElseThrow();
        assertThat(updatedReg.getProgressPercent()).isEqualTo(60);

        // Verify isLessonCompleted checks
        for (long i = 1; i <= 5; i++) {
            assertThat(lessonProgressService.isLessonCompleted(legacyUser.getId(), testCourse.getId(), 99900L + i, (int) i, 10, 60))
                    .as("Lesson " + i + " must remain completed via frozen legacy count").isTrue();
        }
        assertThat(lessonProgressService.isLessonCompleted(legacyUser.getId(), testCourse.getId(), 99906L, 6, 10, 60))
                .as("Lesson 6 must remain uncompleted").isFalse();
        assertThat(lessonProgressService.isLessonCompleted(legacyUser.getId(), testCourse.getId(), 99907L, 7, 10, 60))
                .as("Lesson 7 must be completed via JSONB").isTrue();
    }

    @Test
    @DisplayName("Critical Issue 3: Recommended duration is non-prescriptive and does NOT auto-complete activities")
    void testRecommendedDurationDoesNotMarkCompleted() {
        // Query learning path for user
        LearningPathResponse response = learningPathService.getLearningPath("defect_learner@jeps.edu.vn");

        // Lessons have base duration 30 minutes
        // Lesson with WEAK skill has recommended duration 45m (30 * 1.5)
        // Lesson with STRONG skill has recommended duration 23m (30 * 0.75)
        // But regardless of recommended duration, lesson status must NOT automatically become COMPLETED!
        response.getItems().forEach(item -> {
            if (!item.getId().equals(99906L)) {
                // If not marked completed via Course action, it must NOT be COMPLETED
                assertThat(item.getStatus()).isNotEqualTo("COMPLETED");
                assertThat(item.getActionText()).isNotEqualTo("Ôn lại");
            }
        });
    }

    @Test
    @DisplayName("Case 1: Legacy Snapshot Timing with 0-based, 1-based, and non-contiguous order_index")
    void testCase1_LegacySnapshotTiming_AllOrderIndexVariants() {
        // Test sub-case A: 0-based order_index (0..9)
        Course zeroBasedCourse = courseRepository.save(Course.builder()
                .id(8881L)
                .title("Zero-Based Course")
                .level("N3")
                .price(0.0)
                .status("PUBLISHED")
                .build());
        for (int i = 0; i < 10; i++) {
            jdbcTemplate.update(
                    "INSERT INTO lessons (id, course_id, title, order_index, section_name, duration_minutes, lesson_type, skill_type) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    88100L + i,
                    zeroBasedCourse.getId(),
                    "Lesson " + i,
                    i, // 0-based: 0, 1, 2, ..., 9
                    "Chương 1",
                    30,
                    "VIDEO",
                    "VOCABULARY"
            );
        }
        List<Lesson> zeroLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(zeroBasedCourse.getId());
        Lesson zeroLesson6 = zeroLessons.get(5); // 6th lesson (index 5)

        User userA = userRepository.save(User.builder()
                .email("user_zero_based@jeps.edu.vn")
                .username("user_zero_based")
                .fullName("User Zero Based")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build());

        courseRegistrationRepository.save(CourseRegistration.builder()
                .user(userA)
                .course(zeroBasedCourse)
                .registeredAt(LocalDateTime.now())
                .status("ACTIVE")
                .progressPercent(50) // 5 of 10 completed historically
                .build());

        learningPathRepository.save(LearningPath.builder()
                .userId(userA.getId())
                .courseId(zeroBasedCourse.getId())
                .scheduledDate(LocalDate.now())
                .dailyHoursSetup(BigDecimal.valueOf(1.5))
                .targetLevel("N3")
                .pathStatus("ACTIVE")
                .lessonProgress(null)
                .build());

        // Complete 6th lesson
        lessonProgressService.markLessonCompleted(userA.getId(), zeroBasedCourse.getId(), zeroLesson6.getId());

        // Assertions for Case 1:
        LearningPathProgressDto dtoA = lessonProgressService.getLessonProgress(userA.getId(), zeroBasedCourse.getId());
        // 1. legacyInferredCount frozen as 5
        assertThat(dtoA.getLegacyInferredCount()).isEqualTo(5);
        // 2. Lesson 6 recorded as verified JSONB completion
        assertThat(dtoA.isLessonCompleted(zeroLesson6.getId())).isTrue();
        // 3. Course progress becomes 60% (5 legacy + 1 verified)
        CourseRegistration regA = courseRegistrationRepository.findByUserIdAndCourseId(userA.getId(), zeroBasedCourse.getId()).orElseThrow();
        assertThat(regA.getProgressPercent()).isEqualTo(60);
        // 4. Lessons 1-5 remain legacy inferred only (no fake timestamps in JSONB)
        assertThat(dtoA.getLessons()).hasSize(1);
        for (int i = 0; i < 5; i++) {
            assertThat(dtoA.getLessons().containsKey(String.valueOf(zeroLessons.get(i).getId()))).isFalse();
            assertThat(lessonProgressService.isLessonCompleted(userA.getId(), zeroBasedCourse.getId(), zeroLessons.get(i).getId(), i + 1, 10, 60))
                    .as("Legacy lesson " + i + " must be completed").isTrue();
        }
        // 5. Lesson 6 counted exactly once
        // 6. Lessons 7-10 remain incomplete
        for (int i = 6; i < 10; i++) {
            assertThat(lessonProgressService.isLessonCompleted(userA.getId(), zeroBasedCourse.getId(), zeroLessons.get(i).getId(), i + 1, 10, 60))
                    .as("Lesson " + i + " must remain incomplete").isFalse();
        }
        // 7. No fabricated completedAt timestamps created for legacy lessons
        assertThat(dtoA.getLessonProgress(zeroLessons.get(0).getId())).isNull();

        // Test sub-case B: Non-contiguous order_index (10, 20, 30, ..., 100)
        Course stepCourse = courseRepository.save(Course.builder()
                .id(8882L)
                .title("Step-10 Course")
                .level("N3")
                .price(0.0)
                .status("PUBLISHED")
                .build());
        for (int i = 1; i <= 10; i++) {
            jdbcTemplate.update(
                    "INSERT INTO lessons (id, course_id, title, order_index, section_name, duration_minutes, lesson_type, skill_type) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    88200L + i,
                    stepCourse.getId(),
                    "Step Lesson " + i,
                    i * 10, // Non-contiguous: 10, 20, 30, ..., 100
                    "Chương 1",
                    30,
                    "VIDEO",
                    "GRAMMAR"
            );
        }
        List<Lesson> stepLessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(stepCourse.getId());
        Lesson stepLesson6 = stepLessons.get(5); // 6th lesson

        User userB = userRepository.save(User.builder()
                .email("user_step_based@jeps.edu.vn")
                .username("user_step_based")
                .fullName("User Step Based")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build());

        courseRegistrationRepository.save(CourseRegistration.builder()
                .user(userB)
                .course(stepCourse)
                .registeredAt(LocalDateTime.now())
                .status("ACTIVE")
                .progressPercent(50)
                .build());

        learningPathRepository.save(LearningPath.builder()
                .userId(userB.getId())
                .courseId(stepCourse.getId())
                .scheduledDate(LocalDate.now())
                .dailyHoursSetup(BigDecimal.valueOf(1.5))
                .targetLevel("N3")
                .pathStatus("ACTIVE")
                .lessonProgress(null)
                .build());

        // Complete 6th lesson
        lessonProgressService.markLessonCompleted(userB.getId(), stepCourse.getId(), stepLesson6.getId());

        LearningPathProgressDto dtoB = lessonProgressService.getLessonProgress(userB.getId(), stepCourse.getId());
        assertThat(dtoB.getLegacyInferredCount()).isEqualTo(5);
        CourseRegistration regB = courseRegistrationRepository.findByUserIdAndCourseId(userB.getId(), stepCourse.getId()).orElseThrow();
        assertThat(regB.getProgressPercent()).isEqualTo(60);
    }

    @Test
    @DisplayName("Case 2: Concurrent Auto-Provisioning - No duplicate LearningPath, both completions preserved")
    void testCase2_ConcurrentAutoProvisioning_NoDuplicateLearningPath() throws InterruptedException {
        // Setup learner registered for course, but NO LearningPath entity created yet!
        User autoUser = userRepository.save(User.builder()
                .email("auto_provision_user@jeps.edu.vn")
                .username("auto_provision_user")
                .fullName("Auto Provision User")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build());

        CourseRegistration autoReg = courseRegistrationRepository.save(CourseRegistration.builder()
                .user(autoUser)
                .course(testCourse)
                .registeredAt(LocalDateTime.now())
                .status("ACTIVE")
                .progressPercent(0)
                .build());

        // Confirm learning_paths has NO record for this user/course
        assertThat(learningPathRepository.findByUserIdAndCourseId(autoUser.getId(), testCourse.getId())).isEmpty();

        // 2 concurrent threads complete Lesson 2 (ID: 99902) and Lesson 3 (ID: 99903)
        Long lesson2Id = 99902L;
        Long lesson3Id = 99903L;

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        executor.submit(() -> {
            try {
                startLatch.await();
                lessonProgressService.markLessonCompleted(autoUser.getId(), testCourse.getId(), lesson2Id);
                successCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                lessonProgressService.markLessonCompleted(autoUser.getId(), testCourse.getId(), lesson3Id);
                successCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(2);

        // Verification 1: Exactly ONE LearningPath record created in DB (no duplicates!)
        List<LearningPath> paths = learningPathRepository.findByUserIdAndCourseId(autoUser.getId(), testCourse.getId());
        assertThat(paths).as("Must auto-provision exactly 1 LearningPath record without duplicates").hasSize(1);

        // Verification 2 & 3: Both completions preserved in JSONB
        LearningPathProgressDto progressDto = lessonProgressService.getLessonProgress(autoUser.getId(), testCourse.getId());
        assertThat(progressDto.isLessonCompleted(lesson2Id)).as("Lesson 2 must be recorded").isTrue();
        assertThat(progressDto.isLessonCompleted(lesson3Id)).as("Lesson 3 must be recorded").isTrue();

        // Verification 4: Aggregate progress percent updated accurately to 20% (2 of 10)
        CourseRegistration updatedReg = courseRegistrationRepository.findByUserIdAndCourseId(autoUser.getId(), testCourse.getId()).orElseThrow();
        assertThat(updatedReg.getProgressPercent()).isEqualTo(20);
    }

    @Test
    @DisplayName("Case 4: Manager Reordering - Verified JSONB IDs invariant, Legacy positional inference documented")
    void testCase4_ManagerReordering_VerifiedLessonIdsInvariant() {
        // Setup course with 10 lessons
        Course reorderCourse = courseRepository.save(Course.builder()
                .id(8883L)
                .title("Reorder Test Course")
                .level("N3")
                .price(0.0)
                .status("PUBLISHED")
                .build());
        for (int i = 1; i <= 10; i++) {
            jdbcTemplate.update(
                    "INSERT INTO lessons (id, course_id, title, order_index, section_name, duration_minutes, lesson_type, skill_type) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    88300L + i,
                    reorderCourse.getId(),
                    "Reorder Lesson " + i,
                    i,
                    "Chương 1",
                    30,
                    "VIDEO",
                    "READING"
            );
        }

        User reorderUser = userRepository.save(User.builder()
                .email("reorder_user@jeps.edu.vn")
                .username("reorder_user")
                .fullName("Reorder User")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build());

        courseRegistrationRepository.save(CourseRegistration.builder()
                .user(reorderUser)
                .course(reorderCourse)
                .registeredAt(LocalDateTime.now())
                .status("ACTIVE")
                .progressPercent(50) // 5 of 10 completed historically
                .build());

        learningPathRepository.save(LearningPath.builder()
                .userId(reorderUser.getId())
                .courseId(reorderCourse.getId())
                .scheduledDate(LocalDate.now())
                .dailyHoursSetup(BigDecimal.valueOf(1.5))
                .targetLevel("N3")
                .pathStatus("ACTIVE")
                .lessonProgress(null)
                .build());

        List<Lesson> lessonsBefore = lessonRepository.findByCourseIdOrderByOrderIndexAsc(reorderCourse.getId());
        Lesson lesson1 = lessonsBefore.get(0);
        Lesson lesson6 = lessonsBefore.get(5);
        Lesson lesson10 = lessonsBefore.get(9);

        // Learner completes Lesson 6
        lessonProgressService.markLessonCompleted(reorderUser.getId(), reorderCourse.getId(), lesson6.getId());

        LearningPathProgressDto dtoBefore = lessonProgressService.getLessonProgress(reorderUser.getId(), reorderCourse.getId());
        String originalCompletedAt = dtoBefore.getLessonProgress(lesson6.getId()).getCompletedAt();
        assertThat(originalCompletedAt).isNotNull();

        // Manager reorders: swaps Lesson 10 into slot 1 (orderIndex = 1) and Lesson 1 into slot 10 (orderIndex = 10)
        jdbcTemplate.update("UPDATE lessons SET order_index = 10 WHERE id = ?", lesson1.getId());
        jdbcTemplate.update("UPDATE lessons SET order_index = 1 WHERE id = ?", lesson10.getId());

        // Verify:
        // 1. Verified Lesson 6 ID is INVARIANT: still completed with exact same timestamp
        LearningPathProgressDto dtoAfter = lessonProgressService.getLessonProgress(reorderUser.getId(), reorderCourse.getId());
        assertThat(dtoAfter.isLessonCompleted(lesson6.getId())).isTrue();
        assertThat(dtoAfter.getLessonProgress(lesson6.getId()).getCompletedAt()).isEqualTo(originalCompletedAt);

        // 2. Legacy-inferred completion is POSITIONAL:
        // Lesson 10 is now at rank 1, so it is inferred completed
        // Lesson 1 is now at rank 10, so it is NO LONGER inferred completed
        boolean isLesson10Done = lessonProgressService.isLessonCompleted(reorderUser.getId(), reorderCourse.getId(), lesson10.getId(), 1, 10, 60);
        boolean isLesson1Done = lessonProgressService.isLessonCompleted(reorderUser.getId(), reorderCourse.getId(), lesson1.getId(), 10, 10, 60);

        assertThat(isLesson10Done).as("Lesson 10 moved to slot 1 is positional inferred").isTrue();
        assertThat(isLesson1Done).as("Lesson 1 moved to slot 10 is no longer positional inferred").isFalse();
    }
}
