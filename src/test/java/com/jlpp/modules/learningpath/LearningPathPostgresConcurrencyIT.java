package com.jlpp.modules.learningpath;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.entity.CourseRegistration;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.learningpath.dto.LearningPathProgressDto;
import com.jlpp.modules.learningpath.entity.LearningPath;
import com.jlpp.modules.learningpath.repository.LearningPathRepository;
import com.jlpp.modules.learningpath.service.LessonProgressService;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0",
        "spring.datasource.username=postgres.eztcijnvnmcievogjufk",
        "spring.datasource.password=Cme85Dpt1b2HwrBQ",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=update"
})
public class LearningPathPostgresConcurrencyIT {

    @Autowired
    private LessonProgressService lessonProgressService;

    @Autowired
    private LearningPathRepository learningPathRepository;

    @Autowired
    private CourseRegistrationRepository courseRegistrationRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Case 3: PostgreSQL Locking - Row-level pessimistic locking serializes concurrent completions on PostgreSQL")
    void testPostgreSqlPessimisticRowLocking() throws InterruptedException {
        // 1. Verify PostgreSQL dialect and connectivity
        String dbVersion = jdbcTemplate.queryForObject("SELECT version()", String.class);
        System.out.println(">>> Verified PostgreSQL Database Engine: " + dbVersion);
        assertThat(dbVersion).containsIgnoringCase("PostgreSQL");

        List<String> courseCols = jdbcTemplate.query(
                "SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'courses' ORDER BY ordinal_position",
                (rs, rowNum) -> rs.getString("column_name") + " (" + rs.getString("data_type") + ")"
        );
        System.out.println(">>> Courses columns in PostgreSQL: " + courseCols);

        // Find an existing published course in PostgreSQL
        Long existingCourseId = jdbcTemplate.queryForObject(
                "SELECT id FROM courses WHERE status = 'PUBLISHED' LIMIT 1",
                Long.class
        );
        System.out.println(">>> Existing published course ID in PostgreSQL: " + existingCourseId);

        Long testCourseId = existingCourseId;
        List<Long> lessonIds = jdbcTemplate.queryForList(
                "SELECT id FROM lessons WHERE course_id = ? ORDER BY order_index ASC",
                Long.class,
                testCourseId
        );
        System.out.println(">>> Course " + testCourseId + " has " + lessonIds.size() + " lessons: " + lessonIds);
        assertThat(lessonIds.size()).isGreaterThanOrEqualTo(2);

        Long lesson1Id = lessonIds.get(0);
        Long lesson2Id = lessonIds.get(1);

        userRepository.findByEmailIgnoreCase("pg_concurrency_learner@jeps.edu.vn").ifPresent(u -> {
            jdbcTemplate.update("DELETE FROM learning_paths WHERE user_id = ?", u.getId());
            jdbcTemplate.update("DELETE FROM course_registrations WHERE user_id = ?", u.getId());
            userRepository.delete(u);
        });

        User pgUser = userRepository.save(User.builder()
                .email("pg_concurrency_learner@jeps.edu.vn")
                .username("pg_concurrency_learner")
                .fullName("PG Concurrency Learner")
                .password(passwordEncoder.encode("Password123@"))
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build());
        Long testUserId = pgUser.getId();

        Course pgCourse = courseRepository.findById(testCourseId).orElseThrow();

        courseRegistrationRepository.save(CourseRegistration.builder()
                .user(pgUser)
                .course(pgCourse)
                .registeredAt(LocalDateTime.now())
                .status("ACTIVE")
                .progressPercent(0)
                .build());

        learningPathRepository.save(LearningPath.builder()
                .userId(testUserId)
                .courseId(testCourseId)
                .scheduledDate(LocalDate.now())
                .dailyHoursSetup(java.math.BigDecimal.valueOf(1.5))
                .targetLevel("N3")
                .pathStatus("ACTIVE")
                .lessonProgress(null)
                .build());

        // 3. Fire 2 concurrent completion requests on PostgreSQL
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCounter = new AtomicInteger(0);

        final Long finalUserId = testUserId;

        executor.submit(() -> {
            try {
                startLatch.await();
                lessonProgressService.markLessonCompleted(finalUserId, testCourseId, lesson1Id);
                successCounter.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                lessonProgressService.markLessonCompleted(finalUserId, testCourseId, lesson2Id);
                successCounter.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCounter.get()).isEqualTo(2);

        // 4. Verification directly against PostgreSQL database
        LearningPathProgressDto progressDto = lessonProgressService.getLessonProgress(testUserId, testCourseId);
        assertThat(progressDto.isLessonCompleted(lesson1Id))
                .as("Lesson " + lesson1Id + " must be recorded in PostgreSQL JSONB")
                .isTrue();
        assertThat(progressDto.isLessonCompleted(lesson2Id))
                .as("Lesson " + lesson2Id + " must be recorded in PostgreSQL JSONB")
                .isTrue();

        CourseRegistration reg = courseRegistrationRepository.findByUserIdAndCourseId(testUserId, testCourseId).orElseThrow();
        int expectedPercent = (int) Math.round((2.0 / lessonIds.size()) * 100);
        assertThat(reg.getProgressPercent())
                .as("Course registration progress in PostgreSQL must match expected: " + expectedPercent)
                .isEqualTo(expectedPercent);

        // Cleanup PostgreSQL test data
        jdbcTemplate.update("DELETE FROM learning_paths WHERE user_id = ?", testUserId);
        jdbcTemplate.update("DELETE FROM course_registrations WHERE user_id = ?", testUserId);
        userRepository.findById(testUserId).ifPresent(userRepository::delete);
    }
}

