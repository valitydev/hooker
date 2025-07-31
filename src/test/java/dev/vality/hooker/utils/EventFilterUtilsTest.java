package dev.vality.hooker.utils;

import dev.vality.damsel.webhooker.EventFilter;
import dev.vality.hooker.dao.WebhookAdditionalFilter;
import dev.vality.hooker.model.EventType;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by inalarsanukaev on 10.04.17.
 */
class EventFilterUtilsTest {

    @Test
    void getEventFilterByCode() {
        assertEquals(8, getEventFilter().getInvoice().getTypes().size());
    }

    private EventFilter getEventFilter() {
        Collection<WebhookAdditionalFilter> eventTypeCodeSet = new HashSet<>();
        eventTypeCodeSet.add(WebhookAdditionalFilter.builder().eventType(EventType.INVOICE_CREATED).build());
        eventTypeCodeSet.add(WebhookAdditionalFilter.builder().eventType(EventType.INVOICE_PAYMENT_STARTED).shopId("77")
                .build());
        eventTypeCodeSet
                .add(WebhookAdditionalFilter.builder().eventType(EventType.INVOICE_PAYMENT_STATUS_CHANGED).build());
        eventTypeCodeSet.add(WebhookAdditionalFilter.builder().eventType(EventType.INVOICE_STATUS_CHANGED).build());
        eventTypeCodeSet
                .add(WebhookAdditionalFilter.builder().eventType(EventType.INVOICE_PAYMENT_REFUND_STARTED).build());
        eventTypeCodeSet
                .add(WebhookAdditionalFilter.builder().eventType(EventType.INVOICE_PAYMENT_REFUND_STATUS_CHANGED)
                        .build());
        eventTypeCodeSet
                .add(WebhookAdditionalFilter.builder()
                        .eventType(EventType.INVOICE_PAYMENT_USER_INTERACTION_CHANGE_REQUESTED)
                        .build());
        eventTypeCodeSet
                .add(WebhookAdditionalFilter.builder()
                        .eventType(EventType.INVOICE_PAYMENT_USER_INTERACTION_CHANGE_COMPLETED)
                        .build());
        return EventFilterUtils.getEventFilter(eventTypeCodeSet);
    }

    @Test
    void getWebhookAdditionalFilter() {
        assertEquals(8, EventFilterUtils.getWebhookAdditionalFilter(getEventFilter()).size());
    }

}
