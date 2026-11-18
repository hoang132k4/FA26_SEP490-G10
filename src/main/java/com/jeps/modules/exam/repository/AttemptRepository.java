package com.jeps.modules.exam.repository;

import com.jeps.modules.exam.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
}
