package com.jeps.modules.payment.entity;

import com.jeps.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_transactions", indexes = {
        @Index(name = "idx_payment_transactions_gateway_txn", columnList = "gateway_transaction_id"),
        @Index(name = "idx_payment_transactions_payment_id", columnList = "payment_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_transactions_gateway_txn", columnNames = "gateway_transaction_id")
})
public class PaymentTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "gateway", length = 50)
    @Builder.Default
    private String gateway = "SEPAY";

    @Column(name = "gateway_transaction_id", nullable = false, unique = true, length = 100)
    private String gatewayTransactionId;

    @Column(name = "bank_brand_name", length = 100)
    private String bankBrandName;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "amount_in", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal amountIn = BigDecimal.ZERO;

    @Column(name = "amount_out", precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal amountOut = BigDecimal.ZERO;

    @Column(name = "transaction_content", columnDefinition = "TEXT")
    private String transactionContent;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "webhook_payload", columnDefinition = "jsonb")
    private Map<String, Object> webhookPayload;
}
