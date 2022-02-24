package dev.vality.hooker.handler.poller.customer;

import dev.vality.damsel.payment_processing.CustomerChange;
import dev.vality.geck.filter.Filter;
import dev.vality.geck.filter.PathConditionFilter;
import dev.vality.geck.filter.condition.IsNullCondition;
import dev.vality.geck.filter.rule.PathConditionRule;
import dev.vality.hooker.dao.impl.CustomerDaoImpl;
import dev.vality.hooker.dao.impl.CustomerQueueDao;
import dev.vality.hooker.dao.impl.CustomerTaskDao;
import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.hooker.model.EventInfo;
import dev.vality.hooker.model.EventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Created by inalarsanukaev on 12.10.17.
 */
@Component
@RequiredArgsConstructor
public class CustomerCreatedHandler extends AbstractCustomerEventHandler {

    private final CustomerDaoImpl customerDao;
    private final CustomerQueueDao customerQueueDao;
    private final CustomerTaskDao customerTaskDao;
    private EventType eventType = EventType.CUSTOMER_CREATED;
    private Filter filter =
            new PathConditionFilter(new PathConditionRule(eventType.getThriftPath(), new IsNullCondition().not()));

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    protected void saveEvent(CustomerChange cc, EventInfo eventInfo) throws DaoException {
        dev.vality.damsel.payment_processing.CustomerCreated customerCreatedOrigin = cc.getCustomerCreated();
        CustomerMessage customerMessage = new CustomerMessage();
        customerMessage.setEventId(eventInfo.getEventId());
        customerMessage.setEventTime(eventInfo.getEventCreatedAt());
        customerMessage.setSequenceId(eventInfo.getSequenceId());
        customerMessage.setChangeId(eventInfo.getChangeId());
        customerMessage.setType(CustomerMessageEnum.CUSTOMER);
        customerMessage.setPartyId(customerCreatedOrigin.getOwnerId());
        customerMessage.setEventType(eventType);
        customerMessage.setCustomerId(customerCreatedOrigin.getCustomerId());
        customerMessage.setShopId(customerCreatedOrigin.getShopId());
        Long messageId = customerDao.create(customerMessage);
        if (messageId != null) {
            customerMessage.setId(messageId);
            customerQueueDao.createWithPolicy(messageId);
            customerTaskDao.create(messageId);
        }
    }
}
