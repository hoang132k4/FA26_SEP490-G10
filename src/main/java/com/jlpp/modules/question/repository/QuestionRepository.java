package com.jlpp.modules.question.repository;

import com.jlpp.modules.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("""
            select q from Question q
            where (:search = '' or lower(q.content) like lower(concat('%', :search, '%')))
              and (:category = '' or lower(q.category) = lower(:category))
              and (:examId is null or q.examId = :examId)
            order by q.id desc
            """)
    List<Question> searchForManager(@Param("search") String search,
                                    @Param("category") String category,
                                    @Param("examId") Long examId);

    @Query("select distinct q.category from Question q where q.category is not null and q.category <> '' order by q.category")
    List<String> findDistinctCategories();
}
