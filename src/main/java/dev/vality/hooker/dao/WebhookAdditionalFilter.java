package dev.vality.hooker.dao;

import dev.vality.hooker.model.EventType;
import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WebhookAdditionalFilter {
    private EventType eventType;
    private String shopId;
    private String invoiceStatus;
    private String invoicePaymentStatus;
    private String invoicePaymentRefundStatus;
}
