package com.jlpp.modules.course.service.impl;

import com.jlpp.modules.course.dto.RecommendedCourseItem;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.repository.CourseRegistrationRepository;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.service.CourseRecommendationService;
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
        // Filter or mark
        return all;
    }
}
