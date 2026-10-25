package com.jlpp.modules.admin.service;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.admin.dto.UserUpdateRequest;
import com.jlpp.modules.admin.entity.AdminSystemLog;
import com.jlpp.modules.admin.entity.AdminUser;
import com.jlpp.modules.admin.repository.AdminSystemLogRepository;
import com.jlpp.modules.admin.repository.AdminUserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AdminService {

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final String INACTIVE_STATUS = "INACTIVE";

    private final AdminUserRepository userRepository;
    private final AdminSystemLogRepository systemLogRepository;

    public AdminService(
            AdminUserRepository userRepository,
            AdminSystemLogRepository systemLogRepository) {
        this.userRepository = userRepository;
        this.systemLogRepository = systemLogRepository;
    }

    @Transactional(readOnly = true)
    public long getTotalUsers() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long getActiveUsers() {
        return userRepository.countByDisplayStatus(ACTIVE_STATUS);
    }

    @Transactional(readOnly = true)
    public long getInactiveUsers() {
        return userRepository.countByDisplayStatus(INACTIVE_STATUS);
    }

    @Transactional(readOnly = true)
    public Page<AdminUser> searchUsers(String keyword, String role, String status, Pageable pageable) {
        String normalizedKeyword = normalizeFilter(keyword);
        String normalizedRole = normalizeFilter(role);
        String normalizedStatus = normalizeFilter(status);

        Specification<AdminUser> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (normalizedRole != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(root.get("role")), normalizedRole.toUpperCase(Locale.ROOT)));
            }
            if (normalizedStatus != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(criteriaBuilder.coalesce(
                                root.get("accountStatus"), root.get("legacyStatus"))),
                        normalizedStatus.toUpperCase(Locale.ROOT)));
            }
            if (normalizedKeyword != null) {
                String pattern = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("emailAddress")), pattern)));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        return userRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public AdminUser getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
    }

    @Transactional
    public void updateUser(Long userId, UserUpdateRequest request, String ipAddress) {
        AdminUser user = getUser(userId);
        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        if (userRepository.existsByUsernameIgnoreCaseAndUserIdNot(username, userId)) {
            throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng.");
        }
        if (userRepository.existsByEmailIgnoreCaseAndUserIdNot(email, userId)) {
            throw new IllegalArgumentException("Email đã được sử dụng.");
        }
        if (userRepository.existsByEmailAddressIgnoreCaseAndUserIdNot(email, userId)) {
            throw new IllegalArgumentException("Email đã được sử dụng.");
        }

        user.setFullName(request.getFullName().trim());
        user.setUsername(username);
        user.setEmail(email);
        user.setEmailAddress(email);
        user.setRole(request.getRole().name());
        user.setAccountStatus(normalizeStatus(request.getAccountStatus()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        AdminSystemLog log = new AdminSystemLog();
        log.setLogType("ADMIN");
        log.setActionPerformed("USER_UPDATED");
        log.setIpAddress(ipAddress);
        log.setTimestamp(Instant.now());
        log.setDetails("Updated user account #" + userId);
        systemLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AdminSystemLog> getSystemLogs(Pageable pageable) {
        return systemLogRepository.findAll(pageable);
    }

    public RoleEnum[] getAvailableRoles() {
        return RoleEnum.values();
    }

    public String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!Objects.equals(normalized, ACTIVE_STATUS) && !Objects.equals(normalized, INACTIVE_STATUS)) {
            throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ.");
        }
        return normalized;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
