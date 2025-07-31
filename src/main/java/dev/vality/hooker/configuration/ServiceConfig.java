package dev.vality.hooker.configuration;

import dev.vality.hooker.converter.WebhookMessageBuilder;
import dev.vality.hooker.dao.MessageDao;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.service.EventService;
import dev.vality.hooker.service.MessageService;
import dev.vality.hooker.service.WebhookKafkaProducerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {
    @Bean
    public MessageService<InvoicingMessage> invoicingService(MessageDao<InvoicingMessage> messageDao,
                                                             EventService<InvoicingMessage> eventService,
                                                             WebhookMessageBuilder webhookMessageBuilder,
                                                             WebhookKafkaProducerService webhookKafkaProducerService) {
        return new MessageService<>(messageDao, eventService, webhookMessageBuilder, webhookKafkaProducerService);
    }
}
