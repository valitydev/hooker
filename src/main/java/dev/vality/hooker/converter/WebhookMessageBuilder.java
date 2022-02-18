package dev.vality.hooker.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.hooker.model.Message;
import dev.vality.hooker.model.WebhookMessageModel;
import dev.vality.hooker.service.AdditionalHeadersGenerator;
import dev.vality.hooker.service.crypt.Signer;
import dev.vality.swag_webhook_events.model.Event;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.http.entity.ContentType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class WebhookMessageBuilder {

    private final AdditionalHeadersGenerator additionalHeadersGenerator;
    private final ObjectMapper objectMapper;
    private final Signer signer;

    @SneakyThrows
    public WebhookMessage build(WebhookMessageModel webhookMessageModel, Event event, String sourceId, Long parentId) {
        Message message = webhookMessageModel.getMessage();
        final String messageJson = objectMapper.writeValueAsString(event);
        final String signature = signer.sign(messageJson, webhookMessageModel.getPrivateKey());
        return new WebhookMessage()
                .setWebhookId(message.getId())
                .setSourceId(sourceId)
                .setEventId(message.getEventId())
                .setParentEventId(parentId)
                .setCreatedAt(TypeUtil.temporalToString(Instant.now()))
                .setUrl(webhookMessageModel.getUrl())
                .setContentType(ContentType.APPLICATION_JSON.getMimeType())
                .setAdditionalHeaders(additionalHeadersGenerator.generate(signature))
                .setRequestBody(messageJson.getBytes(StandardCharsets.UTF_8));
    }
}
