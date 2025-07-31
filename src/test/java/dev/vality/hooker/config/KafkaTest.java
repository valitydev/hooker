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
        properties = {"kafka.topics.invoice.enabled=true"},
        topicsKeys = {"kafka.topics.invoice.id"})
@KafkaConfig
public @interface KafkaTest {
}