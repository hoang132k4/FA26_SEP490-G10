package com.jeps.modules.payment.service;

import com.jeps.modules.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentExpiryService {

    private final PaymentRepository paymentRepository;

    @Scheduled(fixedDelayString = "${payment.expiry-sweep-interval-ms:30000}")
    @Transactional
    public void expirePendingPayments() {
        paymentRepository.expirePendingPaymentsBefore(LocalDateTime.now());
    }

    @Transactional
    public int expirePendingPaymentsBefore(LocalDateTime cutoff) {
        return paymentRepository.expirePendingPaymentsBefore(cutoff);
    }
}
