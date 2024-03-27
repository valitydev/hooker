package dev.vality.hooker.configuration;

import dev.vality.hooker.converter.WebhookMessageBuilder;
import dev.vality.hooker.dao.InvoicingMessageDao;
import dev.vality.hooker.dao.MessageDao;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.service.CustomerMessageService;
import dev.vality.hooker.service.EventService;
import dev.vality.hooker.service.InvoiceMessageService;
import dev.vality.hooker.service.WebhookKafkaProducerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {
    @Bean
    public InvoiceMessageService invoicingService(InvoicingMessageDao messageDao,
                                                  EventService<InvoicingMessage> eventService,
                                                  WebhookMessageBuilder webhookMessageBuilder,
                                                  WebhookKafkaProducerService webhookKafkaProducerService) {
        return new InvoiceMessageService(messageDao, eventService, webhookMessageBuilder, webhookKafkaProducerService);
    }

    @Bean
    public CustomerMessageService<CustomerMessage> customerService(MessageDao<CustomerMessage> messageDao,
                                                                   EventService<CustomerMessage> eventService,
                                                                   WebhookMessageBuilder webhookMessageBuilder,
                                                                   WebhookKafkaProducerService webhookKafkaProducerService) {
        return new CustomerMessageService<>(messageDao, eventService, webhookMessageBuilder,
                webhookKafkaProducerService);
    }

}
