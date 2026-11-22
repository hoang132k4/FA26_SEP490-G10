package com.jeps.modules.admin.repository;

import com.jeps.modules.admin.entity.AdminSystemLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSystemLogRepository extends JpaRepository<AdminSystemLog, Long> {
}
