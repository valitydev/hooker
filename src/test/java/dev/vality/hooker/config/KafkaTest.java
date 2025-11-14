package dev.vality.hooker.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.vality.testcontainers.annotations.KafkaConfig;
import dev.vality.testcontainers.annotations.kafka.KafkaTestcontainerSingleton;
import dev.vality.testcontainers.annotations.kafka.constants.Provider;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@KafkaTestcontainerSingleton(
        properties = {"kafka.topics.invoice.enabled=true"},
        topicsKeys = {"kafka.topics.invoice.id"},
        provider = Provider.APACHE)
@KafkaConfig
public @interface KafkaTest {
}