package dev.vality.hooker.handler.poller.customer;

import dev.vality.geck.filter.Filter;
import dev.vality.geck.filter.PathConditionFilter;
import dev.vality.geck.filter.condition.IsNullCondition;
import dev.vality.geck.filter.rule.PathConditionRule;
import dev.vality.hooker.dao.impl.CustomerDaoImpl;
import dev.vality.hooker.dao.impl.CustomerQueueDao;
import dev.vality.hooker.dao.impl.CustomerTaskDao;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.hooker.model.EventType;
import org.springframework.stereotype.Component;

/**
 * Created by inalarsanukaev on 12.10.17.
 */
@Component
public class CustomerReadyHandler extends NeedReadCustomerEventHandler {

    private EventType eventType = EventType.CUSTOMER_READY;

    private Filter filter =
            new PathConditionFilter(new PathConditionRule(eventType.getThriftPath(), new IsNullCondition().not()));

    public CustomerReadyHandler(CustomerDaoImpl customerDao,
                                CustomerQueueDao customerQueueDao,
                                CustomerTaskDao customerTaskDao) {
        super(customerDao, customerQueueDao, customerTaskDao);
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    protected CustomerMessageEnum getMessageType() {
        return CustomerMessageEnum.CUSTOMER;
    }

    @Override
    protected EventType getEventType() {
        return eventType;
    }
}
