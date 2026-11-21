package com.jlpp.modules.payment.repository;

import com.jlpp.modules.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    boolean existsByGatewayTransactionId(String gatewayTransactionId);

    Optional<PaymentTransaction> findByGatewayTransactionId(String gatewayTransactionId);

    Optional<PaymentTransaction> findFirstByPayment_IdOrderByCreatedAtAsc(Long paymentId);

    Optional<PaymentTransaction> findFirstByPayment_IdOrderByCreatedAtDesc(Long paymentId);

    long countByPayment_Id(Long paymentId);
}
