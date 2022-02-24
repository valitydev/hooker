package dev.vality.hooker.handler.poller.invoicing;

import dev.vality.damsel.domain.InvoicePaymentRefund;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.damsel.payment_processing.InvoicePaymentRefundCreated;
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
public class InvoicePaymentRefundStartedMapper extends NeedReadInvoiceEventMapper {

    private EventType eventType = EventType.INVOICE_PAYMENT_REFUND_STARTED;

    private Filter filter =
            new PathConditionFilter(new PathConditionRule(eventType.getThriftPath(), new IsNullCondition().not()));

    public InvoicePaymentRefundStartedMapper(InvoicingMessageDao messageDao) {
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
                .type(InvoicingMessageEnum.PAYMENT)
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
        InvoicePaymentRefundCreated refundCreated =
                ic.getInvoicePaymentChange().getPayload().getInvoicePaymentRefundChange()
                        .getPayload().getInvoicePaymentRefundCreated();
        InvoicePaymentRefund refundOrigin = refundCreated.getRefund();
        message.setRefundId(refundOrigin.getId());
        message.setRefundStatus(RefundStatusEnum.lookup(refundOrigin.getStatus().getSetField().getFieldName()));
    }
}
