package dev.vality.hooker.model;


import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;


class InvoicingMessageKeyTest {

    @Test
    void testEqualHashCode() {
        InvoicingMessageKey key = InvoicingMessageKey.builder()
                .invoiceId("inv")
                .type(InvoicingMessageEnum.INVOICE)
                .build();

        Map<InvoicingMessageKey, InvoicingMessage> map = new HashMap<>();
        map.put(key, new InvoicingMessage());

        InvoicingMessageKey keyCopy = InvoicingMessageKey.builder()
                .invoiceId("inv")
                .type(InvoicingMessageEnum.INVOICE)
                .build();

        assertNotNull(map.get(keyCopy));

    }

}
