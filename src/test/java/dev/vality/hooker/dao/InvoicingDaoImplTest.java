package dev.vality.hooker.dao;

import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.dao.impl.InvoicingDaoImpl;
import dev.vality.hooker.model.*;
import dev.vality.hooker.utils.BuildUtils;
import dev.vality.swag_webhook_events.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@PostgresqlSpringBootITest
public class InvoicingDaoImplTest {

    @Autowired
    private InvoicingDaoImpl messageDao;

    @Autowired
    private HookDao hookDao;

    private final String partyId = "56678";
    private final String invoiceOne = "1234";
    private final String invoiceTwo = "1235";
    private final String invoiceThree = "1236";

    private Long messageIdOne;
    private Long messageId1Two;
    private Long messageId2Two;
    private Long messageIdThree;

    private Hook hook;

    @BeforeEach
    public void setUp() {
        hook = Hook.builder()
                .partyId(partyId)
                .topic(Event.TopicEnum.INVOICESTOPIC.getValue())
                .url("zzz")
                .filters(Set.of(
                        WebhookAdditionalFilter.builder()
                                .eventType(EventType.INVOICE_CREATED)
                                .build(),
                        WebhookAdditionalFilter.builder()
                                .eventType(EventType.INVOICE_PAYMENT_STATUS_CHANGED)
                                .invoicePaymentStatus("captured")
                                .build()))
                .build();

        hookDao.create(hook);
        messageIdOne = messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.INVOICE.getValue(),
                invoiceOne, partyId, EventType.INVOICE_CREATED,
                InvoiceStatusEnum.UNPAID, null));
        messageId1Two = messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.INVOICE.getValue(),
                invoiceTwo, partyId, EventType.INVOICE_CREATED,
                InvoiceStatusEnum.UNPAID, null, 1L, 1));
        messageId2Two = messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.PAYMENT.getValue(),
                invoiceTwo, partyId, EventType.INVOICE_PAYMENT_STATUS_CHANGED,
                InvoiceStatusEnum.PAID, PaymentStatusEnum.CAPTURED, 1L, 2));
        messageIdThree = messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.INVOICE.getValue(),
                invoiceThree, partyId, EventType.INVOICE_STATUS_CHANGED,
                InvoiceStatusEnum.PAID, null));
    }

    @Test
    public void testGetInvoicingMessage() {
        InvoicingMessage messageOne = messageDao.getInvoicingMessage(
                InvoicingMessageKey.builder()
                        .invoiceId(invoiceOne)
                        .type(InvoicingMessageEnum.INVOICE)
                        .build());
        assertEquals(invoiceOne, messageOne.getSourceId());
        assertEquals(InvoiceStatusEnum.UNPAID, messageOne.getInvoiceStatus());
        assertEquals(partyId, messageOne.getPartyId());

        InvoicingMessage messageTwo = messageDao.getInvoicingMessage(
                InvoicingMessageKey.builder()
                        .invoiceId(invoiceTwo)
                        .paymentId("123")
                        .type(InvoicingMessageEnum.PAYMENT)
                        .build());
        assertEquals(invoiceTwo, messageTwo.getSourceId());
        assertEquals(InvoiceStatusEnum.PAID, messageTwo.getInvoiceStatus());
        assertEquals(PaymentStatusEnum.CAPTURED, messageTwo.getPaymentStatus());
        assertEquals(partyId, messageTwo.getPartyId());

        assertNotEquals(messageId1Two, messageId2Two);

        InvoicingMessage messageThree = messageDao.getInvoicingMessage(
                InvoicingMessageKey.builder()
                        .invoiceId(invoiceThree)
                        .type(InvoicingMessageEnum.INVOICE)
                        .build());
        assertEquals(invoiceThree, messageThree.getSourceId());
        assertEquals(InvoiceStatusEnum.PAID, messageThree.getInvoiceStatus());
        assertEquals(partyId, messageThree.getPartyId());
    }

    @Test
    public void testGetWebhookModels() {
        var webhookModelsOne = messageDao.getWebhookModels(messageIdOne);
        assertEquals(1, webhookModelsOne.size());
        assertEquals(hook.getId(), webhookModelsOne.get(0).getHookId());
        assertEquals(invoiceOne, webhookModelsOne.get(0).getMessage().getSourceId());
        assertEquals(InvoiceStatusEnum.UNPAID, webhookModelsOne.get(0).getMessage().getInvoiceStatus());

        var webhookModels1Two = messageDao.getWebhookModels(messageId1Two);
        assertEquals(hook.getId(), webhookModels1Two.get(0).getHookId());
        assertEquals(invoiceTwo, webhookModels1Two.get(0).getMessage().getSourceId());
        assertEquals(InvoiceStatusEnum.UNPAID, webhookModels1Two.get(0).getMessage().getInvoiceStatus());

        var webhookModels2Two = messageDao.getWebhookModels(messageId2Two);
        assertEquals(hook.getId(), webhookModels2Two.get(0).getHookId());
        assertEquals(invoiceTwo, webhookModels2Two.get(0).getMessage().getSourceId());
        assertEquals(InvoiceStatusEnum.PAID, webhookModels2Two.get(0).getMessage().getInvoiceStatus());
        assertEquals(PaymentStatusEnum.CAPTURED, webhookModels2Two.get(0).getMessage().getPaymentStatus());

        var webhookModelsThree = messageDao.getWebhookModels(messageIdThree);
        assertEquals(0, webhookModelsThree.size());
    }

    @Test
    public void testGetParentEventId() {
        Long parentEventIdOne = messageDao.getParentId(hook.getId(), invoiceOne, messageIdOne);
        assertEquals(-1, parentEventIdOne);

        Long parentEventId1Two = messageDao.getParentId(hook.getId(), invoiceTwo, messageId1Two);
        assertEquals(-1, parentEventId1Two);

        Long parentEventId2Two = messageDao.getParentId(hook.getId(), invoiceTwo, messageId2Two);
        var webhookModels1Two = messageDao.getWebhookModels(messageId1Two);
        assertEquals(1, webhookModels1Two.size());

        Long parentEventIdThree = messageDao.getParentId(hook.getId(), invoiceThree, messageIdThree);
        assertEquals(-1, parentEventIdThree);
    }
}
