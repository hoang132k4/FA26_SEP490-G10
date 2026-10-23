package com.jlpp.modules.user.repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.user.model.AdminUser;
import com.jlpp.modules.user.model.AdminUserStatus;

@Repository
public class InMemoryAdminUserRepository implements AdminUserRepository {

    private final ConcurrentHashMap<Long, AdminUser> users = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(4);

    public InMemoryAdminUserRepository() {
        LocalDateTime now = LocalDateTime.now();
        users.put(1L, new AdminUser(1L, "Nguyễn Minh Anh", "minhanh@example.test", "0901234567",
                RoleEnum.LEARNER, AdminUserStatus.ACTIVE, now.minusDays(30)));
        users.put(2L, new AdminUser(2L, "Trần Thu Hà", "thuha@example.test", "0912345678",
                RoleEnum.MANAGER, AdminUserStatus.ACTIVE, now.minusDays(21)));
        users.put(3L, new AdminUser(3L, "Lê Hoàng Nam", "hoangnam@example.test", "",
                RoleEnum.LEARNER, AdminUserStatus.INACTIVE, now.minusDays(8)));
    }

    @Override
    public List<AdminUser> findAll() {
        return users.values().stream()
                .sorted(Comparator.comparing(AdminUser::id))
                .toList();
    }

    @Override
    public Optional<AdminUser> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<AdminUser> findByEmailIgnoreCase(String email) {
        String normalizedEmail = normalizeEmail(email);
        return users.values().stream()
                .filter(user -> normalizeEmail(user.email()).equals(normalizedEmail))
                .findFirst();
    }

    @Override
    public AdminUser save(AdminUser user) {
        Long id = user.id() == null ? nextId.getAndIncrement() : user.id();
        AdminUser saved = new AdminUser(id, user.fullName().trim(), normalizeEmail(user.email()),
                user.phone() == null ? "" : user.phone().trim(), user.role(), user.status(), user.createdAt());
        users.put(id, saved);
        return saved;
    }

    @Override
    public long count() {
        return users.size();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
