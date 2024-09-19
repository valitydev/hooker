package dev.vality.hooker.model;

import dev.vality.hooker.model.interaction.UserInteraction;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.BeanUtils;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
@ToString(callSuper = true)
public class InvoicingMessage extends Message {

    private InvoicingMessageEnum type;
    private InvoiceStatusEnum invoiceStatus;
    private String paymentId;
    private PaymentStatusEnum paymentStatus;
    private String refundId;
    private RefundStatusEnum refundStatus;
    private UserInteraction userInteraction;

    public boolean isInvoice() {
        return type == InvoicingMessageEnum.INVOICE;
    }

    public boolean isPayment() {
        return type == InvoicingMessageEnum.PAYMENT;
    }

    public boolean isRefund() {
        return type == InvoicingMessageEnum.REFUND;
    }

    public InvoicingMessage copy() {
        InvoicingMessage copied = new InvoicingMessage();
        BeanUtils.copyProperties(this, copied);
        return copied;
    }
}
