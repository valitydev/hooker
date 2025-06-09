package dev.vality.hooker.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vality.geck.common.util.TypeUtil;
import dev.vality.hooker.model.WebhookMessageModel;
import dev.vality.hooker.service.AdditionalHeadersGenerator;
import dev.vality.hooker.service.crypt.Signer;
import dev.vality.swag_webhook_events.model.Event;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.MediaType;
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
        final String messageJson = objectMapper.writeValueAsString(event);
        final String signature = signer.sign(messageJson, webhookMessageModel.getPrivateKey());
        return new WebhookMessage()
                .setWebhookId(webhookMessageModel.getHookId())
                .setSourceId(sourceId)
                .setEventId(event.getEventID())
                .setParentEventId(parentId)
                .setCreatedAt(TypeUtil.temporalToString(Instant.now()))
                .setUrl(webhookMessageModel.getUrl())
                .setContentType(MediaType.APPLICATION_JSON_VALUE)
                .setAdditionalHeaders(additionalHeadersGenerator.generate(signature))
                .setRequestBody(messageJson.getBytes(StandardCharsets.UTF_8));
    }
}
