package dev.vality.hooker.utils;

import dev.vality.damsel.base.Content;
import dev.vality.damsel.domain.*;
import dev.vality.damsel.json.Value;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.damsel.payment_processing.InvoicePaymentSession;
import dev.vality.damsel.payment_processing.InvoiceRefundSession;
import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import dev.vality.hooker.dao.WebhookAdditionalFilter;
import dev.vality.hooker.model.*;
import dev.vality.swag_webhook_events.model.Event;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
public class BuildUtils {

    public static InvoicingMessage buildMessage(String type, String invoiceId, String partyId, EventType eventType,
                                                InvoiceStatusEnum invoiceStatus, PaymentStatusEnum paymentStatus) {
        return buildMessage(type, invoiceId, partyId, eventType, invoiceStatus, paymentStatus, null, 0);
    }

    public static InvoicingMessage buildMessage(String type, String invoiceId, String partyId, EventType eventType,
                                                InvoiceStatusEnum invoiceStatus, PaymentStatusEnum paymentStatus,
                                                Long sequenceId, Integer changeId) {
        InvoicingMessage message = new InvoicingMessage();
        message.setEventTime(LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
        message.setType(InvoicingMessageEnum.lookup(type));
        message.setPartyId(partyId);
        message.setEventType(eventType);
        message.setSourceId(invoiceId);
        message.setShopId("123");
        message.setInvoiceStatus(invoiceStatus);
        if (message.isPayment() || message.isRefund()) {
            message.setPaymentId("123");
            message.setPaymentStatus(paymentStatus);
        }

        if (message.isRefund()) {
            message.setRefundId("123");
            message.setRefundStatus(RefundStatusEnum.SUCCEEDED);
        }
        message.setSequenceId(sequenceId);
        message.setChangeId(changeId);
        log.info("Create message: {}", message);
        return message;
    }

    public static dev.vality.damsel.payment_processing.Invoice buildInvoice(String partyId, String invoiceId,
                                                                            String paymentId, String refundId,
                                                                            InvoiceStatus invoiceStatus,
                                                                            InvoicePaymentStatus paymentStatus)
            throws IOException {
        MockTBaseProcessor thriftBaseProcessor = new MockTBaseProcessor(MockMode.RANDOM, 15, 1);
        dev.vality.damsel.payment_processing.Invoice invoice = new dev.vality.damsel.payment_processing.Invoice()
                .setInvoice(buildInvoice(partyId, invoiceId, invoiceStatus, thriftBaseProcessor))
                .setPayments(buildPayments(partyId, paymentId, refundId, paymentStatus, thriftBaseProcessor));
        Payer payer = invoice.getPayments().get(0).getPayment().getPayer();
        if (payer.isSetPaymentResource()) {
            PaymentTool paymentTool = PaymentTool.bank_card(
                    thriftBaseProcessor.process(new BankCard(), new TBaseHandler<>(BankCard.class))
            );
            payer.getPaymentResource().getResource()
                    .setPaymentTool(paymentTool);
        }
        return invoice;
    }

    private static Invoice buildInvoice(String partyId, String invoiceId, InvoiceStatus invoiceStatus,
                                        MockTBaseProcessor thriftBaseProcessor) throws IOException {
        return thriftBaseProcessor.process(
                        new Invoice(),
                        new TBaseHandler<>(Invoice.class)
                )
                .setId(invoiceId)
                .setOwnerId(partyId)
                .setCreatedAt("2016-03-22T06:12:27Z")
                .setContext(new Content("lel", ByteBuffer.wrap("{\"payment_id\": 271771960}".getBytes())))
                .setDue("2016-03-22T06:12:27Z")
                .setStatus(invoiceStatus)
                .setExternalId("invoice-external-id");
    }

    private static List<InvoicePayment> buildPayments(String partyId, String paymentId, String refundId,
                                                      InvoicePaymentStatus paymentStatus,
                                                      MockTBaseProcessor thriftBaseProcessor) throws IOException {
        return Collections.singletonList(
                new InvoicePayment()
                        .setAdjustments(Collections.emptyList())
                        .setPayment(buildPayment(partyId, paymentId, paymentStatus, thriftBaseProcessor))
                        .setRefunds(buildRefunds(refundId, thriftBaseProcessor))
                        .setSessions(Collections.singletonList(
                                new InvoicePaymentSession().setTransactionInfo(getTransactionInfo()))));
    }

    private static dev.vality.damsel.domain.InvoicePayment buildPayment(String partyId, String paymentId,
                                                                        InvoicePaymentStatus paymentStatus,
                                                                        MockTBaseProcessor thriftBaseProcessor)
            throws IOException {
        return thriftBaseProcessor.process(
                        new dev.vality.damsel.domain.InvoicePayment(),
                        new TBaseHandler<>(dev.vality.damsel.domain.InvoicePayment.class)
                )
                .setCreatedAt("2016-03-22T06:12:27Z")
                .setId(paymentId)
                .setOwnerId(partyId)
                .setStatus(paymentStatus)
                .setExternalId("payment-external-id");
    }

    private static List<dev.vality.damsel.payment_processing.InvoicePaymentRefund> buildRefunds(
            String refundId,
            MockTBaseProcessor thriftBaseProcessor
    ) throws IOException {
        return Collections.singletonList(
                new dev.vality.damsel.payment_processing.InvoicePaymentRefund(
                        buildRefund(refundId, thriftBaseProcessor),
                        Collections.singletonList(new InvoiceRefundSession().setTransactionInfo(getTransactionInfo()))
                )
        );
    }

    private static InvoicePaymentRefund buildRefund(
            String refundId,
            MockTBaseProcessor thriftBaseProcessor
    ) throws IOException {
        return thriftBaseProcessor.process(
                        new InvoicePaymentRefund(),
                        new TBaseHandler<>(InvoicePaymentRefund.class)
                )
                .setReason("keksik")
                .setCreatedAt("2016-03-22T06:12:27Z")
                .setId(refundId);
    }

    private static TransactionInfo getTransactionInfo() {
        return new TransactionInfo()
                .setId(UUID.randomUUID().toString())
                .setExtra(Map.of())
                .setAdditionalInfo(getAdditionalInfo());
    }

    private static AdditionalTransactionInfo getAdditionalInfo() {
        return new AdditionalTransactionInfo()
                .setRrn("chicken-teriyaki")
                .setExtraPaymentInfo(Map.of("c2c_commission", "100"));
    }

    public static Hook buildHook(String partyId, String url, EventType... types) {
        Hook hook = new Hook();
        hook.setPartyId(partyId);
        hook.setTopic(Event.TopicEnum.INVOICES_TOPIC.getValue());
        hook.setUrl(url);

        Set<WebhookAdditionalFilter> webhookAdditionalFilters = new HashSet<>();
        for (EventType type : types) {
            webhookAdditionalFilters.add(WebhookAdditionalFilter.builder().eventType(type).build());
        }
        hook.setFilters(webhookAdditionalFilters);
        return hook;
    }
}
