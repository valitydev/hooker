package dev.vality.hooker.configuration;

import dev.vality.hooker.dao.rowmapper.WebhookModelRowMapper;
import dev.vality.hooker.model.InvoicingMessage;
import dev.vality.hooker.model.WebhookMessageModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;

@Configuration
public class RowMapperConfig {

    @Bean
    public RowMapper<WebhookMessageModel<InvoicingMessage>> invoicingWebhookRowMapper(
            RowMapper<InvoicingMessage> invoicingRowMapper) {
        return new WebhookModelRowMapper<>(invoicingRowMapper);
    }
}
