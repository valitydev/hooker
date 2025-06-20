package dev.vality.hooker.service;

import dev.vality.damsel.payment_processing.Customer;
import dev.vality.damsel.payment_processing.CustomerManagementSrv;
import dev.vality.damsel.payment_processing.CustomerNotFound;
import dev.vality.damsel.payment_processing.EventRange;
import dev.vality.hooker.configuration.meta.UserIdentityIdExtensionKit;
import dev.vality.hooker.configuration.meta.UserIdentityRealmExtensionKit;
import dev.vality.hooker.converter.CustomerBindingConverter;
import dev.vality.hooker.converter.CustomerConverter;
import dev.vality.hooker.exception.NotFoundException;
import dev.vality.hooker.exception.RemoteHostException;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.utils.TimeUtils;
import dev.vality.swag_webhook_events.model.*;
import dev.vality.woody.api.flow.WFlow;
import dev.vality.woody.api.trace.ContextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerEventService implements EventService<CustomerMessage> {

    private final CustomerManagementSrv.Iface customerClient;
    private final CustomerConverter customerConverter;
    private final CustomerBindingConverter customerBindingConverter;
    private final WFlow woodyFlow = new WFlow();

    @Override
    public Event getEventByMessage(CustomerMessage message) {
        try {
            Customer customer = woodyFlow.createServiceFork(() -> {
                        addWoodyContext();
                        return customerClient.get(message.getSourceId(), getEventRange(message));
                    }
            ).call();

            return resolveEvent(message, customer)
                    .eventID(message.getId().intValue())
                    .occuredAt(TimeUtils.toOffsetDateTime(message.getEventTime()))
                    .topic(Event.TopicEnum.CUSTOMERS_TOPIC);
        } catch (CustomerNotFound e) {
            throw new NotFoundException("Customer not found, invoiceId=" + message.getSourceId());
        } catch (Exception e) {
            throw new RemoteHostException(e);
        }
    }

    private EventRange getEventRange(CustomerMessage message) {
        return new EventRange().setLimit(message.getSequenceId().intValue());
    }

    private void addWoodyContext() {
        ContextUtils.setCustomMetadataValue(UserIdentityIdExtensionKit.KEY, "hooker");
        ContextUtils.setCustomMetadataValue(UserIdentityRealmExtensionKit.KEY, "service");
    }

    private Event resolveEvent(CustomerMessage message, Customer customer) {
        return switch (message.getEventType()) {
            case CUSTOMER_CREATED -> new CustomerCreated()
                    .customer(customerConverter.convert(customer))
                    .eventType(Event.EventTypeEnum.CUSTOMER_CREATED);
            case CUSTOMER_DELETED -> new CustomerDeleted()
                    .customer(customerConverter.convert(customer))
                    .eventType(Event.EventTypeEnum.CUSTOMER_DELETED);
            case CUSTOMER_READY -> new CustomerReady()
                    .customer(customerConverter.convert(customer))
                    .eventType(Event.EventTypeEnum.CUSTOMER_READY);
            case CUSTOMER_BINDING_STARTED -> new CustomerBindingStarted()
                    .customer(customerConverter.convert(customer))
                    .binding(customerBindingConverter.convert(extractBinding(message, customer)))
                    .eventType(Event.EventTypeEnum.CUSTOMER_BINDING_STARTED);
            case CUSTOMER_BINDING_SUCCEEDED -> new CustomerBindingSucceeded()
                    .customer(customerConverter.convert(customer))
                    .binding(customerBindingConverter.convert(extractBinding(message, customer)))
                    .eventType(Event.EventTypeEnum.CUSTOMER_BINDING_SUCCEEDED);
            case CUSTOMER_BINDING_FAILED -> new CustomerBindingFailed()
                    .customer(customerConverter.convert(customer))
                    .binding(customerBindingConverter.convert(extractBinding(message, customer)))
                    .eventType(Event.EventTypeEnum.CUSTOMER_BINDING_FAILED);
            default -> throw new UnsupportedOperationException("Unknown event type " + message.getEventType());
        };
    }

    private dev.vality.damsel.payment_processing.CustomerBinding extractBinding(CustomerMessage message,
                                                                                  Customer customer) {
        return customer.getBindings().stream()
                .filter(b -> b.getId().equals(message.getBindingId()))
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException(String.format("Customer binding not found, customerId=%s, bindingId=%s",
                                message.getSourceId(), message.getBindingId())));
    }
}
