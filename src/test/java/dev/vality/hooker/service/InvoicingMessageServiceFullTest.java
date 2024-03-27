package dev.vality.hooker.service;

import dev.vality.damsel.domain.*;
import dev.vality.damsel.payment_processing.InvoicingSrv;
import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.dao.HookDao;
import dev.vality.hooker.dao.InvoicingMessageDao;
import dev.vality.hooker.dao.impl.InvoicingDaoImpl;
import dev.vality.hooker.model.*;
import dev.vality.hooker.utils.BuildUtils;
import org.apache.thrift.TException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@PostgresqlSpringBootITest
public class InvoicingMessageServiceFullTest {

    public static final String SHOP_ID = "shopId";
    public static final String INVOICE_ID = "invoice_id";
    @MockBean
    private InvoicingSrv.Iface invoicingClient;

    @MockBean
    private WebhookKafkaProducerService webhookKafkaProducerService;

    @Autowired
    private HookDao hookDao;

    @Autowired
    private InvoicingDaoImpl messageDao;

    @Autowired
    private InvoicingMessageDao invoicingMessageDao;

    @Autowired
    private InvoiceMessageService invoicingService;

    private static final String PARTY_ID = "partyId";

    @BeforeEach
    public void setUp() throws Exception {
        mockGetInvoice(InvoicePaymentStatus.cancelled(new InvoicePaymentCancelled()));
    }

    private void mockGetInvoice(InvoicePaymentStatus invoicePaymentStatus) throws TException, IOException {
        when(invoicingClient.get(any(), any()))
                .thenReturn(BuildUtils.buildInvoice("partyId", "invoiceId", "1", "1",
                        InvoiceStatus.paid(new InvoicePaid()), invoicePaymentStatus));
    }

    @Test
    public void testProcess() throws TException, IOException {
        InvoicingMessage invoicingMessage = buildMessage(PARTY_ID, INVOICE_ID,
                InvoicingMessageEnum.PAYMENT, EventType.INVOICE_PAYMENT_STATUS_CHANGED);

        invoicingService.process(invoicingMessage);

        mockGetInvoice(InvoicePaymentStatus.failed(new InvoicePaymentFailed()));
        invoicingMessage.setPaymentStatus(PaymentStatusEnum.FAILED);
        invoicingMessage.setSequenceId(2L);
        invoicingService.process(invoicingMessage);

        mockGetInvoice(InvoicePaymentStatus.captured(new InvoicePaymentCaptured()));
        invoicingMessage.setPaymentStatus(PaymentStatusEnum.CAPTURED);
        invoicingMessage.setSequenceId(3L);
        invoicingService.process(invoicingMessage);

        Boolean hasWebhooks = messageDao.hasWebhooks(invoicingMessage);
        assertFalse(hasWebhooks);

        Hook hook = BuildUtils.buildHook(PARTY_ID, SHOP_ID, "paid", null, "www.kek.ru",
                EventType.INVOICE_PAYMENT_STATUS_CHANGED, "captured", "cancelled");
        hookDao.create(hook);

        hasWebhooks = messageDao.hasWebhooks(invoicingMessage);
        assertTrue(hasWebhooks);

        mockGetInvoice(InvoicePaymentStatus.failed(new InvoicePaymentFailed()));
        invoicingMessage.setPaymentStatus(PaymentStatusEnum.FAILED);
        invoicingMessage.setSequenceId(4L);
        invoicingService.process(invoicingMessage);

        mockGetInvoice(InvoicePaymentStatus.captured(new InvoicePaymentCaptured()));
        invoicingMessage.setPaymentStatus(PaymentStatusEnum.CAPTURED);
        invoicingMessage.setSequenceId(5L);
        invoicingService.process(invoicingMessage);

        verify(webhookKafkaProducerService, times(1)).send(any());

        Long parentEventId = messageDao.getParentId(hook.getId(), INVOICE_ID, invoicingMessage.getId());
        assertEquals(-1, parentEventId);
    }

    private InvoicingMessage buildMessage(String partyId, String invoiceId,
                                          InvoicingMessageEnum type, EventType eventType) {
        InvoicingMessage invoicingMessage = new InvoicingMessage();
        invoicingMessage.setSequenceId(1L);
        invoicingMessage.setChangeId(1);
        invoicingMessage.setPaymentId("1");
        invoicingMessage.setEventTime("2016-03-22T06:12:27Z");
        invoicingMessage.setSourceId(invoiceId);
        invoicingMessage.setPartyId(partyId);
        invoicingMessage.setShopId(SHOP_ID);
        invoicingMessage.setEventType(eventType);
        invoicingMessage.setType(type);
        invoicingMessage.setInvoiceStatus(InvoiceStatusEnum.PAID);
        invoicingMessage.setPaymentStatus(PaymentStatusEnum.CANCELLED);
        return invoicingMessage;
    }

}
