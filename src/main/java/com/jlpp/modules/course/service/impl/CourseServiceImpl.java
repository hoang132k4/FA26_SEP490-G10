package com.jlpp.modules.course.service.impl;

import com.jlpp.modules.course.dto.CourseFilterRequest;
import com.jlpp.modules.course.entity.Course;
import com.jlpp.modules.course.repository.CourseRepository;
import com.jlpp.modules.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    @Override
    public Page<Course> getCourses(CourseFilterRequest filter) {
        if (filter == null) {
            filter = new CourseFilterRequest();
        }

        Sort sort = switch (filter.getSortBy() != null ? filter.getSortBy() : "popular") {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "recently_updated" -> Sort.by(Sort.Direction.DESC, "updatedAt", "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "enrollmentCount", "createdAt");
        };

        int pageNum = Math.max(0, filter.getPage());
        int pageSize = filter.getSize() > 0 ? filter.getSize() : 9;
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

        String level = (filter.getLevel() != null && !filter.getLevel().trim().isEmpty() && !filter.getLevel().equalsIgnoreCase("ALL"))
                ? filter.getLevel().trim().toUpperCase()
                : null;
        String keyword = (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty())
                ? filter.getKeyword().trim()
                : null;

        return courseRepository.searchCourses(keyword, level, pageable);
    }

    @Override
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy khóa học với mã ID: " + id));
    }

    @Override
    public List<Course> getFeaturedCourses() {
        return courseRepository.findTop4ByFeaturedTrueAndStatus("PUBLISHED");
    }

    @Override
    public List<Course> getOtherCourses(Long excludedId, String level, int limit) {
        List<Course> list = courseRepository.findTop3ByLevelAndStatusAndIdNot(level, "PUBLISHED", excludedId);
        if (list.size() < limit) {
            List<Course> all = courseRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED");
            for (Course c : all) {
                if (!c.getId().equals(excludedId) && !list.contains(c)) {
                    list.add(c);
                    if (list.size() >= limit) break;
                }
            }
        }
        return list;
    }
}
