package dev.vality.hooker.dao;

import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.dao.impl.CustomerDaoImpl;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.hooker.model.EventType;
import dev.vality.hooker.model.Hook;
import dev.vality.hooker.utils.BuildUtils;
import dev.vality.swag_webhook_events.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@PostgresqlSpringBootITest
public class CustomerDaoImplTest {

    @Autowired
    private CustomerDaoImpl messageDao;

    @Autowired
    private HookDao hookDao;

    private final String partyId = "56678";
    private final String customerIdOne = "1234";
    private final String customerIdTwo = "1235";
    private final String customerIdThree = "1236";

    private Long messageIdOne;
    private Long messageId1Two;
    private Long messageId2Two;
    private Long messageIdThree;

    private Hook hook;

    @BeforeEach
    public void setUp() {
        hook = Hook.builder()
                .partyId(partyId)
                .topic(Event.TopicEnum.CUSTOMERSTOPIC.getValue())
                .url("zzz")
                .filters(Set.of(
                        WebhookAdditionalFilter.builder()
                                .eventType(EventType.CUSTOMER_CREATED)
                                .build(),
                        WebhookAdditionalFilter.builder()
                                .eventType(EventType.CUSTOMER_BINDING_STARTED)
                                .build()))
                .build();

        hookDao.create(hook);
        messageIdOne = messageDao.save(BuildUtils.buildCustomerMessage(1L, partyId, EventType.CUSTOMER_CREATED,
                CustomerMessageEnum.CUSTOMER, customerIdOne, "shop"));
        messageId1Two = messageDao.save(BuildUtils.buildCustomerMessage(1L, partyId, EventType.CUSTOMER_CREATED,
                CustomerMessageEnum.CUSTOMER, customerIdTwo, "shop"));
        messageId2Two = messageDao.save(BuildUtils.buildCustomerMessage(1L, partyId, EventType.CUSTOMER_BINDING_STARTED,
                CustomerMessageEnum.BINDING, customerIdTwo, "shop"));
        messageIdThree = messageDao.save(BuildUtils.buildCustomerMessage(1L, partyId, EventType.CUSTOMER_DELETED,
                CustomerMessageEnum.CUSTOMER, customerIdThree, "shop"));

    }

    @Test
    public void testGetInvoicingMessage() {
        CustomerMessage messageOne = messageDao.getAny(customerIdOne, CustomerMessageEnum.CUSTOMER);
        assertEquals(customerIdOne, messageOne.getSourceId());
        assertEquals(EventType.CUSTOMER_CREATED, messageOne.getEventType());
        assertEquals(partyId, messageOne.getPartyId());

        CustomerMessage messageTwo = messageDao.getAny(customerIdTwo, CustomerMessageEnum.BINDING);
        assertEquals(customerIdTwo, messageTwo.getSourceId());
        assertEquals(EventType.CUSTOMER_BINDING_STARTED, messageTwo.getEventType());
        assertEquals(partyId, messageTwo.getPartyId());
    }

    @Test
    public void testGetWebhookModels() {
        var webhookModelsOne = messageDao.getWebhookModels(messageIdOne);
        assertEquals(1, webhookModelsOne.size());
        assertEquals(hook.getId(), webhookModelsOne.get(0).getHookId());
        assertEquals(customerIdOne, webhookModelsOne.get(0).getMessage().getSourceId());
        assertEquals(EventType.CUSTOMER_CREATED, webhookModelsOne.get(0).getMessage().getEventType());

        var webhookModels1Two = messageDao.getWebhookModels(messageId1Two);
        assertEquals(hook.getId(), webhookModels1Two.get(0).getHookId());
        assertEquals(customerIdTwo, webhookModels1Two.get(0).getMessage().getSourceId());
        assertEquals(EventType.CUSTOMER_CREATED, webhookModels1Two.get(0).getMessage().getEventType());

        var webhookModels2Two = messageDao.getWebhookModels(messageId2Two);
        assertEquals(hook.getId(), webhookModels2Two.get(0).getHookId());
        assertEquals(customerIdTwo, webhookModels2Two.get(0).getMessage().getSourceId());
        assertEquals(CustomerMessageEnum.BINDING, webhookModels2Two.get(0).getMessage().getType());
        assertEquals(EventType.CUSTOMER_BINDING_STARTED, webhookModels2Two.get(0).getMessage().getEventType());

        var webhookModelsThree = messageDao.getWebhookModels(messageIdThree);
        assertEquals(0, webhookModelsThree.size());
    }

    @Test
    public void testGetParentEventId() {
        Long parentEventIdOne = messageDao.getParentId(hook.getId(), customerIdOne, messageIdOne);
        assertEquals(-1, parentEventIdOne);

        Long parentEventId1Two = messageDao.getParentId(hook.getId(), customerIdTwo, messageId1Two);
        assertEquals(-1, parentEventId1Two);

        Long parentEventId2Two = messageDao.getParentId(hook.getId(), customerIdTwo, messageId2Two);
        var webhookModels1Two = messageDao.getWebhookModels(messageId1Two);
        assertEquals(1, webhookModels1Two.size());
        assertEquals(parentEventId2Two, messageId1Two);

        Long parentEventIdThree = messageDao.getParentId(hook.getId(), customerIdThree, messageIdThree);
        assertEquals(-1, parentEventIdThree);
    }
}
