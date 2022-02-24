package dev.vality.hooker.listener;

import dev.vality.damsel.domain.ContactInfo;
import dev.vality.damsel.payment_processing.CustomerChange;
import dev.vality.damsel.payment_processing.CustomerCreated;
import dev.vality.damsel.payment_processing.EventPayload;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.hooker.AbstractIntegrationTest;
import dev.vality.hooker.dao.CustomerDao;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.machinegun.eventsink.MachineEvent;
import dev.vality.machinegun.msgpack.Value;
import dev.vality.sink.common.parser.impl.MachineEventParser;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class CustomerMachineEventHandlerTest extends AbstractIntegrationTest {

    @Autowired
    private MachineEventHandler customerMachineEventHandler;

    @Autowired
    private CustomerDao customerDao;

    @MockBean
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
    public void saveEventTest() {
        String customerId = "CID";
        MachineEvent machineEvent = createTestMachineEvent();
        Mockito.when(paymentEventPayloadMachineEventParser.parse(any(MachineEvent.class)))
                .thenReturn(createTestCusomerEventPayload(customerId));

        customerMachineEventHandler.handle(Arrays.asList(machineEvent), new TestAcknowledgment());
        CustomerMessage message = customerDao.getAny(customerId, CustomerMessageEnum.CUSTOMER);
        assertTrue("The message should not be empty", message != null);
    }

    private static class TestAcknowledgment implements Acknowledgment {

        @Override
        public void acknowledge() {

        }
    }
}
