package com.jlpp.modules.manager.courses.controller;

import com.jlpp.modules.manager.courses.dto.CourseOption;
import com.jlpp.modules.manager.courses.service.CourseService;
import com.jlpp.modules.manager.courses.service.CourseImageStorageService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/manager/courses")
public class ManagerCourseController {

    private static final Logger log = LoggerFactory.getLogger(ManagerCourseController.class);
    private final CourseService courseService;
    private final CourseImageStorageService courseImageStorageService;

    public ManagerCourseController(CourseService courseService, CourseImageStorageService courseImageStorageService) {
        this.courseService = courseService;
        this.courseImageStorageService = courseImageStorageService;
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
        form.put("code", ""); form.put("price", "0"); form.put("discountPrice", "");
        form.put("shortDescription", ""); form.put("description", "");
        form.put("thumbnailUrl", "");
        form.put("imageMode", "file");
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
            form.put("code", getCourseValue(course, "code", "course_code"));
            form.put("level", getCourseValue(course, "jlpt_level", "level"));
            form.put("status", normalizedCourseStatus(String.valueOf(getCourseValue(course, "status"))));
            form.put("price", getCourseValue(course, "price", "course_price"));
            form.put("discountPrice", getCourseValue(course, "discount_price", "course_discount_price"));
            form.put("shortDescription", getCourseValue(course, "short_description", "course_description"));
            form.put("description", getCourseValue(course, "full_description", "description"));
            form.put("thumbnailUrl", getCourseValue(course, "thumbnail_url", "course_thumbnail", "thumbnail"));
            form.put("imageMode", "url");
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
    public String createCourse(@RequestParam(required = false) String name, @RequestParam(required = false) String code,
                               @RequestParam(required = false) String level, @RequestParam(required = false) String price,
                               @RequestParam(required = false) String discountPrice,
                               @RequestParam(required = false) String shortDescription, @RequestParam(required = false) String description,
                               @RequestParam(required = false) String objectives,
                               @RequestParam(required = false) String prerequisites, @RequestParam(required = false) String targetAudience,
                               @RequestParam(required = false) String thumbnailUrl, @RequestParam(required = false) String imageMode,
                               @RequestParam(required = false) MultipartFile imageFile, Model model,
                               RedirectAttributes redirectAttributes) {
        Map<String, String> errors = validateCourseForm(name, code, level, price, discountPrice, shortDescription,
                description, objectives, prerequisites, targetAudience, thumbnailUrl, imageMode, imageFile, null);
        if (!errors.isEmpty()) return showInvalidCourseForm(model, errors,
                courseFormValues("", name, code, level, price, discountPrice, shortDescription, description,
                        objectives, prerequisites, targetAudience, thumbnailUrl, imageMode));
        try {
            String savedThumbnail = resolveThumbnailUrl(thumbnailUrl, imageFile);
            courseService.createCourse(clean(name), cleanCourseCode(code), validLevel(level), validPrice(price), parseOptionalPrice(discountPrice),
                    cleanOptional(shortDescription), cleanOptional(description), savedThumbnail, cleanOptional(objectives),
                    cleanOptional(prerequisites), cleanOptional(targetAudience));
            redirectAttributes.addFlashAttribute("courseSuccess", "Đã thêm khóa học.");
        } catch (IllegalArgumentException exception) {
            errors.put("imageFile", exception.getMessage());
            return showInvalidCourseForm(model, errors, courseFormValues("", name, code, level, price, discountPrice,
                    shortDescription, description, objectives, prerequisites, targetAudience, thumbnailUrl, imageMode));
        } catch (java.io.IOException exception) {
            log.error("Could not store course image", exception);
            errors.put("imageFile", "Không thể lưu ảnh khóa học. Vui lòng thử lại.");
            return showInvalidCourseForm(model, errors, courseFormValues("", name, code, level, price, discountPrice,
                    shortDescription, description, objectives, prerequisites, targetAudience, thumbnailUrl, imageMode));
        } catch (DataAccessException exception) {
            log.error("Could not create course", exception);
            redirectAttributes.addFlashAttribute("courseError", "Không thể hoàn tất thao tác thêm khóa học. Kiểm tra log ứng dụng để biết chi tiết.");
        }
        return "redirect:/manager/courses";
    }

    @PostMapping("/{courseId}")
    public String updateCourse(@PathVariable long courseId, @RequestParam(required = false) String name,
                               @RequestParam(required = false) String code, @RequestParam(required = false) String level,
                               @RequestParam(required = false) String price, @RequestParam(required = false) String discountPrice,
                               @RequestParam(required = false) String shortDescription, @RequestParam(required = false) String description,
                               @RequestParam(required = false) String objectives, @RequestParam(required = false) String prerequisites,
                               @RequestParam(required = false) String targetAudience, @RequestParam(required = false) String thumbnailUrl,
                               @RequestParam(required = false) String imageMode, @RequestParam(required = false) MultipartFile imageFile,
                               Model model, RedirectAttributes redirectAttributes) {
        Map<String, String> errors = validateCourseForm(name, code, level, price, discountPrice, shortDescription,
                description, objectives, prerequisites, targetAudience, thumbnailUrl, imageMode, imageFile, courseId);
        if (!errors.isEmpty()) return showInvalidCourseForm(model, errors,
                courseFormValues(String.valueOf(courseId), name, code, level, price, discountPrice, shortDescription,
                        description, objectives, prerequisites, targetAudience, thumbnailUrl, imageMode));
        try {
            String savedThumbnail = resolveThumbnailUrl(thumbnailUrl, imageFile);
            courseService.updateCourse(courseId, clean(name), cleanCourseCode(code), validLevel(level), validPrice(price), parseOptionalPrice(discountPrice),
                    cleanOptional(shortDescription), cleanOptional(description), savedThumbnail, cleanOptional(objectives),
                    cleanOptional(prerequisites), cleanOptional(targetAudience));
            redirectAttributes.addFlashAttribute("courseSuccess", "Đã cập nhật khóa học.");
        } catch (IllegalArgumentException exception) {
            errors.put("imageFile", exception.getMessage());
            return showInvalidCourseForm(model, errors, courseFormValues(String.valueOf(courseId), name, code, level, price,
                    discountPrice, shortDescription, description, objectives, prerequisites, targetAudience, thumbnailUrl, imageMode));
        } catch (java.io.IOException exception) {
            log.error("Could not store image for course {}", courseId, exception);
            errors.put("imageFile", "Không thể lưu ảnh khóa học. Vui lòng thử lại.");
            return showInvalidCourseForm(model, errors, courseFormValues(String.valueOf(courseId), name, code, level, price,
                    discountPrice, shortDescription, description, objectives, prerequisites, targetAudience, thumbnailUrl, imageMode));
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
        Set<String> hiddenFields = Set.of("thumbnail_url", "id", "title", "course_title", "created_at",
                "created_date", "is_featured", "featured", "slug");
        return course.entrySet().stream().filter(entry -> !hiddenFields.contains(entry.getKey().toLowerCase(Locale.ROOT)))
                .map(entry -> {
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

    private Map<String, String> validateCourseForm(String name, String code, String level, String price,
            String discountPrice, String shortDescription, String description, String objectives,
            String prerequisites, String targetAudience, String thumbnailUrl, String imageMode,
            MultipartFile imageFile, Long courseId) {
        Map<String, String> errors = new LinkedHashMap<>();
        String cleanName = name == null ? "" : name.trim();
        String cleanCode = code == null ? "" : code.trim();

        if (cleanName.isEmpty()) errors.put("name", "Vui lòng nhập tên khóa học.");
        else if (cleanName.length() > 255) errors.put("name", "Tên khóa học tối đa 255 ký tự.");
        else if (courseService.isTitleInUse(cleanName, courseId)) errors.put("name", "Tên khóa học này đã được sử dụng.");

        if (cleanCode.isEmpty()) errors.put("code", "Vui lòng nhập mã khóa học.");
        else if (cleanCode.length() > 50) errors.put("code", "Mã khóa học tối đa 50 ký tự.");
        else if (courseService.isCodeInUse(cleanCode, courseId)) errors.put("code", "Mã khóa học này đã được sử dụng.");

        try { validLevel(level); }
        catch (IllegalArgumentException exception) { errors.put("level", exception.getMessage()); }

        Double basePrice = parsePriceField(price, "price", true, errors);
        Double promotionalPrice = parsePriceField(discountPrice, "discountPrice", false, errors);
        if (basePrice != null && promotionalPrice != null && promotionalPrice > basePrice) {
            errors.put("discountPrice", "Giá ưu đãi không được cao hơn giá gốc.");
        }

        checkMaxLength(shortDescription, 500, "shortDescription", "Mô tả ngắn", errors);
        checkMaxLength(description, 2000, "description", "Mô tả chi tiết", errors);
        checkMaxLength(objectives, 5000, "objectives", "Mục tiêu khóa học", errors);
        checkMaxLength(prerequisites, 2000, "prerequisites", "Điều kiện tiên quyết", errors);
        checkMaxLength(targetAudience, 2000, "targetAudience", "Đối tượng học viên", errors);

        boolean fileMode = "file".equalsIgnoreCase(imageMode)
                || (imageMode == null && courseImageStorageService.hasFile(imageFile));
        if (fileMode && courseImageStorageService.hasFile(imageFile)) {
            try { courseImageStorageService.validate(imageFile); }
            catch (IllegalArgumentException exception) { errors.put("imageFile", exception.getMessage()); }
            catch (java.io.IOException exception) { errors.put("imageFile", "Không thể đọc file ảnh đã chọn."); }
        } else if (!fileMode) {
            try { validThumbnailUrl(thumbnailUrl); }
            catch (IllegalArgumentException exception) { errors.put("thumbnailUrl", exception.getMessage()); }
        }
        return errors;
    }

    private Double parsePriceField(String rawValue, String field, boolean required, Map<String, String> errors) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            if (required) errors.put(field, "Vui lòng nhập giá khóa học.");
            return null;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed) || parsed < 0) {
                errors.put(field, "Giá phải là số từ 0 trở lên.");
                return null;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            errors.put(field, "Vui lòng nhập một giá trị số hợp lệ.");
            return null;
        }
    }

    private void checkMaxLength(String value, int max, String field, String label, Map<String, String> errors) {
        if (value != null && value.length() > max) errors.put(field, label + " tối đa " + max + " ký tự.");
    }

    private Map<String, Object> courseFormValues(String id, String name, String code, String level,
            String price, String discountPrice, String shortDescription, String description,
            String objectives, String prerequisites, String targetAudience, String thumbnailUrl, String imageMode) {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("id", id); form.put("name", name == null ? "" : name); form.put("code", code == null ? "" : code);
        form.put("level", level == null || level.isBlank() ? "N5" : level);
        form.put("price", price == null ? "" : price); form.put("discountPrice", discountPrice == null ? "" : discountPrice);
        form.put("shortDescription", shortDescription == null ? "" : shortDescription);
        form.put("description", description == null ? "" : description);
        form.put("objectives", objectives == null ? "" : objectives);
        form.put("prerequisites", prerequisites == null ? "" : prerequisites);
        form.put("targetAudience", targetAudience == null ? "" : targetAudience);
        form.put("thumbnailUrl", thumbnailUrl == null ? "" : thumbnailUrl);
        form.put("imageMode", "url".equalsIgnoreCase(imageMode) ? "url" : "file");
        return form;
    }

    private String showInvalidCourseForm(Model model, Map<String, String> errors, Map<String, Object> form) {
        model.addAttribute("courseErrors", errors);
        model.addAttribute("courseForm", form);
        model.addAttribute("courseError", "Vui lòng kiểm tra các trường được đánh dấu.");
        return courses(model);
    }

    private Map<String, Object> toCourseView(CourseOption row) {
        Map<String, Object> course = new LinkedHashMap<>();
        course.put("id", row.id()); course.put("name", row.title()); course.put("code", row.code()); course.put("level", row.level());
        course.put("description", row.shortDescription()); course.put("thumbnailUrl", row.thumbnailUrl());
        course.put("price", row.price()); course.put("discountPrice", row.discountPrice());
        course.put("durationLabel", row.durationMinutes() == null ? "Chưa xác định" : formatDuration(row.durationMinutes()));
        course.put("coveredSkills", row.coveredSkills());
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
    private String cleanCourseCode(String code) {
        String value = code == null ? "" : code.trim();
        if (value.isBlank() || value.length() > 50) throw new IllegalArgumentException("Mã khóa học không được để trống và tối đa 50 ký tự.");
        return value;
    }
    private String formatDuration(Integer minutes) {
        int total = minutes == null ? 0 : Math.max(0, minutes);
        int hours = total / 60;
        int remainingMinutes = total % 60;
        if (hours == 0) return total + " phút";
        return remainingMinutes == 0 ? hours + " giờ" : hours + " giờ " + remainingMinutes + " phút";
    }
    private String validThumbnailUrl(String value) {
        String url = cleanOptional(value);
        if (url == null) return null;
        boolean publicUrl = false;
        try {
            java.net.URI parsed = java.net.URI.create(url);
            publicUrl = ("http".equalsIgnoreCase(parsed.getScheme()) || "https".equalsIgnoreCase(parsed.getScheme()))
                    && parsed.getHost() != null && parsed.getUserInfo() == null;
        } catch (IllegalArgumentException ignored) { }
        boolean uploadedImage = url.matches("^/uploads/courses/[A-Za-z0-9-]+\\.(?i:jpg|png|webp|gif)$");
        if (url.length() > 500 || (!publicUrl && !uploadedImage)) {
            throw new IllegalArgumentException("Đường dẫn ảnh phải là URL http/https hợp lệ hoặc ảnh đã tải lên, tối đa 500 ký tự.");
        }
        return url;
    }
    private String resolveThumbnailUrl(String value, MultipartFile imageFile) throws java.io.IOException {
        if (courseImageStorageService.hasFile(imageFile)) return courseImageStorageService.store(imageFile);
        return validThumbnailUrl(value);
    }
    private Double validPrice(String price) {
        Double parsed = parseOptionalPrice(price);
        return parsed == null ? 0d : parsed;
    }
    private Double parseOptionalPrice(String price) {
        if (price == null || price.isBlank()) return null;
        return Double.parseDouble(price.trim());
    }
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
