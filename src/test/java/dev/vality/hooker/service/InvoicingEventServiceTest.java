package dev.vality.hooker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.damsel.domain.InvoicePaid;
import dev.vality.damsel.domain.InvoicePaymentPending;
import dev.vality.damsel.domain.InvoicePaymentStatus;
import dev.vality.damsel.domain.InvoiceStatus;
import dev.vality.damsel.payment_processing.InvoicingSrv;
import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.model.*;
import dev.vality.hooker.utils.BuildUtils;
import dev.vality.swag_webhook_events.model.Event;
import dev.vality.swag_webhook_events.model.RefundSucceeded;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@PostgresqlSpringBootITest
public class InvoicingEventServiceTest {

    @MockBean
    private InvoicingSrv.Iface invoicingClient;

    @Autowired
    private InvoicingEventService service;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.when(invoicingClient.get(any(), any()))
                .thenReturn(BuildUtils.buildInvoice("partyId", "invoiceId", "1", "1",
                        InvoiceStatus.paid(new InvoicePaid()),
                        InvoicePaymentStatus.pending(new InvoicePaymentPending())));
    }

    @RepeatedTest(7)
    public void testRefundSucceeded() {
        InvoicingMessage message = random(InvoicingMessage.class, "userInteraction");
        message.setPaymentId("1");
        message.setRefundId("1");
        message.setType(InvoicingMessageEnum.REFUND);
        message.setEventTime("2016-03-22T06:12:27Z");
        message.setEventType(EventType.INVOICE_PAYMENT_REFUND_STATUS_CHANGED);
        message.setRefundStatus(RefundStatusEnum.SUCCEEDED);
        Event event = service.getEventByMessage(message);
        assertTrue(event instanceof RefundSucceeded);
        RefundSucceeded refundSucceded = (RefundSucceeded) event;
        assertEquals("invoiceId", refundSucceded.getInvoice().getId());
        assertEquals("1", refundSucceded.getPayment().getId());
        assertEquals("1", refundSucceded.getRefund().getId());
        assertEquals("keksik", refundSucceded.getRefund().getReason());
        assertEquals("chicken-teriyaki", refundSucceded.getRefund().getRrn());
    }

    @RepeatedTest(7)
    public void testJson() throws JsonProcessingException {
        InvoicingMessage message = random(InvoicingMessage.class, "userInteraction");
        message.setPaymentId("1");
        message.setType(InvoicingMessageEnum.PAYMENT);
        message.setEventTime("2016-03-22T06:12:27Z");
        message.setEventType(EventType.INVOICE_PAYMENT_STATUS_CHANGED);
        message.setPaymentStatus(PaymentStatusEnum.CAPTURED);
        Event event = service.getEventByMessage(message);
        String json = objectMapper.writeValueAsString(event);
        assertTrue(json.contains("\"payment_id\":271771960"));
        assertTrue(json.contains("\"extraPaymentInfo\":{\"c2c_commission\":\"100\"}"));
        assertTrue(json.contains("\"externalId\":\"payment-external-id\""));
        assertTrue(json.contains("\"externalId\":\"invoice-external-id\""));
    }
}
