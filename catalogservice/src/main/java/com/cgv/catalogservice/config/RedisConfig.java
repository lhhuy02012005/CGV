package com.cgv.catalogservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory redisConnectionFactory
    ) {

        RedisTemplate<String, Object> redisTemplate =
                new RedisTemplate<>();

        RedisSerializer<String> stringSerializer =
                RedisSerializer.string();

        RedisSerializer<Object> jsonSerializer =
                RedisSerializer.json();

        redisTemplate.setConnectionFactory(
                redisConnectionFactory
        );

        redisTemplate.setKeySerializer(
                stringSerializer
        );

        redisTemplate.setHashKeySerializer(
                stringSerializer
        );

        redisTemplate.setValueSerializer(
                jsonSerializer
        );

        redisTemplate.setHashValueSerializer(
                jsonSerializer
        );

        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory redisConnectionFactory
    ) {

        RedisSerializer<String> stringSerializer =
                RedisSerializer.string();

        RedisSerializer<Object> jsonSerializer =
                RedisSerializer.json();

        RedisCacheConfiguration cacheConfiguration =
                RedisCacheConfiguration
                        .defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .disableCachingNullValues()
                        .serializeKeysWith(
                                RedisSerializationContext
                                        .SerializationPair
                                        .fromSerializer(
                                                stringSerializer
                                        )
                        )
                        .serializeValuesWith(
                                RedisSerializationContext
                                        .SerializationPair
                                        .fromSerializer(
                                                jsonSerializer
                                        )
                        );

        return RedisCacheManager
                .builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                .build();
    }
}