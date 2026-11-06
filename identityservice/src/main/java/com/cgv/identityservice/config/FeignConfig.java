package com.cgv.identityservice.config;

import feign.Logger;
import feign.codec.Encoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

@Configuration
public class FeignConfig {

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Encoder feignEncoder(ObjectMapper objectMapper) {
        return (object, bodyType, template) -> {
            if (object instanceof Map<?, ?> map && template.headers().getOrDefault("Content-Type", java.util.List.of())
                    .stream().anyMatch(value -> value.startsWith("application/x-www-form-urlencoded"))) {
                StringBuilder builder = new StringBuilder();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object val = entry.getValue();
                    if (val instanceof Collection<?> collection) {
                        for (Object item : collection) {
                            appendParam(builder, entry.getKey(), item);
                        }
                    } else {
                        appendParam(builder, entry.getKey(), val);
                    }
                }
                template.header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                template.body(builder.toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            } else {
                try {
                    template.header("Content-Type", "application/json");
                    template.body(objectMapper.writeValueAsBytes(object), StandardCharsets.UTF_8);
                } catch (java.io.IOException ex) {
                    throw new feign.codec.EncodeException("Cannot serialize request", ex);
                }
            }
        };
    }

    private static void appendParam(StringBuilder builder, Object key, Object value) {
        if (!builder.isEmpty()) {
            builder.append("&");
        }
        builder.append(URLEncoder.encode(key.toString(), StandardCharsets.UTF_8))
                .append("=")
                .append(URLEncoder.encode(value != null ? value.toString() : "", StandardCharsets.UTF_8));
    }
}
