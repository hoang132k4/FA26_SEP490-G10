package com.jlpp.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_COURSES = "courses";
    public static final String CACHE_COURSE_DETAILS = "courseDetails";
    public static final String CACHE_LESSONS = "lessons";
    public static final String CACHE_EXAMS = "exams";
    public static final String CACHE_STUDY_MATERIALS = "studyMaterials";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CACHE_COURSES,
                CACHE_COURSE_DETAILS,
                CACHE_LESSONS,
                CACHE_EXAMS,
                CACHE_STUDY_MATERIALS
        );
    }
}
