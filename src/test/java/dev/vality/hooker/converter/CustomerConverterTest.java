package dev.vality.hooker.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.vality.damsel.json.Value;
import dev.vality.damsel.payment_processing.Customer;
import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final CustomerConverter converter = new CustomerConverter(new MetadataDeserializer(objectMapper));

    @Test
    void testConvert() throws IOException {
        Customer source = new MockTBaseProcessor(MockMode.RANDOM, 15, 1)
                .process(new Customer(), new TBaseHandler<>(Customer.class));
        source.setMetadata(Value.obj(new HashMap<>()));
        dev.vality.swag_webhook_events.model.Customer target = converter.convert(source);
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getShopId(), target.getShopID());
        assertEquals(source.getStatus().getSetField().getFieldName(), target.getStatus().getValue());
    }
}