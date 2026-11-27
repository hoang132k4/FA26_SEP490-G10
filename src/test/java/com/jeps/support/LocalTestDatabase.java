package com.jeps.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Database PostgreSQL cục bộ {@code jeps_test} dùng cho integration test (Report 5.2).
 *
 * <p>Thông tin kết nối đọc từ file {@code .env.local} ở thư mục gốc dự án (đã nằm trong .gitignore).
 * Chỉ chấp nhận URL trỏ tới {@code localhost}/{@code 127.0.0.1} và database {@code jeps_test}, để integration test
 * không bao giờ chạy vào database Supabase (production). Không có database này thì test được bỏ qua.</p>
 */
public final class LocalTestDatabase {

    private static final Path ENV_FILE = Path.of(".env.local");
    private static final Pattern LOCAL_JEPS_TEST =
            Pattern.compile("^jdbc:postgresql://(localhost|127\\.0\\.0\\.1)(:\\d+)?/jeps_test(\\?.*)?$");

    private static Boolean available;

    private LocalTestDatabase() {
    }

    public static String url() {
        return properties().getProperty("spring.datasource.url");
    }

    public static String username() {
        return properties().getProperty("spring.datasource.username");
    }

    public static String password() {
        return properties().getProperty("spring.datasource.password", "");
    }

    /** Dùng cho {@code @EnabledIf("com.jeps.support.LocalTestDatabase#isAvailable")}. */
    public static synchronized boolean isAvailable() {
        if (available != null) return available;
        available = false;
        Properties props = properties();
        String url = props.getProperty("spring.datasource.url");
        if (url == null || !LOCAL_JEPS_TEST.matcher(url.trim()).matches()) return false;
        try {
            DriverManager.setLoginTimeout(3);
            try (Connection connection = DriverManager.getConnection(url.trim(), username(), password());
                 Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT current_database()")) {
                available = rs.next() && "jeps_test".equals(rs.getString(1));
            }
        } catch (Exception exception) {
            available = false;
        }
        return available;
    }

    private static Properties properties() {
        Properties props = new Properties();
        if (!Files.isRegularFile(ENV_FILE)) return props;
        try (InputStream in = Files.newInputStream(ENV_FILE)) {
            props.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // không đọc được file thì xem như không có database test
        }
        return props;
    }
}
