package dev.vality.hooker.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import dev.vality.hooker.converter.WebhookMessageBuilder;
import dev.vality.hooker.dao.MessageDao;
import dev.vality.hooker.dao.rowmapper.WebhookModelRowMapper;
import dev.vality.hooker.model.CustomerMessage;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.WebhookMessageModel;
import dev.vality.hooker.service.EventService;
import dev.vality.hooker.service.MessageService;
import dev.vality.hooker.service.WebhookKafkaProducerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;

@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public MessageService<InvoicingMessage> invoicingService(MessageDao<InvoicingMessage> messageDao,
                                                             EventService<InvoicingMessage> eventService,
                                                             WebhookMessageBuilder webhookMessageBuilder,
                                                             WebhookKafkaProducerService webhookKafkaProducerService) {
        return new MessageService<>(messageDao, eventService, webhookMessageBuilder, webhookKafkaProducerService);
    }

    @Bean
    public MessageService<CustomerMessage> customerService(MessageDao<CustomerMessage> messageDao,
                                                           EventService<CustomerMessage> eventService,
                                                           WebhookMessageBuilder webhookMessageBuilder,
                                                           WebhookKafkaProducerService webhookKafkaProducerService) {
        return new MessageService<>(messageDao, eventService, webhookMessageBuilder, webhookKafkaProducerService);
    }

    @Bean
    public RowMapper<WebhookMessageModel<CustomerMessage>> customerWebhookRowMapper(
            RowMapper<CustomerMessage> customerRowMapper) {
        return new WebhookModelRowMapper<>(customerRowMapper);
    }

    @Bean
    public RowMapper<WebhookMessageModel<InvoicingMessage>> invoicingWebhookRowMapper(
            RowMapper<InvoicingMessage> invoicingRowMapper) {
        return new WebhookModelRowMapper<>(invoicingRowMapper);
    }
}
