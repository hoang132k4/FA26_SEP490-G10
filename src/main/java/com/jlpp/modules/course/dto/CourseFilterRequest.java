package com.jlpp.modules.course.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseFilterRequest {
    private String keyword;
    private String level;
    @Builder.Default
    private String sortBy = "popular"; // popular, recently_updated, price_asc, price_desc
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 9;
}
