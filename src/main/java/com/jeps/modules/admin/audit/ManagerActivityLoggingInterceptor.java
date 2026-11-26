package com.jeps.modules.admin.audit;

import com.jeps.common.util.CurrentUserSession;
import com.jeps.modules.admin.entity.AdminSystemLog;
import com.jeps.modules.admin.repository.AdminSystemLogRepository;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class ManagerActivityLoggingInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Set<String> CREATE_SUFFIXES = Set.of(
            "/sets",
            "/sections",
            "/duplicate",
            "/import",
            "/questions/add",
            "/questions/generate",
            "/questions/generate/confirm"
    );
    private static final List<DetailField> DETAIL_FIELDS = List.of(
            new DetailField("name", "tên", true),
            new DetailField("title", "tiêu đề", true),
            new DetailField("code", "mã", true),
            new DetailField("level", "cấp độ", true),
            new DetailField("jlptLevel", "cấp độ JLPT", true),
            new DetailField("status", "trạng thái", true),
            new DetailField("sectionName", "chương", true),
            new DetailField("newName", "tên mới", true),
            new DetailField("skillType", "kỹ năng", true),
            new DetailField("questionType", "loại câu hỏi", true),
            new DetailField("examType", "loại đề", true),
            new DetailField("price", "giá", true),
            new DetailField("discountPrice", "giá ưu đãi", true),
            new DetailField("description", "trường mô tả", false),
            new DetailField("shortDescription", "trường mô tả ngắn", false),
            new DetailField("objectives", "trường mục tiêu", false),
            new DetailField("prerequisites", "trường điều kiện", false),
            new DetailField("targetAudience", "trường đối tượng", false),
            new DetailField("content", "trường nội dung", false)
    );
    private static final int MAX_DETAILS_LENGTH = 255;
    private static final int MAX_FIELD_VALUE_LENGTH = 40;
    private static final int MAX_SUMMARY_FIELDS = 4;

    private final ObjectProvider<AdminSystemLogRepository> systemLogRepositoryProvider;
    private final ObjectProvider<UserRepository> userRepositoryProvider;

    public ManagerActivityLoggingInterceptor(
            ObjectProvider<AdminSystemLogRepository> systemLogRepositoryProvider,
            ObjectProvider<UserRepository> userRepositoryProvider) {
        this.systemLogRepositoryProvider = systemLogRepositoryProvider;
        this.userRepositoryProvider = userRepositoryProvider;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        String method = request.getMethod();
        if (!MUTATING_METHODS.contains(method) || !isManager()) {
            return;
        }

        AdminSystemLogRepository systemLogRepository = systemLogRepositoryProvider.getIfAvailable();
        if (systemLogRepository == null) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AdminSystemLog log = new AdminSystemLog();
        log.setUserId(resolveUserId(authentication, userRepositoryProvider.getIfAvailable(), request));
        log.setLogType("MANAGER");
        log.setActionPerformed(actionFor(method, applicationPath(request)));
        log.setIpAddress(request.getRemoteAddr());
        log.setTimestamp(Instant.now());
        log.setDetails(detailsFor(method, applicationPath(request), request));
        systemLogRepository.save(log);
    }

    private boolean isManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_MANAGER".equals(authority.getAuthority()));
    }

    private String applicationPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasLength(contextPath) && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

    private String actionFor(String method, String path) {
        String normalizedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        if ("DELETE".equals(method) || normalizedPath.endsWith("/delete")
                || normalizedPath.endsWith("/remove")) {
            return "MANAGER_DELETE";
        }
        if (isCreatePath(normalizedPath)) {
            return "MANAGER_CREATE";
        }
        return "MANAGER_UPDATE";
    }

    private boolean isCreatePath(String path) {
        return Set.of(
                "/manager/courses",
                "/manager/lessons",
                "/manager/questions",
                "/manager/quizzes",
                "/manager/exams",
                "/manager/flashcards",
                "/admin/courses",
                "/admin/lessons",
                "/admin/questions",
                "/admin/quizzes",
                "/admin/exams",
                "/admin/flashcards"
        ).contains(path) || CREATE_SUFFIXES.stream().anyMatch(path::endsWith);
    }

    private String detailsFor(String method, String path, HttpServletRequest request) {
        String verb = operationLabel(method, path);
        String resource = resourceLabel(path);
        String target = targetReference(path, request);
        String fields = parameterSummary(path, request);
        return truncate(verb + " " + resource + target + fields);
    }

    private String operationLabel(String method, String path) {
        if (path.contains("/bulk-status")) {
            return "cập nhật trạng thái hàng loạt";
        }
        if (path.endsWith("/duplicate")) {
            return "nhân bản";
        }
        if (path.contains("/questions/add")) {
            return "thêm câu hỏi vào";
        }
        if (path.contains("/questions/remove")) {
            return "gỡ câu hỏi khỏi";
        }
        if (path.contains("/questions/generate")) {
            return "tạo câu hỏi cho";
        }
        if (path.endsWith("/import")) {
            return "nhập dữ liệu vào";
        }
        if (path.endsWith("/reorder")) {
            return "sắp xếp lại";
        }
        if (path.endsWith("/move")) {
            return "di chuyển";
        }
        if (path.endsWith("/status")) {
            return "cập nhật trạng thái";
        }
        if ("DELETE".equals(method) || path.endsWith("/delete") || path.endsWith("/remove")) {
            return "xóa";
        }
        return switch (actionFor(method, path)) {
            case "MANAGER_CREATE" -> "tạo mới";
            case "MANAGER_DELETE" -> "xóa";
            default -> "cập nhật";
        };
    }

    private String resourceLabel(String path) {
        if (path.startsWith("/manager/courses") || path.startsWith("/admin/courses")) {
            return "khóa học";
        }
        if (path.startsWith("/manager/lessons") || path.startsWith("/admin/lessons")) {
            return path.contains("/sections") ? "chương học" : "bài học";
        }
        if (path.startsWith("/manager/questions") || path.startsWith("/admin/questions")) {
            return "câu hỏi";
        }
        if (path.startsWith("/manager/quizzes") || path.startsWith("/admin/quizzes")) {
            return "quiz";
        }
        if (path.startsWith("/manager/exams") || path.startsWith("/admin/exams")) {
            return "đề thi";
        }
        if (path.startsWith("/manager/flashcards") || path.startsWith("/admin/flashcards")) {
            return path.contains("/sets") ? "bộ thẻ ghi nhớ" : "thẻ ghi nhớ";
        }
        if (path.startsWith("/admin/reports")) {
            return "báo cáo";
        }
        if (path.startsWith("/profile")) {
            return "hồ sơ";
        }
        return "thao tác";
    }

    private String targetReference(String path, HttpServletRequest request) {
        String[] segments = path.split("/");
        for (int index = 1; index < segments.length - 1; index++) {
            if (Set.of("courses", "lessons", "questions", "quizzes", "exams", "flashcards")
                    .contains(segments[index]) && segments[index + 1].matches("\\d+")) {
                return " #" + segments[index + 1];
            }
        }

        String courseId = request.getParameter("courseId");
        return StringUtils.hasText(courseId) && courseId.matches("\\d+")
                ? " của khóa học #" + courseId
                : "";
    }

    private String parameterSummary(String path, HttpServletRequest request) {
        if (!path.startsWith("/manager/") && !path.startsWith("/admin/courses/")
                && !path.startsWith("/admin/lessons/") && !path.startsWith("/admin/questions/")
                && !path.startsWith("/admin/quizzes/") && !path.startsWith("/admin/exams/")
                && !path.startsWith("/admin/flashcards/")) {
            return "";
        }

        List<String> fields = new ArrayList<>();
        for (DetailField field : DETAIL_FIELDS) {
            String value = request.getParameter(field.parameter());
            if (!StringUtils.hasText(value)) {
                continue;
            }
            fields.add(field.includeValue()
                    ? field.label() + "=\"" + safeFieldValue(value) + "\""
                    : field.label());
            if (fields.size() == MAX_SUMMARY_FIELDS) {
                break;
            }
        }

        String[] selectedIds = request.getParameterValues("ids");
        if (selectedIds != null && selectedIds.length > 0 && fields.size() < MAX_SUMMARY_FIELDS) {
            fields.add("số mục=" + selectedIds.length);
        }
        return fields.isEmpty() ? "" : " — " + String.join(", ", fields);
    }

    private String safeFieldValue(String value) {
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").replaceAll("\\s{2,}", " ").trim();
        if (normalized.length() <= MAX_FIELD_VALUE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_FIELD_VALUE_LENGTH - 3) + "...";
    }

    private Long resolveUserId(
            Authentication authentication,
            UserRepository userRepository,
            HttpServletRequest request) {
        Long userId = CurrentUserSession.resolveAuthenticatedUserId(authentication);
        if (userId != null) {
            return userId;
        }

        if (userRepository != null && authentication != null && StringUtils.hasText(authentication.getName())) {
            String identifier = authentication.getName().trim();
            User user = userRepository.findByEmailIgnoreCase(identifier)
                    .or(() -> userRepository.findByUsername(identifier))
                    .orElse(null);
            if (user != null) {
                return user.getId();
            }
        }

        HttpSession session = request.getSession(false);
        Object sessionUserId = session == null ? null : session.getAttribute("CURRENT_LEARNER_ID");
        return sessionUserId instanceof Number number ? number.longValue() : null;
    }

    private String truncate(String value) {
        return value.length() <= MAX_DETAILS_LENGTH ? value : value.substring(0, MAX_DETAILS_LENGTH);
    }

    private record DetailField(String parameter, String label, boolean includeValue) {
    }
}
