package com.jeps.modules.payment.repository;

import com.jeps.modules.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    boolean existsByGatewayTransactionId(String gatewayTransactionId);

    Optional<PaymentTransaction> findByGatewayTransactionId(String gatewayTransactionId);

    @Query("""
            SELECT t FROM PaymentTransaction t
            WHERE t.payment.id = :paymentId AND t.amountIn = :amount
            ORDER BY t.createdAt ASC
            """)
    List<PaymentTransaction> findByPaidAmount(
            @Param("paymentId") Long paymentId, @Param("amount") BigDecimal amount);

    Optional<PaymentTransaction> findFirstByPayment_IdOrderByCreatedAtAsc(Long paymentId);

    Optional<PaymentTransaction> findFirstByPayment_IdOrderByCreatedAtDesc(Long paymentId);

    long countByPayment_Id(Long paymentId);
}
