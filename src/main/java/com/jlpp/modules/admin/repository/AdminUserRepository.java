package com.jlpp.modules.admin.repository;

import com.jlpp.modules.admin.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long>, JpaSpecificationExecutor<AdminUser> {

    boolean existsByUsernameIgnoreCaseAndUserIdNot(String username, Long userId);

    boolean existsByEmailIgnoreCaseAndUserIdNot(String email, Long userId);

    boolean existsByEmailAddressIgnoreCaseAndUserIdNot(String emailAddress, Long userId);

    @Query("""
            select count(u) from AdminUser u
            where upper(coalesce(u.accountStatus, u.legacyStatus)) = upper(:status)
            """)
    long countByDisplayStatus(@Param("status") String status);
}
