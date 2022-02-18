package dev.vality.hooker.handler.poller.customer;

import dev.vality.damsel.payment_processing.CustomerChange;
import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.handler.Mapper;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.EventInfo;

public abstract class AbstractCustomerEventMapper implements Mapper<CustomerChange, CustomerMessage> {

    @Override
    public CustomerMessage handle(CustomerChange c, EventInfo eventInfo) throws DaoException {
        return buildEvent(c, eventInfo);
    }

    protected abstract CustomerMessage buildEvent(CustomerChange cc, EventInfo eventInfo) throws DaoException;
}
