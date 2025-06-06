package dev.vality.hooker.listener;

import dev.vality.damsel.domain.ContactInfo;
import dev.vality.damsel.payment_processing.CustomerChange;
import dev.vality.damsel.payment_processing.CustomerCreated;
import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.dao.CustomerDao;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.msgpack.Value;
import dev.vality.sink.common.parser.impl.MachineEventParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@PostgresqlSpringBootITest
@SpringBootTest
class CustomerMachineEventHandlerTest {

    @Autowired
    private MachineEventHandler customerMachineEventHandler;

    @Autowired
    private CustomerDao customerDao;

    @MockitoBean
    private MachineEventParser<EventPayload> paymentEventPayloadMachineEventParser;

    private static MachineEvent createTestMachineEvent() {
        MachineEvent machineEvent = new MachineEvent();
        machineEvent.setSourceId("sourceId-1");
        machineEvent.setEventId(1L);
        machineEvent.setSourceNs("SourceNs");
        machineEvent.setCreatedAt(TypeUtil.temporalToString(LocalDateTime.now()));
        machineEvent.setFormatVersion(1);
        Value value = new Value();
        value.setStr("str");
        machineEvent.setData(value);
        return machineEvent;
    }

    private static EventPayload createTestCusomerEventPayload(String customerId) {
        EventPayload payload = new EventPayload();
        payload.setCustomerChanges(createCustomerChangeList(customerId));
        return payload;
    }

    private static List<CustomerChange> createCustomerChangeList(String customerId) {
        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setEmail("1@1.ru");
        contactInfo.setPhoneNumber("88005553535");

        CustomerCreated created = new CustomerCreated();
        created.setCustomerId(customerId);
        created.setOwnerId("PartyId");
        created.setShopId("ShopId");
        created.setMetadata(null);
        created.setContactInfo(contactInfo);
        created.setCreatedAt(TypeUtil.temporalToString(LocalDateTime.now()));

        CustomerChange change = new CustomerChange();
        change.setCustomerCreated(created);

        List<CustomerChange> changes = new ArrayList<>();
        changes.add(change);
        return changes;
    }

    @Test
    void saveEventTest() {
        String customerId = "CID";
        MachineEvent machineEvent = createTestMachineEvent();
        when(paymentEventPayloadMachineEventParser.parse(any()))
                .thenReturn(createTestCusomerEventPayload(customerId));

        customerMachineEventHandler.handle(List.of(machineEvent), new TestAcknowledgment());
        CustomerMessage message = customerDao.getAny(customerId, CustomerMessageEnum.CUSTOMER);
        assertNotNull("The message should not be empty", message);
    }

    private static class TestAcknowledgment implements Acknowledgment {

        @Override
        public void acknowledge() {

        }
    }
}
