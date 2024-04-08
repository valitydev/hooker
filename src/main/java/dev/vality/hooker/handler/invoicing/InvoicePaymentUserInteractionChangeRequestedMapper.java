package dev.vality.hooker.handler.invoicing;

import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.geck.filter.Filter;
import dev.vality.geck.filter.PathConditionFilter;
import dev.vality.geck.filter.condition.IsNullCondition;
import dev.vality.geck.filter.rule.PathConditionRule;
import dev.vality.hooker.converter.InvoiceChangeToUserInteractionConverter;
import dev.vality.hooker.dao.InvoicingMessageDao;
import dev.vality.hooker.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InvoicePaymentUserInteractionChangeRequestedMapper extends NeedReadInvoiceEventMapper {

    @Autowired
    private InvoiceChangeToUserInteractionConverter userInteractionConverter;
    
    private EventType eventType = EventType.INVOICE_PAYMENT_USER_INTERACTION_CHANGE_REQUESTED;

    private Filter filter =
            new PathConditionFilter(new PathConditionRule(eventType.getThriftPath(), new IsNullCondition().not()));

    public InvoicePaymentUserInteractionChangeRequestedMapper(InvoicingMessageDao messageDao) {
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
        return InvoicingMessageEnum.PAYMENT;
    }

    @Override
    protected EventType getEventType() {
        return eventType;
    }

    @Override
    protected void modifyMessage(InvoiceChange ic, InvoicingMessage message) {
        message.setUserInteraction(userInteractionConverter.convert(ic));
    }

}