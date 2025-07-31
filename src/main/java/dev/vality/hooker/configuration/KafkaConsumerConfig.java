package dev.vality.hooker.configuration;

import dev.vality.hooker.listener.InvoicingEventKafkaListener;
import dev.vality.hooker.listener.MachineEventHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    @ConditionalOnProperty(value = "kafka.topics.invoice.enabled", havingValue = "true")
    public InvoicingEventKafkaListener paymentEventsKafkaListener(MachineEventHandler invoicingMachineEventHandler) {
        return new InvoicingEventKafkaListener(invoicingMachineEventHandler);
    }
}
