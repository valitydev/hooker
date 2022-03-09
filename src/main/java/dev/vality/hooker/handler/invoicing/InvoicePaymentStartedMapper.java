package dev.vality.hooker.handler.invoicing;

import dev.vality.damsel.domain.InvoicePayment;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.geck.filter.Filter;
import dev.vality.geck.filter.PathConditionFilter;
import dev.vality.geck.filter.condition.IsNullCondition;
import dev.vality.geck.filter.rule.PathConditionRule;
import dev.vality.hooker.dao.InvoicingMessageDao;
import dev.vality.hooker.model.*;
import org.springframework.stereotype.Component;

@Component
public class InvoicePaymentStartedMapper extends NeedReadInvoiceEventMapper {

    private EventType eventType = EventType.INVOICE_PAYMENT_STARTED;

    private Filter filter =
            new PathConditionFilter(new PathConditionRule(eventType.getThriftPath(), new IsNullCondition().not()));

    public InvoicePaymentStartedMapper(InvoicingMessageDao messageDao) {
        super(messageDao);
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    protected InvoicingMessageEnum getMessageType() {
        return InvoicingMessageEnum.PAYMENT;
    }

    @Override
    protected EventType getEventType() {
        return eventType;
    }

    @Override
    protected void modifyMessage(InvoiceChange ic, InvoicingMessage message) {
        InvoicePayment paymentOrigin =
                ic.getInvoicePaymentChange().getPayload().getInvoicePaymentStarted().getPayment();
        message.setPaymentId(paymentOrigin.getId());
        message.setPaymentStatus(PaymentStatusEnum.lookup(paymentOrigin.getStatus().getSetField().getFieldName()));
    }

    @Override
    protected InvoicingMessageKey getMessageKey(String invoiceId, InvoiceChange ic) {
        return InvoicingMessageKey.builder()
                .invoiceId(invoiceId)
                .type(InvoicingMessageEnum.INVOICE)
                .build();
    }
}
