package com.jlpp.modules.manager.reports.repository;

import com.jlpp.modules.manager.reports.dto.ManagerReportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ManagerReportRepositoryTest {

    private ManagerReportRepository reportRepository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:manager-reports;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP ALL OBJECTS");
        jdbcTemplate.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, full_name VARCHAR(100), username VARCHAR(50), email VARCHAR(150))");
        jdbcTemplate.execute("CREATE TABLE exams (exam_id BIGINT PRIMARY KEY, exam_title VARCHAR(200), jlpt_level VARCHAR(20))");
        jdbcTemplate.execute("""
                CREATE TABLE exam_results (
                    user_id BIGINT, exam_id BIGINT, total_score INTEGER, percentage DECIMAL(5, 2),
                    passed_status BOOLEAN, end_time TIMESTAMP
                )
                """);
        jdbcTemplate.execute("CREATE TABLE courses (course_id BIGINT PRIMARY KEY, course_title VARCHAR(200), jlpt_level VARCHAR(20))");
        jdbcTemplate.execute("""
                CREATE TABLE course_registrations (
                    user_id BIGINT, course_id BIGINT, progress_percent INTEGER, status VARCHAR(30), registered_at TIMESTAMP
                )
                """);
        jdbcTemplate.update("INSERT INTO users VALUES (1, 'Test Learner', 'learner', 'learner@example.com')");
        jdbcTemplate.update("INSERT INTO exams VALUES (2, 'N5 Mock Exam', 'N5')");
        jdbcTemplate.update("INSERT INTO exam_results VALUES (1, 2, 80, 85.50, TRUE, TIMESTAMP '2026-10-15 12:00:00')");
        jdbcTemplate.update("INSERT INTO courses VALUES (3, 'N5 Course', 'N5')");
        jdbcTemplate.update("INSERT INTO course_registrations VALUES (1, 3, 60, 'ACTIVE', TIMESTAMP '2026-10-20 12:00:00')");
        reportRepository = new ManagerReportRepository(jdbcTemplate);
    }

    @Test
    void findsExamResultsWithinDateRange() {
        List<List<String>> rows = reportRepository.findReportRows(
                ManagerReportType.EXAM_RESULTS, LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));

        assertThat(rows).containsExactly(List.of(
                "1", "Test Learner", "N5 Mock Exam", "N5", "80", "85.50", "Đạt", "15/10/2026 12:00"));
    }

    @Test
    void findsCourseEnrollmentsWithoutLeakingOutsideDateRange() {
        List<List<String>> rows = reportRepository.findReportRows(
                ManagerReportType.COURSE_ENROLLMENTS, null, LocalDate.parse("2026-10-19"));

        assertThat(rows).isEmpty();
    }

    @Test
    void findsCourseEnrollmentsWithinDateRange() {
        List<List<String>> rows = reportRepository.findReportRows(
                ManagerReportType.COURSE_ENROLLMENTS, LocalDate.parse("2026-10-20"), null);

        assertThat(rows).containsExactly(List.of(
                "1", "Test Learner", "N5 Course", "N5", "60", "ACTIVE", "20/10/2026 12:00"));
    }
}
