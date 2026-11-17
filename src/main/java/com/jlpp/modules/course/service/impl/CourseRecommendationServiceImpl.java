package com.jlpp.modules.course.service.impl;

import com.jlpp.modules.course.dto.ExamRecommendationDto;
import com.jlpp.modules.course.dto.RecommendedCourseItem;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.service.CourseRecommendationService;
import com.jlpp.modules.exam.dto.ExamResultResponse;
import com.jlpp.modules.user.entity.User;
import com.jlpp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseRecommendationServiceImpl implements CourseRecommendationService {

    private final CourseRepository courseRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    @Override
    public List<RecommendedCourseItem> getRecommendationsForLearner(Long userId) {
        String currentLevel = "N5";
        String targetLevel = "N4";

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                if (user.getCurrentJlptLevel() != null && !user.getCurrentJlptLevel().isBlank()) {
                    currentLevel = user.getCurrentJlptLevel();
                }
                if (user.getTargetJlptLevel() != null && !user.getTargetJlptLevel().isBlank()) {
                    targetLevel = user.getTargetJlptLevel();
                }
            }
        }

        List<RecommendedCourseItem> items = new ArrayList<>();

        // 1. Primary Recommendation: Target Level Course
        List<Course> targetCourses = courseRepository.findByLevelAndStatus(targetLevel, "PUBLISHED");
        for (Course c : targetCourses) {
            boolean enrolled = userId != null && registrationRepository.existsByUserIdAndCourseId(userId, c.getId());
            items.add(RecommendedCourseItem.builder()
                    .course(c)
                    .matchPercentage(98)
                    .matchReason("Phù hợp 100% với mục tiêu đỗ JLPT " + targetLevel + " trong lộ trình học tập của bạn.")
                    .tag("Mục tiêu trọng tâm")
                    .isEnrolled(enrolled)
                    .build());
        }

        // 2. Secondary Recommendation: Current Level Foundation Course (if different from target)
        if (!currentLevel.equalsIgnoreCase(targetLevel)) {
            List<Course> currentCourses = courseRepository.findByLevelAndStatus(currentLevel, "PUBLISHED");
            for (Course c : currentCourses) {
                boolean enrolled = userId != null && registrationRepository.existsByUserIdAndCourseId(userId, c.getId());
                items.add(RecommendedCourseItem.builder()
                        .course(c)
                        .matchPercentage(92)
                        .matchReason("Củng cố nền tảng cấp độ " + currentLevel + " vững chắc trước khi tiến lên cấp độ cao hơn.")
                        .tag("Nền tảng bổ trợ")
                        .isEnrolled(enrolled)
                        .build());
            }
        }

        // Fallback if none found
        if (items.isEmpty()) {
            List<Course> all = courseRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED");
            for (Course c : all) {
                boolean enrolled = userId != null && registrationRepository.existsByUserIdAndCourseId(userId, c.getId());
                items.add(RecommendedCourseItem.builder()
                        .course(c)
                        .matchPercentage(85)
                        .matchReason("Khóa học phổ biến được nhiều học viên đánh giá cao tại JEPS.")
                        .tag("Khóa học nổi bật")
                        .isEnrolled(enrolled)
                        .build());
                if (items.size() >= 3) break;
            }
        }

        return items;
    }

    @Override
    public List<Course> getOtherAvailableCourses(Long userId) {
        List<Course> all = courseRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED");
        if (userId == null) {
            return all;
        }
        return all;
    }

    @Override
    public ExamRecommendationDto getRecommendationsFromExamResult(ExamResultResponse examResult, Long userId, String targetLevel, String hasStudied, String assignedLevel) {
        if (examResult == null) {
            return null;
        }

        // 1. Resolve effective levels
        if (assignedLevel == null || assignedLevel.isBlank()) {
            assignedLevel = (examResult.getJlptLevel() != null && !examResult.getJlptLevel().isBlank()) 
                    ? examResult.getJlptLevel().trim().toUpperCase() : "N5";
        } else {
            assignedLevel = assignedLevel.trim().toUpperCase();
        }

        if (targetLevel == null || targetLevel.isBlank()) {
            if ("YES".equalsIgnoreCase(hasStudied)) {
                targetLevel = assignedLevel;
            } else {
                targetLevel = switch (assignedLevel) {
                    case "N5" -> "N4";
                    case "N4" -> "N3";
                    case "N3" -> "N2";
                    case "N2" -> "N1";
                    default -> assignedLevel;
                };
            }
        } else {
            targetLevel = targetLevel.trim().toUpperCase();
        }

        int percentage = (examResult.getPercentage() != null) ? examResult.getPercentage().intValue() : 0;
        boolean isAssessmentOk = percentage >= 60;
        String weakestSkill = (examResult.getWeakestSkill() != null && !examResult.getWeakestSkill().isBlank()) 
                ? examResult.getWeakestSkill() : "Ngữ pháp";
        String strongestSkill = (examResult.getStrongestSkill() != null && !examResult.getStrongestSkill().isBlank()) 
                ? examResult.getStrongestSkill() : "Từ vựng";

        // 2. Determine Evaluation Tier & Messaging
        String evalTitle;
        String evalSummary;
        String badgeText;
        String badgeClass;
        String primaryRecommendedLevel;

        if (percentage >= 80) {
            evalTitle = "Xuất sắc! Nền tảng " + assignedLevel + " vững chắc, sẵn sàng bứt phá lên " + targetLevel;
            evalSummary = "Bạn đạt kết quả rất ấn tượng (" + percentage + "%). Nền móng kiến thức ngữ pháp và từ vựng của bạn hoàn toàn đạt chuẩn để theo học ngay chương trình mục tiêu " + targetLevel + ".";
            badgeText = "Nền tảng xuất sắc (" + percentage + "%)";
            badgeClass = "bg-success text-white shadow-sm";
            primaryRecommendedLevel = targetLevel;
        } else if (percentage >= 60) {
            evalTitle = "Đạt yêu cầu đầu vào! Bước đệm tốt để học JLPT " + targetLevel;
            evalSummary = "Bạn đã hoàn thành tốt bài kiểm tra với " + percentage + "%. Bạn đủ điều kiện để học thẳng " + targetLevel + ", tuy nhiên nên chú ý bồi dưỡng thêm kỹ năng " + weakestSkill + ".";
            badgeText = "Đạt chuẩn đầu vào (" + percentage + "%)";
            badgeClass = "bg-success-subtle text-success border border-success-subtle";
            primaryRecommendedLevel = targetLevel;
        } else if (percentage >= 40) {
            evalTitle = "Cần củng cố nền tảng " + assignedLevel + " trước khi bước vào " + targetLevel;
            evalSummary = "Kết quả bài kiểm tra đạt " + percentage + "%, trong đó kỹ năng " + weakestSkill + " còn chưa đạt yêu cầu. Để tránh bị quá tải kiến thức khi lên " + targetLevel + ", bạn nên hoàn thiện khóa học nền tảng " + assignedLevel + " trước.";
            badgeText = "Cần củng cố thêm (" + percentage + "%)";
            badgeClass = "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
            primaryRecommendedLevel = assignedLevel;
        } else {
            evalTitle = "Khuyến nghị xây dựng lại nền móng kiến thức " + assignedLevel;
            evalSummary = "Điểm số hiện tại (" + percentage + "%) cho thấy bạn bị hổng khá nhiều kiến thức cơ bản ở phần " + weakestSkill + ". Hãy bắt đầu với khóa học cốt lõi " + assignedLevel + " để tích lũy từ vựng và ngữ pháp trước.";
            badgeText = "Chưa đạt chuẩn (" + percentage + "%)";
            badgeClass = "bg-danger-subtle text-danger border border-danger-subtle";
            primaryRecommendedLevel = assignedLevel;
        }

        // 3. Find Candidate Courses from Database
        List<Course> allPublished = courseRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED");
        List<RecommendedCourseItem> recommendations = new ArrayList<>();
        List<Long> addedCourseIds = new ArrayList<>();

        // Tier 1: Primary Course
        Course primaryCourse = findBestCourseForLevel(allPublished, primaryRecommendedLevel, null);
        if (primaryCourse == null) {
            primaryCourse = findBestCourseForLevel(allPublished, assignedLevel, null);
        }
        if (primaryCourse != null) {
            addedCourseIds.add(primaryCourse.getId());
            boolean enrolled = userId != null && registrationRepository.existsByUserIdAndCourseId(userId, primaryCourse.getId());
            String reason = percentage >= 60 
                    ? "Khóa học mục tiêu trọng tâm giúp bạn chinh phục JLPT " + primaryRecommendedLevel + " theo lộ trình chuẩn."
                    : "Củng cố toàn diện nền móng kiến thức " + primaryRecommendedLevel + " trước khi tiến lên cấp độ cao hơn.";
            recommendations.add(RecommendedCourseItem.builder()
                    .course(primaryCourse)
                    .matchPercentage(percentage >= 80 ? 98 : (percentage >= 60 ? 95 : 92))
                    .matchReason(reason)
                    .tag(percentage >= 60 ? "Lộ trình tối ưu" : "Nền tảng cốt lõi")
                    .isEnrolled(enrolled)
                    .build());
        }

        // Tier 2: Skill-gap Course (khắc phục kỹ năng yếu nhất)
        Course skillGapCourse = findSkillGapCourse(allPublished, primaryRecommendedLevel, assignedLevel, weakestSkill, addedCourseIds);
        if (skillGapCourse != null) {
            addedCourseIds.add(skillGapCourse.getId());
            boolean enrolled = userId != null && registrationRepository.existsByUserIdAndCourseId(userId, skillGapCourse.getId());
            recommendations.add(RecommendedCourseItem.builder()
                    .course(skillGapCourse)
                    .matchPercentage(88)
                    .matchReason("Tập trung bồi dưỡng chuyên sâu kỹ năng " + weakestSkill + " để không bị điểm liệt và cân bằng năng lực.")
                    .tag("Khắc phục điểm yếu")
                    .isEnrolled(enrolled)
                    .build());
        }

        // Tier 3: Practice / Exam-prep Course (Luyện đề thực chiến)
        Course practiceCourse = findPracticeCourse(allPublished, primaryRecommendedLevel, targetLevel, addedCourseIds);
        if (practiceCourse != null) {
            addedCourseIds.add(practiceCourse.getId());
            boolean enrolled = userId != null && registrationRepository.existsByUserIdAndCourseId(userId, practiceCourse.getId());
            recommendations.add(RecommendedCourseItem.builder()
                    .course(practiceCourse)
                    .matchPercentage(85)
                    .matchReason("Luyện giải đề thi chuẩn format quốc tế giúp rèn luyện phản xạ và kỹ năng quản trị thời gian.")
                    .tag("Luyện thi thực chiến")
                    .isEnrolled(enrolled)
                    .build());
        }

        // Fallback: If less than 2 courses, add other high quality courses of target or assigned level
        if (recommendations.size() < 2) {
            for (Course c : allPublished) {
                if (!addedCourseIds.contains(c.getId())) {
                    if (targetLevel.equalsIgnoreCase(c.getLevel()) || assignedLevel.equalsIgnoreCase(c.getLevel())) {
                        addedCourseIds.add(c.getId());
                        boolean enrolled = userId != null && registrationRepository.existsByUserIdAndCourseId(userId, c.getId());
                        recommendations.add(RecommendedCourseItem.builder()
                                .course(c)
                                .matchPercentage(80)
                                .matchReason("Khóa học chất lượng cao được nhiều học viên đánh giá 5 sao.")
                                .tag("Khóa học bổ trợ")
                                .isEnrolled(enrolled)
                                .build());
                        if (recommendations.size() >= 3) break;
                    }
                }
            }
        }

        RecommendedCourseItem primaryItem = recommendations.isEmpty() ? null : recommendations.get(0);
        RecommendedCourseItem skillGapItem = recommendations.size() > 1 ? recommendations.get(1) : null;
        RecommendedCourseItem practiceItem = recommendations.size() > 2 ? recommendations.get(2) : null;

        return ExamRecommendationDto.builder()
                .evaluationTitle(evalTitle)
                .evaluationSummary(evalSummary)
                .assessmentBadgeText(badgeText)
                .assessmentBadgeClass(badgeClass)
                .targetLevel(targetLevel)
                .currentAssessedLevel(assignedLevel)
                .weakestSkill(weakestSkill)
                .strongestSkill(strongestSkill)
                .percentage(percentage)
                .isAssessmentOk(isAssessmentOk)
                .primaryCourse(primaryItem)
                .skillGapCourse(skillGapItem)
                .practiceCourse(practiceItem)
                .allRecommendations(recommendations)
                .build();
    }

    private Course findBestCourseForLevel(List<Course> courses, String level, List<Long> excludeIds) {
        if (courses == null || level == null) return null;
        return courses.stream()
                .filter(c -> level.equalsIgnoreCase(c.getLevel()))
                .filter(c -> excludeIds == null || !excludeIds.contains(c.getId()))
                .sorted((a, b) -> {
                    boolean aFeatured = Boolean.TRUE.equals(a.getFeatured());
                    boolean bFeatured = Boolean.TRUE.equals(b.getFeatured());
                    if (aFeatured != bFeatured) return aFeatured ? -1 : 1;
                    return Long.compare(b.getId(), a.getId());
                })
                .findFirst()
                .orElse(null);
    }

    private Course findSkillGapCourse(List<Course> courses, String primaryLevel, String fallbackLevel, String weakestSkill, List<Long> excludeIds) {
        if (courses == null || weakestSkill == null) return null;
        String wLower = weakestSkill.toLowerCase();
        String keyword = "";
        if (wLower.contains("từ vựng") || wLower.contains("vocab") || wLower.contains("kanji") || wLower.contains("chữ hán") || wLower.contains("hán tự")) {
            keyword = "từ vựng";
        } else if (wLower.contains("ngữ pháp") || wLower.contains("grammar")) {
            keyword = "ngữ pháp";
        } else if (wLower.contains("đọc") || wLower.contains("reading")) {
            keyword = "đọc hiểu";
        } else if (wLower.contains("nghe") || wLower.contains("listening")) {
            keyword = "nghe hiểu";
        }

        final String finalKeyword = keyword;
        Course matched = courses.stream()
                .filter(c -> primaryLevel.equalsIgnoreCase(c.getLevel()))
                .filter(c -> excludeIds == null || !excludeIds.contains(c.getId()))
                .filter(c -> matchesKeyword(c, finalKeyword))
                .findFirst()
                .orElse(null);

        if (matched == null && fallbackLevel != null && !fallbackLevel.equalsIgnoreCase(primaryLevel)) {
            matched = courses.stream()
                    .filter(c -> fallbackLevel.equalsIgnoreCase(c.getLevel()))
                    .filter(c -> excludeIds == null || !excludeIds.contains(c.getId()))
                    .filter(c -> matchesKeyword(c, finalKeyword))
                    .findFirst()
                    .orElse(null);
        }

        return matched;
    }

    private Course findPracticeCourse(List<Course> courses, String primaryLevel, String targetLevel, List<Long> excludeIds) {
        if (courses == null) return null;
        return courses.stream()
                .filter(c -> primaryLevel.equalsIgnoreCase(c.getLevel()) || (targetLevel != null && targetLevel.equalsIgnoreCase(c.getLevel())))
                .filter(c -> excludeIds == null || !excludeIds.contains(c.getId()))
                .filter(this::matchesPracticeKeyword)
                .findFirst()
                .orElse(null);
    }

    private boolean matchesKeyword(Course c, String keyword) {
        if (keyword == null || keyword.isBlank()) return false;
        String text = (c.getTitle() + " " + (c.getShortDescription() != null ? c.getShortDescription() : "") + " " + (c.getCoveredSkills() != null ? c.getCoveredSkills() : "")).toLowerCase();
        return text.contains(keyword);
    }

    private boolean matchesPracticeKeyword(Course c) {
        String text = (c.getTitle() + " " + (c.getShortDescription() != null ? c.getShortDescription() : "") + " " + (c.getCoveredSkills() != null ? c.getCoveredSkills() : "")).toLowerCase();
        return text.contains("đề") || text.contains("luyện") || text.contains("thực chiến") || text.contains("format");
    }
}
