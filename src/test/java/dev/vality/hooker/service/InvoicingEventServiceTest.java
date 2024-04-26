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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@PostgresqlSpringBootITest
class InvoicingEventServiceTest {

    @MockBean
    private InvoicingSrv.Iface invoicingClient;

    @Autowired
    private InvoicingEventService service;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {
        when(invoicingClient.get(any(), any()))
                .thenReturn(BuildUtils.buildInvoice("partyId", "invoiceId", "1", "1",
                        InvoiceStatus.paid(new InvoicePaid()),
                        InvoicePaymentStatus.pending(new InvoicePaymentPending())));
    }

    @RepeatedTest(7)
    void testRefundSucceeded() {
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
    void testJson() throws JsonProcessingException {
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

    @RepeatedTest(1)
    void testAdjustment() throws IOException, TException {
        String adjustmentId = "1";
        createInvoiceWithStatusChangeAdjustmnt result = createInvoiceWithStatusChangeAdjustment(adjustmentId);
        InvoicePaymentAdjustment invoicePaymentAdjustment;
        when(invoicingClient.get(any(), any())).thenReturn(result.invoice());

        InvoicingMessage message = createInvloiceMessage(adjustmentId);
        InvoicePaymentAdjustment adjustmentByMessage = service.getAdjustmentByMessage(message, adjustmentId);
        assertEquals(adjustmentId, adjustmentByMessage.id);
        assertTrue(adjustmentByMessage.isSetState());
        assertTrue(adjustmentByMessage.getState().isSetStatusChange());
        assertTrue(adjustmentByMessage.getState().getStatusChange().getScenario().getTargetStatus().isSetCaptured());

        result.invoicePaymentAdjustmentState().setCashFlow(new InvoicePaymentAdjustmentCashFlowState());
        invoicePaymentAdjustment = new InvoicePaymentAdjustment()
                .setId(adjustmentId)
                .setState(result.invoicePaymentAdjustmentState());
        result.invoice().getPayments().get(0).setAdjustments(List.of(invoicePaymentAdjustment));
        when(invoicingClient.get(any(), any())).thenReturn(result.invoice());

        adjustmentByMessage = service.getAdjustmentByMessage(message, adjustmentId);
        assertTrue(adjustmentByMessage.getState().isSetCashFlow());
    }

    @NotNull
    private static InvoicingMessage createInvloiceMessage(String adjustmentId) {
        InvoicingMessage message = random(InvoicingMessage.class, "userInteraction");
        message.setPaymentId(adjustmentId);
        message.setType(InvoicingMessageEnum.PAYMENT);
        message.setEventTime("2016-03-22T06:12:27Z");
        message.setEventType(EventType.INVOICE_PAYMENT_STATUS_CHANGED);
        message.setPaymentStatus(PaymentStatusEnum.CAPTURED);
        return message;
    }

    @NotNull
    private static createInvoiceWithStatusChangeAdjustmnt createInvoiceWithStatusChangeAdjustment(String adjustmentId)
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
        createInvoiceWithStatusChangeAdjustmnt result = new createInvoiceWithStatusChangeAdjustmnt(invoice, invoicePaymentAdjustmentState);
        return result;
    }

    private record createInvoiceWithStatusChangeAdjustmnt(Invoice invoice, InvoicePaymentAdjustmentState invoicePaymentAdjustmentState) {
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
        assertTrue(event instanceof PaymentInteractionRequested);
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
        assertTrue(event instanceof PaymentInteractionCompleted);
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
        assertTrue(event instanceof PaymentInteractionCompleted);
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
        assertTrue(event instanceof PaymentInteractionCompleted);
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
        assertTrue(event instanceof PaymentInteractionCompleted);
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
