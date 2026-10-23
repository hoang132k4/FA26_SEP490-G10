package com.jlpp.modules.user.repository;

import java.util.List;
import java.util.Optional;

import com.jlpp.modules.user.model.AdminUser;

public interface AdminUserRepository {

    List<AdminUser> findAll();

    Optional<AdminUser> findById(Long id);

    Optional<AdminUser> findByEmailIgnoreCase(String email);

    AdminUser save(AdminUser user);

    long count();
}
