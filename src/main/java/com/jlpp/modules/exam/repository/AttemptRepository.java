package com.jlpp.modules.exam.repository;

import com.jlpp.modules.exam.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
}
