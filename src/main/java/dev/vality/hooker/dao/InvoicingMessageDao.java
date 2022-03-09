package dev.vality.hooker.dao;

import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.exception.NotFoundException;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.InvoicingMessageKey;

public interface InvoicingMessageDao extends MessageDao<InvoicingMessage> {
    InvoicingMessage getInvoicingMessage(InvoicingMessageKey key) throws NotFoundException, DaoException;
}
