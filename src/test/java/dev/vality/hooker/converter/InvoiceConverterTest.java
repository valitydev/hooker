package dev.vality.hooker.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.vality.damsel.domain.Invoice;
import dev.vality.geck.serializer.kit.mock.MockMode;
import dev.vality.geck.serializer.kit.mock.MockTBaseProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final InvoiceConverter converter = new InvoiceConverter(new MetadataDeserializer(objectMapper));

    @Test
    void testConverter() throws IOException {
        Invoice source = new MockTBaseProcessor(MockMode.RANDOM, 15, 1)
                .process(new Invoice(), new TBaseHandler<>(Invoice.class));
        source.setCreatedAt("2016-03-22T06:12:27Z");
        source.setDue("2016-03-22T06:12:27Z");
        dev.vality.swag_webhook_events.model.Invoice target = converter.convert(source);
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getShopRef().getId(), target.getShopID());
        assertEquals(source.getCost().getAmount(), target.getAmount().longValue());
        assertEquals(source.getStatus().getSetField().getFieldName(), target.getStatus().getValue());
        if (source.getDetails().isSetCart()) {
            assertEquals(source.getDetails().getCart().getLines().size(), target.getCart().size());
        }
    }
}
