package com.cgv.identityservice.config;

import feign.Logger;
import feign.codec.Encoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
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
        return Logger.Level.FULL;
    }

    @Bean
    public Encoder feignEncoder(ObjectFactory<HttpMessageConverters> converters) {
        Encoder defaultEncoder = new SpringEncoder(converters);
        return (object, bodyType, template) -> {
            if (object instanceof Map<?, ?> map) {
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
                defaultEncoder.encode(object, bodyType, template);
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
