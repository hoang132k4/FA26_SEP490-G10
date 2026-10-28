package com.jlpp.modules.manager.courses.controller;

import com.jlpp.modules.manager.courses.dto.CourseOption;
import com.jlpp.modules.manager.courses.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/manager/courses")
public class ManagerCourseController {

    private static final Logger log = LoggerFactory.getLogger(ManagerCourseController.class);
    private final CourseService courseService;

    public ManagerCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public String courses(Model model) {
        List<Map<String, Object>> courses;
        try {
            courses = courseService.findCourses().stream().map(this::toCourseView).toList();
        } catch (DataAccessException | TransactionException exception) {
            courses = List.of();
            model.addAttribute("courseError", "Không kết nối được Supabase. Kiểm tra file .env và thông tin Session Pooler rồi tải lại.");
        }
        model.addAttribute("courses", courses);
        model.addAttribute("courseCount", courses.size());
        model.addAttribute("courseForm", model.containsAttribute("courseForm") ? model.getAttribute("courseForm") : null);
        return "pages/manager/course/manager-courses";
    }

    @GetMapping("/new")
    public String newCourse(Model model) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("id", ""); form.put("name", ""); form.put("level", "N5");
        form.put("price", "0"); form.put("description", ""); form.put("durationHours", "40");
        form.put("objectives", ""); form.put("prerequisites", ""); form.put("targetAudience", "");
        model.addAttribute("courseForm", form);
        return courses(model);
    }

    @GetMapping("/{courseId}/edit")
    public String editCourse(@PathVariable long courseId, Model model, RedirectAttributes redirectAttributes) {
        return courseService.findCourseDetails(courseId).map(course -> {
            Map<String, Object> form = new LinkedHashMap<>();
            form.put("id", courseId);
            form.put("name", getCourseValue(course, "course_title", "title"));
            form.put("level", getCourseValue(course, "jlpt_level", "level"));
            form.put("status", normalizedCourseStatus(String.valueOf(getCourseValue(course, "status"))));
            form.put("price", getCourseValue(course, "price"));
            form.put("description", getCourseValue(course, "description", "short_description", "full_description"));
            form.put("durationHours", getCourseValue(course, "duration_hours", "duration"));
            form.put("objectives", getCourseValue(course, "objectives"));
            form.put("prerequisites", getCourseValue(course, "prerequisites"));
            form.put("targetAudience", getCourseValue(course, "target_audience"));
            model.addAttribute("courseForm", form);
            return courses(model);
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("courseError", "Không tìm thấy khóa học cần sửa.");
            return "redirect:/manager/courses";
        });
    }

    @GetMapping("/{courseId}/view")
    public String viewCourse(@PathVariable long courseId, Model model, RedirectAttributes redirectAttributes) {
        return courseService.findCourseDetails(courseId).map(course -> {
            model.addAttribute("courseDetails", course);
            model.addAttribute("courseId", courseId);
            String currentStatus = normalizedCourseStatus(String.valueOf(getCourseValue(course, "status")))
                    .toUpperCase(Locale.ROOT);
            String nextStatus = switch (currentStatus) {
                case "PUBLISHED" -> "ARCHIVED";
                case "ARCHIVED" -> "DRAFT";
                default -> "PUBLISHED";
            };
            model.addAttribute("courseNextStatus", nextStatus);
            model.addAttribute("courseNextStatusLabel", switch (nextStatus) {
                case "ARCHIVED" -> "Archived";
                case "DRAFT" -> "Unpublished";
                default -> "Published";
            });
            model.addAttribute("courseFields", toCourseDetailFields(course));
            Object thumbnail = getCourseValue(course, "thumbnail_url");
            String thumbnailUrl = thumbnail == null ? null : thumbnail.toString().trim();
            if (thumbnailUrl != null && !thumbnailUrl.isBlank()
                    && !thumbnailUrl.matches("(?i)^https?://.*") && thumbnailUrl.matches("(?i)^www\\..*")) {
                thumbnailUrl = "https://" + thumbnailUrl;
            }
            model.addAttribute("thumbnailUrl", thumbnailUrl == null || thumbnailUrl.isBlank() ? null : thumbnailUrl);
            return "pages/manager/course/course-detail";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("courseError", "Không tìm thấy khóa học.");
            return "redirect:/manager/courses";
        });
    }

    @PostMapping
    public String createCourse(@RequestParam String name, @RequestParam String level,
                               @RequestParam(required = false) Double price, @RequestParam(required = false) String description,
                               @RequestParam(required = false) Integer durationHours, @RequestParam(required = false) String objectives,
                               @RequestParam(required = false) String prerequisites, @RequestParam(required = false) String targetAudience,
                               RedirectAttributes redirectAttributes) {
        try {
            courseService.createCourse(clean(name), validLevel(level), validPrice(price),
                    cleanOptional(description), validDuration(durationHours), cleanOptional(objectives),
                    cleanOptional(prerequisites), cleanOptional(targetAudience));
            redirectAttributes.addFlashAttribute("courseSuccess", "Đã thêm khóa học.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("courseError", exception.getMessage());
        } catch (DataAccessException exception) {
            log.error("Could not create course", exception);
            redirectAttributes.addFlashAttribute("courseError", "Không thể hoàn tất thao tác thêm khóa học. Kiểm tra log ứng dụng để biết chi tiết.");
        }
        return "redirect:/manager/courses";
    }

    @PostMapping("/{courseId}")
    public String updateCourse(@PathVariable long courseId, @RequestParam String name, @RequestParam String level,
                               @RequestParam(required = false) Double price,
                               @RequestParam(required = false) String description, @RequestParam(required = false) Integer durationHours,
                               @RequestParam(required = false) String objectives, @RequestParam(required = false) String prerequisites,
                               @RequestParam(required = false) String targetAudience, RedirectAttributes redirectAttributes) {
        try {
            courseService.updateCourse(courseId, clean(name), validLevel(level), validPrice(price),
                    cleanOptional(description), validDuration(durationHours), cleanOptional(objectives),
                    cleanOptional(prerequisites), cleanOptional(targetAudience));
            redirectAttributes.addFlashAttribute("courseSuccess", "Đã cập nhật khóa học.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("courseError", exception.getMessage());
        } catch (DataAccessException exception) {
            log.error("Could not update course {}", courseId, exception);
            redirectAttributes.addFlashAttribute("courseError", "Không thể hoàn tất thao tác cập nhật khóa học. Kiểm tra log ứng dụng để biết chi tiết.");
        }
        return "redirect:/manager/courses";
    }

    @PostMapping("/{courseId}/status")
    public String transitionCourseStatus(@PathVariable long courseId, @RequestParam String status,
                                         RedirectAttributes redirectAttributes) {
        try {
            courseService.transitionCourseStatus(courseId, status);
            redirectAttributes.addFlashAttribute("courseSuccess", "Đã chuyển trạng thái khóa học.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("courseError", exception.getMessage());
        } catch (DataAccessException exception) {
            log.error("Could not transition course {} status", courseId, exception);
            redirectAttributes.addFlashAttribute("courseError", "Không thể chuyển trạng thái khóa học. Kiểm tra log ứng dụng để biết chi tiết.");
        }
        return "redirect:/manager/courses/" + courseId + "/view";
    }

    private List<Map<String, Object>> toCourseDetailFields(Map<String, Object> course) {
        Map<String, String> labels = Map.ofEntries(
                Map.entry("id", "Mã khóa học"), Map.entry("course_id", "Mã khóa học"),
                Map.entry("title", "Tên khóa học"), Map.entry("course_title", "Tên khóa học"),
                Map.entry("slug", "Đường dẫn thân thiện"), Map.entry("code", "Mã khóa học"),
                Map.entry("level", "Cấp độ JLPT"), Map.entry("jlpt_level", "Cấp độ JLPT"),
                Map.entry("short_description", "Mô tả ngắn"), Map.entry("full_description", "Mô tả chi tiết"),
                Map.entry("description", "Mô tả"), Map.entry("price", "Giá khóa học"),
                Map.entry("discount_price", "Giá ưu đãi"), Map.entry("duration_hours", "Thời lượng (giờ)"),
                Map.entry("duration", "Thời lượng"), Map.entry("lesson_count", "Số bài học"),
                Map.entry("covered_skills", "Kỹ năng bao gồm"), Map.entry("objectives", "Mục tiêu khóa học"),
                Map.entry("target_audience", "Đối tượng học viên"), Map.entry("prerequisites", "Điều kiện tiên quyết"),
                Map.entry("status", "Trạng thái"), Map.entry("is_featured", "Khóa học nổi bật"),
                Map.entry("featured", "Khóa học nổi bật"), Map.entry("enrollment_count", "Số lượt đăng ký"),
                Map.entry("rating", "Đánh giá"), Map.entry("created_at", "Ngày tạo"),
                Map.entry("created_date", "Ngày tạo"), Map.entry("updated_at", "Ngày cập nhật"),
                Map.entry("published_at", "Ngày xuất bản"), Map.entry("published_date", "Ngày xuất bản"));
        NumberFormat moneyFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        return course.entrySet().stream().filter(entry -> !"thumbnail_url".equalsIgnoreCase(entry.getKey())
                && !"id".equalsIgnoreCase(entry.getKey())).map(entry -> {
            String key = entry.getKey(); Object value = entry.getValue();
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("label", labels.getOrDefault(key.toLowerCase(Locale.ROOT), key));
            field.put("rating", "rating".equalsIgnoreCase(key));
            field.put("status", "status".equalsIgnoreCase(key));
            if ("status".equalsIgnoreCase(key)) {
                String status = value == null ? "" : value.toString();
                String normalizedStatus = normalizedCourseStatus(status);
                field.put("displayValue", courseStatusLabel(status));
                field.put("statusClass", "PUBLISHED".equalsIgnoreCase(normalizedStatus) || "published".equalsIgnoreCase(normalizedStatus)
                        ? "bg-success"
                        : "ARCHIVED".equalsIgnoreCase(normalizedStatus) ? "bg-secondary" : "bg-warning text-dark");
            } else if ("rating".equalsIgnoreCase(key) && value instanceof Number number) {
                field.put("ratingValue", Math.max(0, Math.min(5, number.doubleValue())));
                field.put("displayValue", number.doubleValue() + "/5");
            } else if (("price".equalsIgnoreCase(key) || "discount_price".equalsIgnoreCase(key)) && value instanceof Number number) {
                field.put("displayValue", moneyFormat.format(number.doubleValue()) + " VND");
            } else {
                field.put("displayValue", value == null ? "—" : value);
            }
            return field;
        }).toList();
    }

    private Map<String, Object> toCourseView(CourseOption row) {
        Map<String, Object> course = new LinkedHashMap<>();
        course.put("id", row.id()); course.put("name", row.title()); course.put("level", row.level());
        course.put("description", row.description()); course.put("thumbnailUrl", row.thumbnailUrl());
        course.put("price", row.price()); course.put("discountPrice", row.discountPrice());
        course.put("durationHours", row.durationHours()); course.put("coveredSkills", row.coveredSkills());
        course.put("featured", row.featured()); course.put("status", row.status());
        course.put("statusLabel", courseStatusLabel(row.status()));
        course.put("statusFilterValue", normalizedCourseStatus(row.status()));
        course.put("lessonCount", row.lessonCount()); course.put("createdAt", "-");
        return course;
    }

    private Object getCourseValue(Map<String, Object> course, String... keys) {
        for (String key : keys) {
            for (Map.Entry<String, Object> entry : course.entrySet()) {
                if (key.equalsIgnoreCase(entry.getKey()) && entry.getValue() != null) return entry.getValue();
            }
        }
        return "";
    }

    private String cleanOptional(String value) { if (value == null) return null; String cleaned = value.trim(); return cleaned.isEmpty() ? null : cleaned; }
    private Double validPrice(Double price) { double value = price == null ? 0 : price; if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Giá khóa học phải từ 0 trở lên."); return value; }
    private Integer validDuration(Integer duration) { int value = duration == null ? 0 : duration; if (value < 0) throw new IllegalArgumentException("Thời lượng phải từ 0 giờ trở lên."); return value; }
    private String clean(String name) { String value = name == null ? "" : name.trim(); if (value.isBlank() || value.length() > 255) throw new IllegalArgumentException("Tên khóa học không được để trống và tối đa 255 ký tự."); return value; }
    private String validLevel(String level) { String value = level == null ? "" : level.trim().toUpperCase(); if (!List.of("N5", "N4", "N3", "N2", "N1").contains(value)) throw new IllegalArgumentException("Vui lòng chọn cấp độ JLPT hợp lệ."); return value; }
    private String courseStatusLabel(String status) {
        if ("PUBLISHED".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status)) return "Published";
        if ("UNPUBLISHED".equalsIgnoreCase(status) || "INACTIVE".equalsIgnoreCase(status) || "DRAFT".equalsIgnoreCase(status)) return "Unpublished";
        if ("ARCHIVED".equalsIgnoreCase(status)) return "Archived";
        return status;
    }
    private String normalizedCourseStatus(String status) {
        if ("ACTIVE".equalsIgnoreCase(status)) return "published";
        if ("INACTIVE".equalsIgnoreCase(status) || "UNPUBLISHED".equalsIgnoreCase(status)) return "DRAFT";
        return status == null ? null : status.toUpperCase();
    }
}
