package com.jeps.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nền chung cho integration test của Report 5.2 (Course / Lesson / Learning Path).
 *
 * <ul>
 *   <li>Chạy toàn bộ ứng dụng (@SpringBootTest + MockMvc) trên PostgreSQL cục bộ {@code jeps_test}.</li>
 *   <li>Mỗi test chạy trong một transaction và được rollback khi kết thúc, nên không để lại dữ liệu.</li>
 *   <li>Datasource luôn bị ép về {@code jeps_test}; nếu máy không có database này, test được bỏ qua.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
@EnabledIf(value = "com.jeps.support.LocalTestDatabase#isAvailable",
        disabledReason = "Không có PostgreSQL cục bộ jeps_test (.env.local) – bỏ qua integration test")
public abstract class LocalPostgresIntegrationTest {

    protected static final Path UPLOAD_DIR = Path.of(System.getProperty("java.io.tmpdir"), "jeps-it-uploads");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @jakarta.persistence.PersistenceContext
    protected jakarta.persistence.EntityManager entityManager;

    /**
     * Test chạy trong một transaction nên thay đổi qua JPA có thể còn nằm trong persistence context.
     * Gọi trước khi đọc lại database bằng JDBC để thấy đúng dữ liệu đã ghi.
     */
    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @DynamicPropertySource
    static void localDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", LocalTestDatabase::url);
        registry.add("spring.datasource.username", LocalTestDatabase::username);
        registry.add("spring.datasource.password", LocalTestDatabase::password);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("app.upload-dir", UPLOAD_DIR::toString);
    }

    @BeforeEach
    void guardDatabase() {
        assertThat(jdbc.queryForObject("SELECT current_database()", String.class))
                .as("Integration test chỉ được chạy trên database jeps_test")
                .isEqualTo("jeps_test");
    }
}
