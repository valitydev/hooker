package dev.vality.hooker.config;


import dev.vality.testcontainers.annotations.postgresql.PostgresqlTestcontainer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PostgresqlTestcontainer
public @interface PostgresqlSpringBootITest {
}