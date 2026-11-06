package com.cgv.identityservice;

import com.cgv.identityservice.config.FeignConfig;
import com.cgv.identityservice.config.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestTemplate;
import feign.codec.Encoder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FeignConfigTest {
    @Test void springCreatesEncoderWithJacksonTwoMapper() {
        try (var context = new AnnotationConfigApplicationContext(JacksonConfig.class, FeignConfig.class)) {
            assertNotNull(context.getBean(ObjectMapper.class));
            assertNotNull(context.getBean(Encoder.class));
        }
    }

    @Test void keycloakFormEncodesSpecialCharactersCollectionsAndNull() {
        try (var context = new AnnotationConfigApplicationContext(JacksonConfig.class, FeignConfig.class)) {
            var request = new RequestTemplate();
            request.header("Content-Type", "application/x-www-form-urlencoded");
            Map<String, Object> form = new LinkedHashMap<>();
            form.put("username", "name+tag@example.test");
            form.put("scope", List.of("openid", "profile"));
            form.put("optional", null);
            context.getBean(Encoder.class).encode(form, Map.class, request);
            assertEquals("username=name%2Btag%40example.test&scope=openid&scope=profile&optional=",
                    new String(request.body(), StandardCharsets.UTF_8));
            assertTrue(request.headers().get("Content-Type").contains("application/x-www-form-urlencoded;charset=UTF-8"));
        }
    }

    @Test void keycloakUserUpdateMapUsesJson() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(JacksonConfig.class, FeignConfig.class)) {
            var request = new RequestTemplate();
            request.header("Content-Type", "application/json");
            context.getBean(Encoder.class).encode(Map.of("enabled", false), Map.class, request);
            assertFalse(context.getBean(ObjectMapper.class).readTree(request.body()).get("enabled").asBoolean());
            assertTrue(request.headers().get("Content-Type").contains("application/json"));
        }
    }

    @Test void nonFormRequestIsSerializedAsJson() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(JacksonConfig.class, FeignConfig.class)) {
            var request = new RequestTemplate();
            context.getBean(Encoder.class).encode(new Payload("hello"), Payload.class, request);
            assertEquals("hello", context.getBean(ObjectMapper.class).readTree(request.body()).get("message").asText());
            assertTrue(request.headers().get("Content-Type").contains("application/json"));
        }
    }
    @Test void bootFourCreatesKafkaTemplateForIdentity() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration.class))
                .withPropertyValues("spring.kafka.bootstrap-servers=localhost:9092",
                        "spring.kafka.listener.auto-startup=false")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertNotNull(context.getBean(org.springframework.kafka.core.KafkaTemplate.class));
                });
    }
    record Payload(String message) {}
}
