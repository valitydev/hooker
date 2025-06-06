package dev.vality.hooker.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.vality.hooker.model.WebhookMessageModel;
import dev.vality.hooker.service.AdditionalHeadersGenerator;
import dev.vality.hooker.service.crypt.Signer;
import dev.vality.swag_webhook_events.model.Event;
import dev.vality.webhook.dispatcher.WebhookMessage;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class WebhookMessageBuilderTest {

    @Mock
    private Signer signer;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private WebhookMessageBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new WebhookMessageBuilder(new AdditionalHeadersGenerator(), objectMapper, signer);
        Mockito.when(signer.sign(any(), any())).thenReturn("signature");
    }

    @Test
    void testBuild() {
        var webhookMessageModel = new WebhookMessageModel<>();
        webhookMessageModel.setHookId(1245L);
        webhookMessageModel.setUrl("kek.url");
        webhookMessageModel.setPrivateKey("");
        Event event = random(Event.class);
        String sourceId = "keks";
        long parentId = 1L;
        WebhookMessage webhookMessage = builder.build(webhookMessageModel, event, sourceId, parentId);
        assertEquals(sourceId, webhookMessage.getSourceId());
        assertEquals(parentId, webhookMessage.getParentEventId());
        assertEquals(webhookMessageModel.getHookId(), webhookMessage.getWebhookId());
        assertEquals(event.getEventID().longValue(), webhookMessage.getEventId());
        assertNotNull(webhookMessage.getAdditionalHeaders().get("Content-Signature"));
        assertEquals(ContentType.APPLICATION_JSON.getMimeType(), webhookMessage.getContentType());
        assertNotNull(webhookMessage.getCreatedAt());
        assertNotNull(webhookMessage.getRequestBody());
    }
}