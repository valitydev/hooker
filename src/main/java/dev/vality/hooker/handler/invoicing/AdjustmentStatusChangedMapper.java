package dev.vality.hooker.handler.invoicing;

import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.geck.filter.Filter;
import dev.vality.geck.filter.PathConditionFilter;
import dev.vality.geck.filter.condition.IsNullCondition;
import dev.vality.geck.filter.rule.PathConditionRule;
import dev.vality.hooker.dao.InvoicingMessageDao;
import dev.vality.hooker.model.*;
import dev.vality.hooker.service.HellgateInvoicingService;
import org.springframework.stereotype.Component;

@Component
public class AdjustmentStatusChangedMapper extends NeedReadInvoiceEventMapper {

    private static final EventType EVENT_TYPE = EventType.INVOICE_PAYMENT_STATUS_CHANGED;
    private static final String ADJUSTMENT_STATUS_CHANGED_PATH = "invoice_payment_change.payload." +
            "invoice_payment_adjustment_change.payload.invoice_payment_adjustment_status_changed." +
            "status.captured";
    private static final Filter FILTER = new PathConditionFilter(
            new PathConditionRule(ADJUSTMENT_STATUS_CHANGED_PATH, new IsNullCondition().not())
    );
    private final HellgateInvoicingService<InvoicingMessage> invoicingEventService;

    public AdjustmentStatusChangedMapper(InvoicingMessageDao messageDao,
                                         HellgateInvoicingService<InvoicingMessage> invoicingEventService) {
        super(messageDao);
        this.invoicingEventService = invoicingEventService;
    }

    @Override
    public Filter getFilter() {
        return FILTER;
    }

    @Override
    protected InvoicingMessageKey getMessageKey(String invoiceId, InvoiceChange ic) {
        return InvoicingMessageKey.builder()
                .invoiceId(invoiceId)
                .paymentId(ic.getInvoicePaymentChange().getId())
                .type(InvoicingMessageEnum.PAYMENT)
                .build();
    }

    @Override
    protected InvoicingMessageEnum getMessageType() {
        return InvoicingMessageEnum.PAYMENT;
    }

    @Override
    protected EventType getEventType() {
        return EVENT_TYPE;
    }

    @Override
    protected void modifyMessage(InvoiceChange ic, InvoicingMessage message) {
        InvoicePayment invoicePayment = invoicingEventService.getPaymentByMessage(message);
        message.setPaymentStatus(
                PaymentStatusEnum.lookup(invoicePayment.getPayment().getStatus().getSetField().getFieldName())
        );
    }
}
