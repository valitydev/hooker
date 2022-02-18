package dev.vality.hooker.service;

import dev.vality.kafka.common.exception.KafkaProduceException;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookKafkaProducerService {

    private final KafkaTemplate<String, WebhookMessage> kafkaTemplate;

    @Value("${kafka.topic.webhook-dispatcher.name}")
    private String topicName;

    @Value("${kafka.topic.webhook-dispatcher.produce.enabled}")
    private boolean producerEnabled;

    public void send(WebhookMessage webhookMessage) {
        if (producerEnabled) {
            sendWebhook(webhookMessage);
        }
    }

    private void sendWebhook(WebhookMessage webhookMessage) {
        try {
            kafkaTemplate.send(topicName, webhookMessage.getSourceId(), webhookMessage).get();
        } catch (InterruptedException e) {
            log.error("InterruptedException command: {}", webhookMessage, e);
            Thread.currentThread().interrupt();
            throw new KafkaProduceException(e);
        } catch (Exception e) {
            log.error("Error while sending command: {}", webhookMessage, e);
            throw new KafkaProduceException(e);
        }
    }
}
