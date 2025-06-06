package dev.vality.hooker.config;


import dev.vality.testcontainers.annotations.KafkaConfig;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainerSingleton;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@KafkaTestcontainerSingleton(
        properties = {"kafka.topics.invoice.enabled=true", "kafka.topics.customer.enabled=true"},
        topicsKeys = {"kafka.topics.invoice.id", "kafka.topics.customer.id"})
@KafkaConfig
public @interface KafkaTest {
}