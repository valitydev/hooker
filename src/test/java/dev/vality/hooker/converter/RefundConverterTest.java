package dev.vality.hooker.converter;

import dev.vality.damsel.domain.InvoicePaymentRefund;
import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import dev.vality.swag_webhook_events.model.Refund;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration(classes = {
        RefundConverter.class
})
@SpringBootTest
public class RefundConverterTest {

    @Autowired
    private RefundConverter converter;

    @Test
    public void testConvert() throws IOException {
        InvoicePaymentRefund source = new MockTBaseProcessor(MockMode.RANDOM, 15, 1)
                .process(new InvoicePaymentRefund(), new TBaseHandler<>(InvoicePaymentRefund.class));
        source.setCreatedAt("2016-03-22T06:12:27Z");
        Refund target =
                converter.convert(new dev.vality.damsel.payment_processing.InvoicePaymentRefund(source, of()));
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getStatus().getSetField().getFieldName(), target.getStatus().getValue());
        assertEquals(source.getReason(), target.getReason());
    }
}