package dev.vality.hooker.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.vality.testcontainers.annotations.KafkaConfig;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@KafkaConfig
public @interface KafkaTest {
}