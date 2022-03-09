package dev.vality.hooker.handler.invoicing;

import dev.vality.damsel.payment_processing.InvoiceChange;
import dev.vality.hooker.dao.InvoicingMessageDao;
import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.exception.NotFoundException;
import dev.vality.hooker.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class NeedReadInvoiceEventMapper extends AbstractInvoiceEventMapper {

    private final InvoicingMessageDao messageDao;

    @Override
    protected InvoicingMessage buildEvent(InvoiceChange ic, EventInfo eventInfo) throws DaoException {
        InvoicingMessage message;
        InvoicingMessageKey messageKey = getMessageKey(eventInfo.getSourceId(), ic);
        try {
            message = messageDao.getInvoicingMessage(messageKey);
            message = message.copy();
        } catch (NotFoundException e) {
            log.warn(e.getMessage());
            return null;
        }
        message.setEventType(getEventType());
        message.setType(getMessageType());
        message.setEventTime(eventInfo.getEventCreatedAt());
        message.setSequenceId(eventInfo.getSequenceId());
        message.setChangeId(eventInfo.getChangeId());
        modifyMessage(ic, message);
        return message;
    }

    protected abstract InvoicingMessageEnum getMessageType();

    protected abstract InvoicingMessageKey getMessageKey(String invoiceId, InvoiceChange ic)
            throws NotFoundException, DaoException;

    protected abstract EventType getEventType();

    protected abstract void modifyMessage(InvoiceChange ic, InvoicingMessage message);
}
