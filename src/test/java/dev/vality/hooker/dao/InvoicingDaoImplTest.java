package dev.vality.hooker.dao;

import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.dao.impl.InvoicingDaoImpl;
import dev.vality.hooker.model.*;
import dev.vality.hooker.utils.BuildUtils;
import dev.vality.swag_webhook_events.model.Event;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Slf4j
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
    private Long messageIdCreatedTwo;
    private Long messageIdProcessedTwo;
    private Long messageIdCapturedTwo;
    private Long messageIdThree;

    private Hook hook;

    @BeforeEach
    public void setUp() throws InterruptedException {
        hook = createHookModel();

        hook = hookDao.create(hook);
        log.info("hookOld: {}", hookDao.getHookById(hook.getId()));

        Thread.sleep(1000L); // Sleep for lag between create hook and events

        messageIdOne = messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.INVOICE.getValue(),
                invoiceOne, partyId, EventType.INVOICE_CREATED,
                InvoiceStatusEnum.UNPAID, null));
        messageIdCreatedTwo = messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.INVOICE.getValue(),
                invoiceTwo, partyId, EventType.INVOICE_CREATED,
                InvoiceStatusEnum.UNPAID, null, 1L, 1));
        messageIdProcessedTwo = messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.PAYMENT.getValue(),
                invoiceTwo, partyId, EventType.INVOICE_PAYMENT_STATUS_CHANGED,
                InvoiceStatusEnum.PAID, PaymentStatusEnum.PROCESSED, 1L, 2));
        messageIdCapturedTwo = messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.PAYMENT.getValue(),
                invoiceTwo, partyId, EventType.INVOICE_PAYMENT_STATUS_CHANGED,
                InvoiceStatusEnum.PAID, PaymentStatusEnum.CAPTURED, 1L, 3));
        messageIdThree = messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.INVOICE.getValue(),
                invoiceThree, partyId, EventType.INVOICE_STATUS_CHANGED,
                InvoiceStatusEnum.PAID, null));
    }

    private Hook createHookModel() {
        return Hook.builder()
                .partyId(partyId)
                .topic(Event.TopicEnum.INVOICESTOPIC.getValue())
                .url("zzz")
                .filters(Set.of(
                        WebhookAdditionalFilter.builder()
                                .eventType(EventType.INVOICE_CREATED)
                                .build(),
                        WebhookAdditionalFilter.builder()
                                .eventType(EventType.INVOICE_PAYMENT_STATUS_CHANGED)
                                .invoicePaymentStatus("processed")
                                .build(),
                        WebhookAdditionalFilter.builder()
                                .eventType(EventType.INVOICE_PAYMENT_STATUS_CHANGED)
                                .invoicePaymentStatus("captured")
                                .build()
                ))
                .build();
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

        assertNotEquals(messageIdCreatedTwo, messageIdProcessedTwo);

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

        var webhookModels1Two = messageDao.getWebhookModels(messageIdCreatedTwo);
        assertEquals(hook.getId(), webhookModels1Two.get(0).getHookId());
        assertEquals(invoiceTwo, webhookModels1Two.get(0).getMessage().getSourceId());
        assertEquals(InvoiceStatusEnum.UNPAID, webhookModels1Two.get(0).getMessage().getInvoiceStatus());

        var webhookModels2Two = messageDao.getWebhookModels(messageIdProcessedTwo);
        assertEquals(hook.getId(), webhookModels2Two.get(0).getHookId());
        assertEquals(invoiceTwo, webhookModels2Two.get(0).getMessage().getSourceId());
        assertEquals(InvoiceStatusEnum.PAID, webhookModels2Two.get(0).getMessage().getInvoiceStatus());
        assertEquals(PaymentStatusEnum.PROCESSED, webhookModels2Two.get(0).getMessage().getPaymentStatus());

        var webhookModelsThree = messageDao.getWebhookModels(messageIdThree);
        assertEquals(0, webhookModelsThree.size());
    }

    @Test
    public void testGetParentEventId() {
        Long parentEventIdOne = messageDao.getParentId(hook.getId(), invoiceOne, messageIdOne);
        assertEquals(-1, parentEventIdOne);

        Long parentEventIdCreatedTwo = messageDao.getParentId(hook.getId(), invoiceTwo, messageIdCreatedTwo);
        assertEquals(-1, parentEventIdCreatedTwo);

        Long parentEventIdProcessedTwo = messageDao.getParentId(hook.getId(), invoiceTwo, messageIdProcessedTwo);
        var webhookModels1Two = messageDao.getWebhookModels(messageIdCreatedTwo);
        assertEquals(1, webhookModels1Two.size());
        assertEquals(parentEventIdProcessedTwo, messageIdCreatedTwo);

        Long parentEventIdCapturedTwo = messageDao.getParentId(hook.getId(), invoiceTwo, messageIdCapturedTwo);
        var webhookModels2Two = messageDao.getWebhookModels(messageIdCreatedTwo);
        assertEquals(1, webhookModels2Two.size());
        assertEquals(parentEventIdCapturedTwo, messageIdProcessedTwo);

        Long parentEventIdThree = messageDao.getParentId(hook.getId(), invoiceThree, messageIdThree);
        assertEquals(-1, parentEventIdThree);
    }

    @Test
    public void testGetParentEventIdWithOldHook() throws InterruptedException {
        Hook hookOld = hookDao.create(createHookModel());
        log.info("hookOld: {}", hookDao.getHookById(hookOld.getId()));

        Thread.sleep(1000L); // Sleep for lag between create hook and events

        String newInvoiceId = "new_invoice";
        var oldMessageId = messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.INVOICE.getValue(),
                newInvoiceId, partyId, EventType.INVOICE_CREATED,
                InvoiceStatusEnum.UNPAID, null, 1L, 1));

        Long parentEventId = messageDao.getParentId(hookOld.getId(), newInvoiceId, oldMessageId);
        assertEquals(-1, parentEventId);

        hookDao.delete(hookOld.getId());
        log.info("hookOld: {}", hookDao.getHookById(hookOld.getId()));

        Thread.sleep(2000L);

        Hook hookModel = createHookModel();
        hookModel.setCreatedAt(null);
        Hook hookNew = hookDao.create(hookModel);
        log.info("hookNew: {}", hookDao.getHookById(hookNew.getId()));

        Thread.sleep(1000L);

        var newMessageId = messageDao.save(BuildUtils.buildMessage(InvoicingMessageEnum.PAYMENT.getValue(),
                newInvoiceId, partyId, EventType.INVOICE_PAYMENT_STATUS_CHANGED,
                InvoiceStatusEnum.PAID, PaymentStatusEnum.CAPTURED, 1L, 2));

        parentEventId = messageDao.getParentId(hookNew.getId(), newInvoiceId, newMessageId);
        assertEquals(-1, parentEventId);
    }
}
