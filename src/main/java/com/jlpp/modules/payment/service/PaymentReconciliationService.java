package com.jlpp.modules.payment.service;

import com.jlpp.modules.payment.entity.PaymentTransaction;
import com.jlpp.modules.payment.security.SepayAccountNumberCryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private static final List<String> ACCOUNT_NUMBER_FIELDS =
            List.of("accountNumber", "bankAccountNumber", "accountNo");

    private final SepayAccountNumberCryptoService accountNumberCryptoService;

    public String decryptReceiverAccountNumber(PaymentTransaction transaction) {
        if (transaction == null || transaction.getWebhookPayload() == null) {
            throw new IllegalArgumentException("Stored webhook account data is unavailable.");
        }

        for (Map.Entry<String, Object> entry : transaction.getWebhookPayload().entrySet()) {
            if (ACCOUNT_NUMBER_FIELDS.stream().noneMatch(entry.getKey()::equalsIgnoreCase)) {
                continue;
            }
            if (!(entry.getValue() instanceof String encryptedAccountNumber)) {
                throw new IllegalArgumentException("Stored webhook account data is invalid.");
            }
            return accountNumberCryptoService.decrypt(encryptedAccountNumber);
        }

        throw new IllegalArgumentException("Stored webhook account data is unavailable.");
    }
}
