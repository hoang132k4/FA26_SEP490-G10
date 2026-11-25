package com.jeps.modules.payment.dto;

public record SePayWebhookResult(Outcome outcome) {

    public enum Outcome {
        PROCESSED,
        PROCESSED_LATE,
        DUPLICATE,
        RECONCILIATION_REQUIRED,
        PAYMENT_NOT_FOUND,
        INVALID_PAYLOAD,
        INVALID_TRANSACTION_ID,
        AMOUNT_MISMATCH,
        ACCOUNT_MISMATCH,
        INVALID_DIRECTION,
        INVALID_TRANSACTION_DATE,
        PAYMENT_STATE_REJECTED
    }
}
