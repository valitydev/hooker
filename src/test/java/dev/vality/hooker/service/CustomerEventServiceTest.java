package dev.vality.hooker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.damsel.payment_processing.CustomerManagementSrv;
import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.hooker.model.EventType;
import dev.vality.hooker.utils.BuildUtils;
import dev.vality.swag_webhook_events.model.CustomerBindingSucceeded;
import dev.vality.swag_webhook_events.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@PostgresqlSpringBootITest
@SpringBootTest
class CustomerEventServiceTest {

    @MockitoBean
    private CustomerManagementSrv.Iface customerClient;

    @Autowired
    private CustomerEventService service;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        Mockito.when(customerClient.get(any(), any()))
                .thenReturn(BuildUtils.buildCustomer("customerId", "bindingId"));
    }

    @Test
    void testCustomerSucceeded() {
        CustomerMessage message = random(CustomerMessage.class);
        message.setType(CustomerMessageEnum.BINDING);
        message.setEventTime("2016-03-22T06:12:27Z");
        message.setEventType(EventType.CUSTOMER_BINDING_SUCCEEDED);
        message.setBindingId("bindingId");
        Event event = service.getEventByMessage(message);
        assertInstanceOf(CustomerBindingSucceeded.class, event);
        CustomerBindingSucceeded bindingSucceeded = (CustomerBindingSucceeded) event;
        assertEquals("customerId", bindingSucceeded.getCustomer().getId());
        assertEquals("bindingId", bindingSucceeded.getBinding().getId());
    }

    @Test
    void testJson() throws JsonProcessingException {
        CustomerMessage message = random(CustomerMessage.class);
        message.setType(CustomerMessageEnum.BINDING);
        message.setEventTime("2016-03-22T06:12:27Z");
        message.setEventType(EventType.CUSTOMER_BINDING_FAILED);
        message.setBindingId("bindingId");
        Event event = service.getEventByMessage(message);
        String json = objectMapper.writeValueAsString(event);
        assertTrue(json.contains("2016-03-22T06:12:27Z"));
    }
}
