package dev.vality.hooker.service;

import dev.vality.damsel.payment_processing.Customer;
import dev.vality.damsel.payment_processing.CustomerManagementSrv;
import dev.vality.damsel.payment_processing.CustomerNotFound;
import dev.vality.hooker.configuration.meta.UserIdentityIdExtensionKit;
import dev.vality.hooker.configuration.meta.UserIdentityRealmExtensionKit;
import dev.vality.hooker.converter.CustomerBindingConverter;
import dev.vality.hooker.converter.CustomerConverter;
import dev.vality.hooker.exception.NotFoundException;
import dev.vality.hooker.exception.RemoteHostException;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.utils.HellgateUtils;
import dev.vality.hooker.utils.TimeUtils;
import dev.vality.swag_webhook_events.model.CustomerBindingFailed;
import dev.vality.swag_webhook_events.model.CustomerBindingStarted;
import dev.vality.swag_webhook_events.model.CustomerBindingSucceeded;
import dev.vality.swag_webhook_events.model.CustomerCreated;
import dev.vality.swag_webhook_events.model.CustomerDeleted;
import dev.vality.swag_webhook_events.model.CustomerReady;
import dev.vality.swag_webhook_events.model.Event;
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
                        return customerClient.get(message.getSourceId(),
                                HellgateUtils.getEventRange(message.getSequenceId().intValue()));
                    }
            ).call();

            return resolveEvent(message, customer)
                    .eventID(message.getEventId().intValue())
                    .occuredAt(TimeUtils.toOffsetDateTime(message.getEventTime()))
                    .topic(Event.TopicEnum.CUSTOMERSTOPIC);
        } catch (CustomerNotFound e) {
            throw new NotFoundException("Customer not found, invoiceId=" + message.getSourceId());
        } catch (Exception e) {
            throw new RemoteHostException(e);
        }
    }

    private void addWoodyContext() {
        ContextUtils.setCustomMetadataValue(UserIdentityIdExtensionKit.KEY, "hooker");
        ContextUtils.setCustomMetadataValue(UserIdentityRealmExtensionKit.KEY, "service");
    }

    private Event resolveEvent(CustomerMessage message, Customer customer) {
        return switch (message.getEventType()) {
            case CUSTOMER_CREATED -> new CustomerCreated()
                    .customer(customerConverter.convert(customer))
                    .eventType(Event.EventTypeEnum.CUSTOMERCREATED);
            case CUSTOMER_DELETED -> new CustomerDeleted()
                    .customer(customerConverter.convert(customer))
                    .eventType(Event.EventTypeEnum.CUSTOMERDELETED);
            case CUSTOMER_READY -> new CustomerReady()
                    .customer(customerConverter.convert(customer))
                    .eventType(Event.EventTypeEnum.CUSTOMERREADY);
            case CUSTOMER_BINDING_STARTED -> new CustomerBindingStarted()
                    .customer(customerConverter.convert(customer))
                    .binding(customerBindingConverter.convert(extractBinding(message, customer)))
                    .eventType(Event.EventTypeEnum.CUSTOMERBINDINGSTARTED);
            case CUSTOMER_BINDING_SUCCEEDED -> new CustomerBindingSucceeded()
                    .customer(customerConverter.convert(customer))
                    .binding(customerBindingConverter.convert(extractBinding(message, customer)))
                    .eventType(Event.EventTypeEnum.CUSTOMERBINDINGSUCCEEDED);
            case CUSTOMER_BINDING_FAILED -> new CustomerBindingFailed()
                    .customer(customerConverter.convert(customer))
                    .binding(customerBindingConverter.convert(extractBinding(message, customer)))
                    .eventType(Event.EventTypeEnum.CUSTOMERBINDINGFAILED);
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
