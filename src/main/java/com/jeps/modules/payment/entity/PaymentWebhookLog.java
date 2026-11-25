package com.jeps.modules.payment.entity;

import com.jeps.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_webhook_logs")
public class PaymentWebhookLog extends BaseEntity {

    @Column(name = "gateway", length = 50)
    @Builder.Default
    private String gateway = "SEPAY";

    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", columnDefinition = "jsonb")
    private Map<String, Object> requestPayload;

    @Column(name = "processing_status", nullable = false, length = 30)
    @Builder.Default
    private String processingStatus = "RECEIVED";

    @Column(name = "processing_message", columnDefinition = "TEXT")
    private String processingMessage;

    @Column(name = "received_at")
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
