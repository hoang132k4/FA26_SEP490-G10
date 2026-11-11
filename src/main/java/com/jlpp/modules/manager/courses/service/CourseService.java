package com.jlpp.modules.manager.courses.service;

import com.jlpp.common.constants.LessonStatusEnum;
import com.jlpp.modules.manager.courses.dto.CourseOption;
import com.jlpp.modules.manager.courses.repository.CourseRepository;
import com.jlpp.modules.manager.lessons.dto.LessonDtos.LessonInput;
import com.jlpp.modules.manager.lessons.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

@Service
public class CourseService {

    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "PUBLISHED", "ARCHIVED");

    private final CourseRepository courseRepository;
    private LessonRepository lessonRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Autowired(required = false)
    public void setLessonRepository(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseOption> findCourses() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<CourseOption> findCourse(long courseId) {
        return courseRepository.findById(courseId);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> findCourseDetails(long courseId) {
        return courseRepository.findDetailsById(courseId);
    }

    @Transactional(readOnly = true)
    public boolean isCodeInUse(String code, Long excludedCourseId) {
        return courseRepository.existsByCode(code, excludedCourseId);
    }

    @Transactional(readOnly = true)
    public boolean isTitleInUse(String title, Long excludedCourseId) {
        return courseRepository.existsByTitle(title, excludedCourseId);
    }

    @Transactional
    public long createCourse(String title, String code, String level, Double price, Double discountPrice,
                             String shortDescription, String description, String thumbnailUrl, String objectives,
                             String prerequisites, String targetAudience) {
        long courseId = courseRepository.insert(title, code, level, "DRAFT", price, discountPrice,
                shortDescription, description, thumbnailUrl, objectives, prerequisites, targetAudience);
        if (lessonRepository != null) {
            try {
                LessonInput milestoneLesson = new LessonInput(
                        "Bài thi giữa kì: Đánh giá năng lực giai đoạn 1",
                        "Kiểm tra & Đánh giá định kỳ",
                        "Bài kiểm tra cột mốc định kỳ bắt buộc để đánh giá năng lực học viên.",
                        "Hoàn thành bài kiểm tra cột mốc để đánh giá mức độ tiếp thu kiến thức và mở khóa giai đoạn tiếp theo.",
                        "EXAM",
                        LessonStatusEnum.ACTIVE.name(),
                        1,
                        35,
                        false
                );
                lessonRepository.insert(courseId, milestoneLesson, 1);
            } catch (Exception ignored) {
            }
        }
        return courseId;
    }

    @Transactional
    public void updateCourse(long courseId, String title, String code, String level, Double price, Double discountPrice,
                             String shortDescription, String description, String thumbnailUrl, String objectives,
                             String prerequisites, String targetAudience) {
        if (courseRepository.update(courseId, title, code, level, price, discountPrice,
                shortDescription, description, thumbnailUrl, objectives, prerequisites, targetAudience) == 0) {
            throw new IllegalArgumentException("Không tìm thấy khóa học cần sửa.");
        }
    }

    @Transactional
    public void transitionCourseStatus(long courseId, String requestedStatus) {
        String targetStatus = normalizeStatus(requestedStatus);
        if (!VALID_STATUSES.contains(targetStatus)) {
            throw new IllegalArgumentException("Vui lòng chọn trạng thái khóa học hợp lệ.");
        }

        String currentStatus = courseRepository.findStatusById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học cần chuyển trạng thái."));
        if (targetStatus.equals(normalizeStatus(currentStatus))) return;

        if (courseRepository.transitionStatus(courseId, targetStatus) == 0) {
            throw new IllegalArgumentException("Không tìm thấy khóa học cần chuyển trạng thái.");
        }
    }

    private String normalizeStatus(String status) {
        String value = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "ACTIVE" -> "PUBLISHED";
            case "INACTIVE", "UNPUBLISHED" -> "DRAFT";
            default -> value;
        };
    }
}
