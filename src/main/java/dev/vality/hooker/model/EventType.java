package dev.vality.hooker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@SuppressWarnings("LineLength")
@Getter
@AllArgsConstructor
public enum EventType {
    INVOICE_CREATED("invoice_created"),
    INVOICE_STATUS_CHANGED("invoice_status_changed"),
    INVOICE_PAYMENT_STARTED("invoice_payment_change.payload.invoice_payment_started"),
    INVOICE_PAYMENT_STATUS_CHANGED("invoice_payment_change.payload.invoice_payment_status_changed"),
    INVOICE_PAYMENT_REFUND_STARTED(
            "invoice_payment_change.payload.invoice_payment_refund_change.payload.invoice_payment_refund_created"),
    INVOICE_PAYMENT_REFUND_STATUS_CHANGED(
            "invoice_payment_change.payload.invoice_payment_refund_change.payload.invoice_payment_refund_status_changed"),
    INVOICE_PAYMENT_CASH_FLOW_CHANGED("invoice_payment_change.payload.invoice_payment_cash_flow_changed"),
    INVOICE_PAYMENT_CASH_CHANGED("invoice_payment_change.payload.invoice_payment_cash_changed"),

    INVOICE_PAYMENT_USER_INTERACTION_CHANGE_REQUESTED(
            "invoice_payment_change.payload.invoice_payment_session_change.payload.session_interaction_changed.status.requested"),
    INVOICE_PAYMENT_USER_INTERACTION_CHANGE_COMPLETED(
            "invoice_payment_change.payload.invoice_payment_session_change.payload.session_interaction_changed.status.completed");

    private String thriftPath;

    public boolean isInvoiceEvent() {
        return this.name().startsWith("INVOICE");
    }
}
