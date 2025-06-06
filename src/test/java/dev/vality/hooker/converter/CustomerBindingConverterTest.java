package dev.vality.hooker.converter;

import dev.vality.damsel.domain.BankCard;
import dev.vality.damsel.domain.PaymentTool;
import dev.vality.damsel.payment_processing.CustomerBinding;
import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerBindingConverterTest {

    private CustomerBindingConverter converter = new CustomerBindingConverter();

    @Test
    void testConvert() throws IOException {
        MockTBaseProcessor mockTBaseProcessor = new MockTBaseProcessor(MockMode.RANDOM, 15, 1);
        CustomerBinding source = mockTBaseProcessor
                .process(new CustomerBinding(), new TBaseHandler<>(CustomerBinding.class));
        source.getPaymentResource().setPaymentTool(
                PaymentTool.bank_card(mockTBaseProcessor.process(new BankCard(), new TBaseHandler<>(BankCard.class))));
        dev.vality.swag_webhook_events.model.CustomerBinding target = converter.convert(source);
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getPaymentResource().getPaymentSessionId(),
                target.getPaymentResource().getPaymentSession());
        assertEquals(source.getStatus().getSetField().getFieldName(), target.getStatus().getValue());
    }
}
