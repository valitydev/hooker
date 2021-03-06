package dev.vality.hooker.handler.customer;

import dev.vality.damsel.payment_processing.CustomerChange;
import dev.vality.geck.filter.Filter;
import dev.vality.geck.filter.PathConditionFilter;
import dev.vality.geck.filter.condition.IsNullCondition;
import dev.vality.geck.filter.rule.PathConditionRule;
import dev.vality.hooker.exception.DaoException;
import dev.vality.hooker.handler.Mapper;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.hooker.model.EventInfo;
import dev.vality.hooker.model.EventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerCreatedMapper implements Mapper<CustomerChange, CustomerMessage> {

    private EventType eventType = EventType.CUSTOMER_CREATED;
    private Filter filter =
            new PathConditionFilter(new PathConditionRule(eventType.getThriftPath(), new IsNullCondition().not()));

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public CustomerMessage map(CustomerChange cc, EventInfo eventInfo) throws DaoException {
        dev.vality.damsel.payment_processing.CustomerCreated customerCreatedOrigin = cc.getCustomerCreated();
        CustomerMessage customerMessage = new CustomerMessage();
        customerMessage.setEventTime(eventInfo.getEventCreatedAt());
        customerMessage.setSequenceId(eventInfo.getSequenceId());
        customerMessage.setChangeId(eventInfo.getChangeId());
        customerMessage.setType(CustomerMessageEnum.CUSTOMER);
        customerMessage.setPartyId(customerCreatedOrigin.getOwnerId());
        customerMessage.setEventType(eventType);
        customerMessage.setSourceId(customerCreatedOrigin.getCustomerId());
        customerMessage.setShopId(customerCreatedOrigin.getShopId());
        return customerMessage;
    }
}
