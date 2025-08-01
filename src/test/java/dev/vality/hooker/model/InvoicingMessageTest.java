package dev.vality.hooker.model;


import org.junit.jupiter.api.Test;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InvoicingMessageTest {

    @Test
    void testCopy() {
        InvoicingMessage invoicingMessage = random(InvoicingMessage.class, "userInteraction");
        InvoicingMessage copy = invoicingMessage.copy();
        invoicingMessage.setRefundId("asd");
        assertNotEquals("asd", copy.getRefundId());
    }
}
