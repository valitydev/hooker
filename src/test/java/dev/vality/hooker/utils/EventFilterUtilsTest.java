package dev.vality.hooker.utils;

import dev.vality.damsel.webhooker.EventFilter;
import dev.vality.hooker.dao.WebhookAdditionalFilter;
import dev.vality.hooker.model.EventType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created by inalarsanukaev on 10.04.17.
 */
public class EventFilterUtilsTest {
    @Test
    public void getEventFilterByCode() throws Exception {
        Assert.assertEquals(getEventFilter().getInvoice().getTypes().size(), 6);
        Assert.assertEquals(getCustomerEventFilter().getCustomer().getTypes().size(), 6);
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
        return EventFilterUtils.getEventFilter(eventTypeCodeSet);
    }

    private EventFilter getCustomerEventFilter() {
        Collection<WebhookAdditionalFilter> eventTypeCodeSet = new HashSet<>();
        eventTypeCodeSet.add(WebhookAdditionalFilter.builder().eventType(EventType.CUSTOMER_CREATED).build());
        eventTypeCodeSet
                .add(WebhookAdditionalFilter.builder().eventType(EventType.CUSTOMER_DELETED).shopId("77").build());
        eventTypeCodeSet.add(WebhookAdditionalFilter.builder().eventType(EventType.CUSTOMER_READY).build());
        eventTypeCodeSet.add(WebhookAdditionalFilter.builder().eventType(EventType.CUSTOMER_BINDING_STARTED).build());
        eventTypeCodeSet.add(WebhookAdditionalFilter.builder().eventType(EventType.CUSTOMER_BINDING_SUCCEEDED).build());
        eventTypeCodeSet.add(WebhookAdditionalFilter.builder().eventType(EventType.CUSTOMER_BINDING_FAILED).build());
        return EventFilterUtils.getEventFilter(eventTypeCodeSet);
    }

    @Test
    public void getWebhookAdditionalFilter() throws Exception {
        Assert.assertEquals(EventFilterUtils.getWebhookAdditionalFilter(getEventFilter()).size(), 6);
        Assert.assertEquals(EventFilterUtils.getWebhookAdditionalFilter(getCustomerEventFilter()).size(), 6);
    }

}
