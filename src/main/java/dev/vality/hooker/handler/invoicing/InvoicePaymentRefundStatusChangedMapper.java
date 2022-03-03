package dev.vality.hooker.handler.invoicing;

import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.geck.filter.Filter;
import dev.vality.geck.filter.PathConditionFilter;
import dev.vality.geck.filter.condition.IsNullCondition;
import dev.vality.geck.filter.rule.PathConditionRule;
import dev.vality.hooker.dao.InvoicingMessageDao;
import dev.vality.hooker.model.EventType;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.InvoicingMessageEnum;
import dev.vality.hooker.model.InvoicingMessageKey;
import dev.vality.hooker.model.RefundStatusEnum;
import org.springframework.stereotype.Component;

@Component
public class InvoicePaymentRefundStatusChangedMapper extends NeedReadInvoiceEventMapper {

    private EventType eventType = EventType.INVOICE_PAYMENT_REFUND_STATUS_CHANGED;

    private Filter filter =
            new PathConditionFilter(new PathConditionRule(eventType.getThriftPath(), new IsNullCondition().not()));

    public InvoicePaymentRefundStatusChangedMapper(InvoicingMessageDao messageDao) {
        super(messageDao);
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    protected InvoicingMessageKey getMessageKey(String invoiceId, InvoiceChange ic) {
        return InvoicingMessageKey.builder()
                .invoiceId(invoiceId)
                .paymentId(ic.getInvoicePaymentChange().getId())
                .refundId(ic.getInvoicePaymentChange().getPayload().getInvoicePaymentRefundChange().getId())
                .type(InvoicingMessageEnum.REFUND)
                .build();
    }

    @Override
    protected InvoicingMessageEnum getMessageType() {
        return InvoicingMessageEnum.REFUND;
    }

    @Override
    protected EventType getEventType() {
        return eventType;
    }

    @Override
    protected void modifyMessage(InvoiceChange ic, InvoicingMessage message) {
        message.setRefundStatus(
                RefundStatusEnum.lookup(ic.getInvoicePaymentChange().getPayload().getInvoicePaymentRefundChange()
                        .getPayload().getInvoicePaymentRefundStatusChanged().getStatus().getSetField().getFieldName()));
    }
}
