package dev.vality.hooker.handler.invoicing;

import dev.vality.damsel.domain.Invoice;
import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.geck.filter.Filter;
import dev.vality.geck.filter.PathConditionFilter;
import dev.vality.geck.filter.condition.IsNullCondition;
import dev.vality.geck.filter.rule.PathConditionRule;
import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.model.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InvoiceCreatedMapper extends AbstractInvoiceEventMapper {

    private EventType eventType = EventType.INVOICE_CREATED;

    private Filter filter =
            new PathConditionFilter(new PathConditionRule(eventType.getThriftPath(), new IsNullCondition().not()));

    @Override
    @Transactional
    public InvoicingMessage buildEvent(InvoiceChange ic, EventInfo eventInfo) throws DaoException {
        Invoice invoiceOrigin = ic.getInvoiceCreated().getInvoice();
        InvoicingMessage message = new InvoicingMessage();
        message.setEventTime(eventInfo.getEventCreatedAt());
        message.setSequenceId(eventInfo.getSequenceId());
        message.setChangeId(eventInfo.getChangeId());
        message.setType(InvoicingMessageEnum.INVOICE);
        message.setPartyId(invoiceOrigin.getOwnerId());
        message.setEventType(eventType);
        message.setSourceId(invoiceOrigin.getId());
        message.setShopId(invoiceOrigin.getShopId());
        message.setInvoiceStatus(InvoiceStatusEnum.lookup(invoiceOrigin.getStatus().getSetField().getFieldName()));
        return message;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

}
