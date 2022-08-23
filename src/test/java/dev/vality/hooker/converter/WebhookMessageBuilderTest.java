package dev.vality.hooker.converter;

import dev.vality.hooker.configuration.AppConfig;
import dev.vality.hooker.model.WebhookMessageModel;
import dev.vality.hooker.service.AdditionalHeadersGenerator;
import dev.vality.hooker.service.crypt.AsymSigner;
import dev.vality.swag_webhook_events.model.Event;
import dev.vality.webhook.dispatcher.WebhookMessage;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ContextConfiguration(classes = {
        WebhookMessageBuilder.class,
        AdditionalHeadersGenerator.class,
        AppConfig.class
})
@SpringBootTest
public class WebhookMessageBuilderTest {

    @MockBean
    private AsymSigner signer;

    @Autowired
    private WebhookMessageBuilder builder;

    @BeforeEach
    public void setUp() {
        Mockito.when(signer.sign(any(), any())).thenReturn("signature");
    }

    @Test
    public void testBuild() {
        var webhookMessageModel = new WebhookMessageModel<>();
        webhookMessageModel.setHookId(1245L);
        webhookMessageModel.setUrl("kek.url");
        webhookMessageModel.setPrivateKey("");
        Event event = random(Event.class);
        String sourceId = "keks";
        long parentId = 1L;
        WebhookMessage webhookMessage = builder.build(webhookMessageModel, event, sourceId, parentId);
        assertEquals(sourceId, webhookMessage.getSourceId());
        //assertEquals(parentId, webhookMessage.getParentEventId());
        assertEquals(webhookMessageModel.getHookId(), webhookMessage.getWebhookId());
        assertEquals(event.getEventID().longValue(), webhookMessage.getEventId());
        assertNotNull(webhookMessage.getAdditionalHeaders().get("Content-Signature"));
        assertEquals(ContentType.APPLICATION_JSON.getMimeType(), webhookMessage.getContentType());
        assertNotNull(webhookMessage.getCreatedAt());
        assertNotNull(webhookMessage.getRequestBody());
    }
}