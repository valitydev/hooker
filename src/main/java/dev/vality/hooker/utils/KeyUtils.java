package dev.vality.hooker.utils;

import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.InvoicingMessageKey;

public class KeyUtils {

    public static InvoicingMessageKey key(InvoicingMessage message) {
        return InvoicingMessageKey.builder()
                .invoiceId(message.getInvoiceId())
                .paymentId(message.getPaymentId())
                .refundId(message.getRefundId())
                .type(message.getType())
                .build();
    }
}

