package dev.vality.hooker.handler.customer;

import dev.vality.damsel.payment_processing.CustomerChange;
import dev.vality.hooker.dao.impl.CustomerDaoImpl;
import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.handler.Mapper;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.hooker.model.EventInfo;
import dev.vality.hooker.model.EventType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class NeedReadCustomerEventMapper implements Mapper<CustomerChange, CustomerMessage> {

    protected final CustomerDaoImpl customerDao;

    @Override
    public CustomerMessage map(CustomerChange cc, EventInfo eventInfo) throws DaoException {
        CustomerMessage message = getCustomerMessage(eventInfo.getSourceId());
        if (message == null) {
            throw new DaoException("CustomerMessage for customer with id " + eventInfo.getSourceId() + " not exist");
        }
        message.setEventType(getEventType());
        message.setType(getMessageType());
        message.setEventTime(eventInfo.getEventCreatedAt());
        message.setSequenceId(eventInfo.getSequenceId());
        message.setChangeId(eventInfo.getChangeId());
        modifyMessage(cc, message);
        return message;
    }

    protected CustomerMessage getCustomerMessage(String customerId) {
        return customerDao.getAny(customerId, getMessageType());
    }

    protected abstract CustomerMessageEnum getMessageType();

    protected abstract EventType getEventType();

    protected void modifyMessage(CustomerChange cc, CustomerMessage message) {
    }
}
