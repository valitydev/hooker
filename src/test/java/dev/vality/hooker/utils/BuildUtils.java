package dev.vality.hooker.utils;

import dev.vality.damsel.base.Content;
import dev.vality.damsel.domain.AdditionalTransactionInfo;
import dev.vality.damsel.domain.BankCard;
import dev.vality.damsel.domain.Invoice;
import dev.vality.damsel.domain.InvoicePaymentRefund;
import dev.vality.damsel.domain.InvoicePaymentStatus;
import dev.vality.damsel.domain.InvoiceStatus;
import dev.vality.damsel.domain.PaymentTool;
import dev.vality.damsel.domain.TransactionInfo;
import dev.vality.damsel.json.Value;
import dev.vality.damsel.payment_processing.InvoicePayment;
import dev.vality.damsel.payment_processing.InvoiceRefundSession;
import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import dev.vality.hooker.dao.WebhookAdditionalFilter;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.CustomerMessageEnum;
import dev.vality.hooker.model.EventType;
import dev.vality.hooker.model.Hook;
import dev.vality.hooker.model.InvoiceStatusEnum;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.InvoicingMessageEnum;
import dev.vality.hooker.model.PaymentStatusEnum;
import dev.vality.hooker.model.RefundStatusEnum;
import dev.vality.swag_webhook_events.model.Event;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BuildUtils {
    private static int messageId = 1;

    public static InvoicingMessage buildMessage(String type, String invoiceId, String partyId, EventType eventType,
                                                InvoiceStatusEnum invoiceStatus, PaymentStatusEnum paymentStatus) {
        return buildMessage(type, invoiceId, partyId, eventType, invoiceStatus, paymentStatus, null, 0);
    }

    public static InvoicingMessage buildMessage(String type, String invoiceId, String partyId, EventType eventType,
                                                InvoiceStatusEnum invoiceStatus, PaymentStatusEnum paymentStatus,
                                                Long sequenceId, Integer changeId) {
        InvoicingMessage message = new InvoicingMessage();
        message.setEventTime("2016-03-22T06:12:27Z");
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
        return message;
    }

    public static dev.vality.damsel.payment_processing.Customer buildCustomer(String customerId, String bindingId)
            throws IOException {
        MockTBaseProcessor thriftBaseProcessor = new MockTBaseProcessor(MockMode.RANDOM, 15, 1);
        dev.vality.damsel.payment_processing.Customer customer = thriftBaseProcessor.process(
                new dev.vality.damsel.payment_processing.Customer(),
                new TBaseHandler<>(dev.vality.damsel.payment_processing.Customer.class)
        );
        dev.vality.damsel.payment_processing.CustomerBinding customerBinding = thriftBaseProcessor.process(
                new dev.vality.damsel.payment_processing.CustomerBinding(),
                new TBaseHandler<>(dev.vality.damsel.payment_processing.CustomerBinding.class)
        ).setId(bindingId);

        customer.setId(customerId)
                .setCreatedAt("2016-03-22T06:12:27Z")
                .setMetadata(Value.obj(new HashMap<>()))
                .setBindings(Collections.singletonList(customerBinding));

        customer.getBindings().get(0).getPaymentResource().setPaymentTool(
                PaymentTool.bank_card(thriftBaseProcessor.process(new BankCard(), new TBaseHandler<>(BankCard.class))));
        return customer;
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
        if (invoice.getPayments().get(0).getPayment().getPayer().isSetPaymentResource()) {
            PaymentTool paymentTool = PaymentTool.bank_card(
                    thriftBaseProcessor.process(new BankCard(), new TBaseHandler<>(BankCard.class))
            );
            invoice.getPayments().get(0).getPayment().getPayer().getPaymentResource().getResource()
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
                .setStatus(invoiceStatus);
    }

    private static List<InvoicePayment> buildPayments(String partyId, String paymentId, String refundId,
                                                      InvoicePaymentStatus paymentStatus,
                                                      MockTBaseProcessor thriftBaseProcessor) throws IOException {
        return Collections.singletonList(
                new InvoicePayment()
                        .setAdjustments(Collections.emptyList())
                        .setPayment(buildPayment(partyId, paymentId, paymentStatus, thriftBaseProcessor))
                        .setRefunds(buildRefunds(refundId, thriftBaseProcessor))
                        .setSessions(Collections.emptyList())
        );
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
                .setStatus(paymentStatus);
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
                .setRrn("chicken-teriyaki");
    }

    public static CustomerMessage buildCustomerMessage(Long eventId, String partyId, EventType eventType,
                                                       CustomerMessageEnum type, String custId, String shopId) {
        CustomerMessage customerMessage = new CustomerMessage();
        customerMessage.setPartyId(partyId);
        customerMessage.setEventTime("2018-03-22T06:12:27Z");
        customerMessage.setEventType(eventType);
        customerMessage.setType(type);
        customerMessage.setSourceId(custId);
        customerMessage.setShopId(shopId);

        if (customerMessage.isBinding()) {
            customerMessage.setBindingId("12456");
        }
        return customerMessage;
    }

    public static Hook buildHook(String partyId, String url, EventType... types) {
        Hook hook = new Hook();
        hook.setPartyId(partyId);
        hook.setTopic(Event.TopicEnum.INVOICESTOPIC.getValue());
        hook.setUrl(url);

        Set<WebhookAdditionalFilter> webhookAdditionalFilters = new HashSet<>();
        for (EventType type : types) {
            webhookAdditionalFilters.add(WebhookAdditionalFilter.builder().eventType(type).build());
        }
        hook.setFilters(webhookAdditionalFilters);
        return hook;
    }
}
