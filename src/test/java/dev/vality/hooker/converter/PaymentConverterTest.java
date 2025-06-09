package dev.vality.hooker.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.vality.damsel.domain.*;
import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import dev.vality.swag_webhook_events.model.CustomerPayer;
import dev.vality.swag_webhook_events.model.Payment;
import dev.vality.swag_webhook_events.model.PaymentResourcePayer;
import dev.vality.swag_webhook_events.model.RecurrentPayer;
import org.junit.jupiter.api.RepeatedTest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PaymentConverterTest  {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final PaymentConverter converter = new PaymentConverter(new MetadataDeserializer(objectMapper));

    @RepeatedTest(7)
    void testConvert() throws IOException {
        MockTBaseProcessor mockTBaseProcessor = new MockTBaseProcessor(MockMode.RANDOM, 15, 1);
        InvoicePayment source = mockTBaseProcessor
                .process(new InvoicePayment(), new TBaseHandler<>(InvoicePayment.class));
        source.setCreatedAt("2016-03-22T06:12:27Z");
        PaymentTool paymentTool = PaymentTool
                .bank_card(mockTBaseProcessor.process(new BankCard(), new TBaseHandler<>(BankCard.class)));
        if (source.getPayer().isSetPaymentResource()) {
            source.getPayer().getPaymentResource().getResource()
                    .setPaymentTool(paymentTool);
        } else if (source.getPayer().isSetCustomer()) {
            source.getPayer().getCustomer().setPaymentTool(paymentTool);
        }
        source.setStatus(InvoicePaymentStatus.pending(new InvoicePaymentPending()));
        Payment target = converter
                .convert(new dev.vality.damsel.payment_processing.InvoicePayment(source,
                                List.of(), List.of(), List.of(), List.of()),
                        createMockInvoice(
                                source.getStatus().isSetCaptured()
                                        ? source.getStatus().getCaptured().getCost().getAmount()
                                        : source.getCost().getAmount()));
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getStatus().getSetField().getFieldName(), target.getStatus().getValue());
        if (source.getStatus().isSetCaptured() && source.getStatus().getCaptured().isSetCost()) {
            assertEquals(source.getStatus().getCaptured().getCost().getAmount(), target.getAmount().longValue());
            assertEquals(source.getStatus().getCaptured().getCost().getCurrency().getSymbolicCode(),
                    target.getCurrency());
        } else {
            assertEquals(source.getCost().getAmount(), target.getAmount().longValue());
            assertEquals(source.getCost().getCurrency().getSymbolicCode(), target.getCurrency());
        }
        if (source.getPayer().isSetCustomer()) {
            assertInstanceOf(CustomerPayer.class, target.getPayer());
            assertEquals(source.getPayer().getCustomer().getCustomerId(),
                    ((CustomerPayer) target.getPayer()).getCustomerID());
        }
        if (source.getPayer().isSetPaymentResource()) {
            assertInstanceOf(PaymentResourcePayer.class, target.getPayer());
            assertEquals(source.getPayer().getPaymentResource().getContactInfo().getEmail(),
                    ((PaymentResourcePayer) target.getPayer()).getContactInfo().getEmail());
            assertEquals(source.getPayer().getPaymentResource().getContactInfo().getPhoneNumber(),
                    ((PaymentResourcePayer) target.getPayer()).getContactInfo().getPhoneNumber());
            assertEquals(source.getPayer().getPaymentResource().getResource().getPaymentSessionId(),
                    ((PaymentResourcePayer) target.getPayer()).getPaymentSession());
        } else if (source.getPayer().isSetRecurrent()) {
            assertInstanceOf(RecurrentPayer.class, target.getPayer());
            assertEquals(source.getPayer().getRecurrent().getRecurrentParent().getInvoiceId(),
                    ((RecurrentPayer) target.getPayer()).getRecurrentParentPayment().getInvoiceID());
        }
    }

    private static dev.vality.damsel.payment_processing.Invoice createMockInvoice(Long amount) {
        return new dev.vality.damsel.payment_processing.Invoice().setInvoice(
                new Invoice()
                        .setCost(new Cash()
                                .setAmount(amount)
                                .setCurrency(new CurrencyRef()
                                        .setSymbolicCode("RUB"))
                        ));
    }
}
