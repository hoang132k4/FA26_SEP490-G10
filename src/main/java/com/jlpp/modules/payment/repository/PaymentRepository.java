package com.jlpp.modules.payment.repository;

import com.jlpp.modules.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderCode(String orderCode);

    Optional<Payment> findByTransferContent(String transferContent);

    List<Payment> findByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(
            Long userId, Long courseId, String status);

    Optional<Payment> findFirstByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(
            Long userId, Long courseId, String status);

    @Query("SELECT p.user.id FROM Payment p WHERE p.transferContent = :transferContent")
    Optional<Long> findUserIdByTransferContent(@Param("transferContent") String transferContent);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.transferContent = :transferContent")
    Optional<Payment> findByTransferContentForUpdate(@Param("transferContent") String transferContent);

    @Query("SELECT p FROM Payment p JOIN FETCH p.user u JOIN FETCH p.course c WHERE p.id = :paymentId")
    Optional<Payment> findByIdWithUserAndCourse(@Param("paymentId") Long paymentId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.user u JOIN FETCH p.course c WHERE p.id = :paymentId AND u.id = :userId")
    Optional<Payment> findByIdAndUserIdWithCourse(@Param("paymentId") Long paymentId, @Param("userId") Long userId);
}
