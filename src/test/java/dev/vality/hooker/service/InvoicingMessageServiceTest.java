package dev.vality.hooker.service;

import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.converter.WebhookMessageBuilder;
import dev.vality.hooker.dao.HookDao;
import dev.vality.hooker.model.EventType;
import dev.vality.hooker.model.InvoiceStatusEnum;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.InvoicingMessageEnum;
import dev.vality.hooker.utils.BuildUtils;
import dev.vality.swag_webhook_events.model.Event;
import dev.vality.webhook.dispatcher.WebhookMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;

@PostgresqlSpringBootITest
@SpringBootTest
class InvoicingMessageServiceTest {

    @Autowired
    private MessageService<InvoicingMessage> invoicingService;

    @Autowired
    private HookDao hookDao;

    @MockitoBean
    private InvoicingEventService invoicingMessageEventService;

    @MockitoBean
    private WebhookMessageBuilder webhookMessageBuilder;

    @MockitoBean
    private WebhookKafkaProducerService webhookKafkaProducerService;

    private static final String PARTY_ID = "partyId";

    @BeforeEach
    void setUp() {
        Mockito.when(invoicingMessageEventService.getEventByMessage(any())).thenReturn(new Event());
        Mockito.when(webhookMessageBuilder.build(any(), any(), any(), any())).thenReturn(new WebhookMessage());
        Mockito.doNothing().when(webhookKafkaProducerService).send(any());
    }

    @Test
    void testProcess() {
        hookDao.create(BuildUtils.buildHook(PARTY_ID, "www.kek.ru", EventType.INVOICE_CREATED));
        hookDao.create(BuildUtils.buildHook(PARTY_ID, "www.lol.ru", EventType.INVOICE_CREATED));
        InvoicingMessage invoicingMessage = buildMessage(PARTY_ID, "invoice_id",
                InvoicingMessageEnum.INVOICE, EventType.INVOICE_CREATED);
        invoicingService.process(invoicingMessage);
        Mockito.verify(invoicingMessageEventService, Mockito.times(1)).getEventByMessage(any());
        Mockito.verify(webhookMessageBuilder, Mockito.times(2)).build(any(), any(), any(), any());
        Mockito.verify(webhookKafkaProducerService, Mockito.times(2)).send(any());
    }

    @Test
    void testInvoiceCashChange() {
        hookDao.create(BuildUtils.buildHook(PARTY_ID, "www.kek.ru", EventType.INVOICE_PAYMENT_CASH_CHANGED));
        hookDao.create(BuildUtils.buildHook(PARTY_ID, "www.lol.ru", EventType.INVOICE_PAYMENT_CASH_CHANGED));
        InvoicingMessage invoicingMessage = buildMessage(PARTY_ID, "invoice_id",
                InvoicingMessageEnum.PAYMENT, EventType.INVOICE_PAYMENT_CASH_CHANGED);
        invoicingService.process(invoicingMessage);
        Mockito.verify(invoicingMessageEventService, Mockito.times(1)).getEventByMessage(any());
        Mockito.verify(webhookMessageBuilder, Mockito.times(2)).build(any(), any(), any(), any());
        Mockito.verify(webhookKafkaProducerService, Mockito.times(2)).send(any());
    }

    private InvoicingMessage buildMessage(String partyId, String invoiceId,
                                          InvoicingMessageEnum type, EventType eventType) {
        InvoicingMessage invoicingMessage = new InvoicingMessage();
        invoicingMessage.setSequenceId(1L);
        invoicingMessage.setChangeId(1);
        invoicingMessage.setEventTime("2016-03-22T06:12:27Z");
        invoicingMessage.setSourceId(invoiceId);
        invoicingMessage.setPartyId(partyId);
        invoicingMessage.setShopId("shopId");
        invoicingMessage.setEventType(eventType);
        invoicingMessage.setType(type);
        invoicingMessage.setInvoiceStatus(InvoiceStatusEnum.UNPAID);
        return invoicingMessage;
    }
}
