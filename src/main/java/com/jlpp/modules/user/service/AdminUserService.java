package com.jlpp.modules.user.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.jlpp.modules.systemlog.service.SystemLogService;
import com.jlpp.modules.user.model.AdminUser;
import com.jlpp.modules.user.model.AdminUserForm;
import com.jlpp.modules.user.repository.AdminUserRepository;

@Service
public class AdminUserService {

    private final AdminUserRepository userRepository;
    private final SystemLogService systemLogService;

    public AdminUserService(AdminUserRepository userRepository, SystemLogService systemLogService) {
        this.userRepository = userRepository;
        this.systemLogService = systemLogService;
        this.systemLogService.record("System", "AdminUserService", "Đã khởi tạo dữ liệu người dùng demo trong bộ nhớ.");
    }

    public List<AdminUser> findAll() {
        return userRepository.findAll();
    }

    public Optional<AdminUser> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public long count() {
        return userRepository.count();
    }

    public synchronized AdminUser update(Long userId, AdminUserForm form) {
        AdminUser current = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));

        userRepository.findByEmailIgnoreCase(form.getEmail())
                .filter(existing -> !existing.id().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng bởi tài khoản khác.");
                });

        AdminUser updated = new AdminUser(
                current.id(),
                form.getFullName(),
                form.getEmail(),
                form.getPhone(),
                form.getRole(),
                form.getStatus(),
                current.createdAt() == null ? LocalDateTime.now() : current.createdAt());

        AdminUser saved = userRepository.save(updated);
        systemLogService.record("Admin", "AdminUserService",
                "Đã cập nhật tài khoản người dùng #" + saved.id() + " (" + saved.email() + ").");
        return saved;
    }
}
