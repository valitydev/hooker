package dev.vality.hooker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.damsel.base.Rational;
import dev.vality.damsel.domain.*;
import dev.vality.damsel.payment_processing.Invoice;
import dev.vality.damsel.payment_processing.InvoicingSrv;
import dev.vality.hooker.config.PostgresqlSpringBootITest;
import dev.vality.hooker.model.*;
import dev.vality.hooker.model.interaction.*;
import dev.vality.hooker.utils.BuildUtils;
import dev.vality.swag_webhook_events.model.Event;
import dev.vality.swag_webhook_events.model.PaymentInteractionCompleted;
import dev.vality.swag_webhook_events.model.PaymentInteractionRequested;
import dev.vality.swag_webhook_events.model.RefundSucceeded;
import org.apache.thrift.TException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@PostgresqlSpringBootITest
@SpringBootTest
class InvoicingEventServiceTest {

    @MockitoBean
    private InvoicingSrv.Iface invoicingClient;

    @Autowired
    private InvoicingEventService service;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        when(invoicingClient.get(any(), any()))
                .thenReturn(BuildUtils.buildInvoice("partyId", "invoiceId", "1", "1",
                        InvoiceStatus.paid(new InvoicePaid()),
                        InvoicePaymentStatus.pending(new InvoicePaymentPending())));
    }

    @RepeatedTest(7)
    void testRefundSucceeded() {
        InvoicingMessage message = random(InvoicingMessage.class, "userInteraction");
        message.setPaymentId("1");
        message.setSequenceId(1L);
        message.setRefundId("1");
        message.setType(InvoicingMessageEnum.REFUND);
        message.setEventTime("2016-03-22T06:12:27Z");
        message.setEventType(EventType.INVOICE_PAYMENT_REFUND_STATUS_CHANGED);
        message.setRefundStatus(RefundStatusEnum.SUCCEEDED);
        Event event = service.getEventByMessage(message);
        assertInstanceOf(RefundSucceeded.class, event);
        RefundSucceeded refundSucceded = (RefundSucceeded) event;
        assertEquals("invoiceId", refundSucceded.getInvoice().getId());
        assertEquals("1", refundSucceded.getPayment().getId());
        assertEquals("1", refundSucceded.getRefund().getId());
        assertEquals("keksik", refundSucceded.getRefund().getReason());
        assertEquals("chicken-teriyaki", refundSucceded.getRefund().getRrn());
    }

    @RepeatedTest(7)
    void testJson() throws JsonProcessingException {
        InvoicingMessage message = random(InvoicingMessage.class, "userInteraction");
        message.setPaymentId("1");
        message.setSequenceId(1L);
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

    @RepeatedTest(1)
    void testAdjustment() throws IOException, TException {
        String adjustmentId = "1";
        var invoice = createInvoiceWithStatusChangeAdjustment(adjustmentId);
        InvoicePaymentAdjustment invoicePaymentAdjustment;
        when(invoicingClient.get(any(), any())).thenReturn(invoice);

        InvoicingMessage message = createInvloiceMessage(adjustmentId);
        InvoicePaymentAdjustment adjustmentByMessage = service.getAdjustmentByMessage(message, adjustmentId);
        assertEquals(adjustmentId, adjustmentByMessage.id);
        assertTrue(adjustmentByMessage.isSetState());
        assertTrue(adjustmentByMessage.getState().isSetStatusChange());
        assertTrue(adjustmentByMessage.getState().getStatusChange().getScenario().getTargetStatus().isSetCaptured());

        when(invoicingClient.get(any(), any())).thenReturn(initCashFlowChangeAdjustment(adjustmentId));

        adjustmentByMessage = service.getAdjustmentByMessage(message, adjustmentId);
        assertTrue(adjustmentByMessage.getState().isSetCashFlow());
    }

    private Invoice initCashFlowChangeAdjustment(String adjustmentId) throws IOException {
        var invoice = createInvoiceWithStatusChangeAdjustment(adjustmentId);
        InvoicePaymentAdjustment invoicePaymentAdjustment;
        InvoicePaymentAdjustmentState invoicePaymentAdjustmentState = new InvoicePaymentAdjustmentState();
        invoicePaymentAdjustmentState.setCashFlow(new InvoicePaymentAdjustmentCashFlowState());
        invoicePaymentAdjustment = new InvoicePaymentAdjustment()
                .setId(adjustmentId)
                .setState(invoicePaymentAdjustmentState);
        invoice.getPayments().get(0).setAdjustments(List.of(invoicePaymentAdjustment));
        return invoice;
    }

    @NotNull
    private static InvoicingMessage createInvloiceMessage(String adjustmentId) {
        InvoicingMessage message = random(InvoicingMessage.class, "userInteraction");
        message.setPaymentId(adjustmentId);
        message.setType(InvoicingMessageEnum.PAYMENT);
        message.setEventTime("2016-03-22T06:12:27Z");
        message.setEventType(EventType.INVOICE_PAYMENT_STATUS_CHANGED);
        message.setPaymentStatus(PaymentStatusEnum.CAPTURED);
        message.setSequenceId(1L);
        return message;
    }

    @NotNull
    private Invoice createInvoiceWithStatusChangeAdjustment(String adjustmentId)
            throws IOException {
        Invoice invoice = BuildUtils.buildInvoice("partyId", "invoiceId", "1", "1",
                InvoiceStatus.paid(new InvoicePaid()),
                InvoicePaymentStatus.pending(new InvoicePaymentPending()));
        InvoicePaymentAdjustmentState invoicePaymentAdjustmentState = new InvoicePaymentAdjustmentState();
        invoicePaymentAdjustmentState.setStatusChange(new InvoicePaymentAdjustmentStatusChangeState()
                .setScenario(new InvoicePaymentAdjustmentStatusChange()
                        .setTargetStatus(new InvoicePaymentStatus(InvoicePaymentStatus.captured(
                                new InvoicePaymentCaptured())))));
        InvoicePaymentAdjustment invoicePaymentAdjustment = new InvoicePaymentAdjustment()
                .setId(adjustmentId)
                .setState(invoicePaymentAdjustmentState);
        invoice.getPayments().get(0).setAdjustments(List.of(invoicePaymentAdjustment));
        return invoice;
    }

    @Test
    void testUserInteractionRequested() throws JsonProcessingException {
        InvoicingMessage message = createDefaultInvoicingMessage();
        message.setEventType(EventType.INVOICE_PAYMENT_USER_INTERACTION_CHANGE_REQUESTED);
        message.setUserInteraction(new BrowserHttpInteraction("get", "http://test", null));
        Event event = service.getEventByMessage(message);
        String json = objectMapper.writeValueAsString(event);
        assertTrue(json.contains("\"eventType\":\"PaymentInteractionRequested\""));
        assertTrue(json.contains("\"requestType\":\"get\""));
        assertTrue(json.contains("\"userInteractionType\":\"BrowserHTTPRequest\""));
        assertTrue(json.contains("\"invoiceId\":\"invoiceId\""));
        assertTrue(json.contains("\"paymentId\":\"%s\"".formatted(message.getPaymentId())));
        assertInstanceOf(PaymentInteractionRequested.class, event);
    }

    @Test
    void testUserInteractionsCompleted() throws JsonProcessingException {
        InvoicingMessage message = createDefaultInvoicingMessage();
        message.setEventType(EventType.INVOICE_PAYMENT_USER_INTERACTION_CHANGE_COMPLETED);
        message.setUserInteraction(new QrCodeDisplay("wefvqewvrq32fveqrw".getBytes()));
        Event event = service.getEventByMessage(message);
        String json = objectMapper.writeValueAsString(event);
        assertTrue(json.contains("\"eventType\":\"PaymentInteractionCompleted\""));
        assertTrue(json.contains("\"userInteractionType\":\"QrCodeDisplayRequest\""));
        assertTrue(json.contains("\"invoiceId\":\"invoiceId\""));
        assertTrue(json.contains("\"paymentId\":\"%s\"".formatted(message.getPaymentId())));
        assertInstanceOf(PaymentInteractionCompleted.class, event);
    }

    @Test
    void testUserInteractionsCompletedApiExtension() throws JsonProcessingException {
        InvoicingMessage message = createDefaultInvoicingMessage();
        message.setEventType(EventType.INVOICE_PAYMENT_USER_INTERACTION_CHANGE_COMPLETED);
        message.setUserInteraction(new ApiExtension("p2p"));
        Event event = service.getEventByMessage(message);
        String json = objectMapper.writeValueAsString(event);
        assertTrue(json.contains("\"eventType\":\"PaymentInteractionCompleted\""));
        assertTrue(json.contains("\"userInteractionType\":\"ApiExtensionRequest\""));
        assertTrue(json.contains("\"invoiceId\":\"invoiceId\""));
        assertTrue(json.contains("\"paymentId\":\"%s\"".formatted(message.getPaymentId())));
        assertInstanceOf(PaymentInteractionCompleted.class, event);
    }

    @Test
    void testUserInteractionsCompletedPaymentTerminal() throws JsonProcessingException {
        InvoicingMessage message = createDefaultInvoicingMessage();
        message.setEventType(EventType.INVOICE_PAYMENT_USER_INTERACTION_CHANGE_COMPLETED);
        message.setUserInteraction(new PaymentTerminalReceipt("p2p", "2016-03-22T06:12:27Z"));
        Event event = service.getEventByMessage(message);
        String json = objectMapper.writeValueAsString(event);
        assertTrue(json.contains("\"eventType\":\"PaymentInteractionCompleted\""));
        assertTrue(json.contains("\"userInteractionType\":\"PaymentTerminalReceipt\""));
        assertTrue(json.contains("\"invoiceId\":\"invoiceId\""));
        assertTrue(json.contains("\"paymentId\":\"%s\"".formatted(message.getPaymentId())));
        assertInstanceOf(PaymentInteractionCompleted.class, event);
    }

    @Test
    void testUserInteractionsCompletedCrypto() throws JsonProcessingException {
        InvoicingMessage message = createDefaultInvoicingMessage();
        message.setEventType(EventType.INVOICE_PAYMENT_USER_INTERACTION_CHANGE_COMPLETED);
        message.setUserInteraction(new CryptoCurrencyTransfer("address", new Rational(1L, 10L), "bitcoin"));
        Event event = service.getEventByMessage(message);
        String json = objectMapper.writeValueAsString(event);
        assertTrue(json.contains("\"eventType\":\"PaymentInteractionCompleted\""));
        assertTrue(json.contains("\"userInteractionType\":\"CryptoCurrencyTransferRequest\""));
        assertTrue(json.contains("\"cryptoAddress\":\"address\""));
        assertTrue(json.contains("\"cryptoCurrency\":\"bitcoin\""));
        assertTrue(json.contains("\"denominator\":1"));
        assertTrue(json.contains("\"invoiceId\":\"invoiceId\""));
        assertTrue(json.contains("\"paymentId\":\"%s\"".formatted(message.getPaymentId())));
        assertInstanceOf(PaymentInteractionCompleted.class, event);
    }

    @NotNull
    private static InvoicingMessage createDefaultInvoicingMessage() {
        InvoicingMessage message = new InvoicingMessage();
        message.setId(123L);
        message.setPaymentId("271771960");
        message.setSequenceId(123L);
        message.setType(InvoicingMessageEnum.PAYMENT);
        message.setEventTime("2016-03-22T06:12:27Z");
        return message;
    }
}
