package dev.vality.hooker.handler.invoicing;

import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.handler.Mapper;
import dev.vality.hooker.model.EventInfo;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.InvoicingMessageKey;

import java.util.Map;

public abstract class AbstractInvoiceEventMapper implements Mapper<InvoiceChange, InvoicingMessage> {

    @Override
    public InvoicingMessage handle(InvoiceChange ic, EventInfo eventInfo) throws DaoException {
        return buildEvent(ic, eventInfo);
    }

    protected abstract InvoicingMessage buildEvent(InvoiceChange ic, EventInfo eventInfo)
            throws DaoException;
}