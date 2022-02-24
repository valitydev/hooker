package dev.vality.hooker.handler.poller.customer;

import dev.vality.damsel.payment_processing.CustomerChange;
import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.handler.Handler;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.EventInfo;

/**
 * Created by inalarsanukaev on 07.04.17.
 */
public abstract class AbstractCustomerEventHandler implements Handler<CustomerChange, CustomerMessage> {

    @Override
    public void handle(CustomerChange c, EventInfo eventInfo) throws DaoException {
        saveEvent(c, eventInfo);
    }

    protected abstract void saveEvent(CustomerChange cc, EventInfo eventInfo) throws DaoException;
}
