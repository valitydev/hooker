package dev.vality.hooker.service;

import dev.vality.hooker.converter.WebhookMessageBuilder;
import dev.vality.hooker.dao.MessageDao;
import dev.vality.hooker.model.Message;
import dev.vality.swag_webhook_events.model.Event;
import dev.vality.webhook.dispatcher.WebhookMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MessageService<T extends Message> {
    private final MessageDao<T> messageDao;
    private final EventService<T> eventService;
    private final WebhookMessageBuilder webhookMessageBuilder;
    private final WebhookKafkaProducerService webhookKafkaProducerService;

    public void process(T message) {
        log.info("Start processing of message {}", message);
        Long id = messageDao.save(message);
        String sourceId = message.getSourceId();
        if (id != null) {
            message.setId(id);
            var webhookModels = messageDao.getWebhookModels(id);
            if (!webhookModels.isEmpty()) {
                log.info("Processing {} webhook(s)", webhookModels.size());
                Event event = eventService.getEventByMessage(message);
                webhookModels.forEach(w -> {
                    Long hookId = w.getHookId();
                    Long parentEventId = messageDao.getParentId(hookId, sourceId, id);
                    WebhookMessage webhookMessage = webhookMessageBuilder.build(w, event, sourceId, parentEventId);
                    log.info("Try to send webhook to kafka: {}, parentId {}", webhookMessage, parentEventId);
                    webhookKafkaProducerService.send(webhookMessage);
                    log.info("Webhook to kafka was sent: sourceId={}", webhookMessage.getSourceId());
                });
            }
        }
        log.info("End processing of message {}", sourceId);
    }
}
