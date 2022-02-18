package dev.vality.hooker.converter;

import dev.vality.damsel.domain.BankCard;
import dev.vality.damsel.domain.PaymentTool;
import dev.vality.damsel.payment_processing.CustomerBinding;
import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration(classes = {
        CustomerBindingConverter.class
})
@SpringBootTest
public class CustomerBindingConverterTest {

    @Autowired
    private CustomerBindingConverter converter;

    @Test
    public void testConvert() throws IOException {
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
