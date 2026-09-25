package com.cgv.catalogservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        // Hot home page / movie list caches (30 minutes)
        cacheConfigs.put("movies:now-showing", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("movies:coming-soon", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("movies:detail", defaultConfig.entryTtl(Duration.ofHours(2)));

        // Genres rarely change (24 hours)
        cacheConfigs.put("genres:all", defaultConfig.entryTtl(Duration.ofHours(24)));

        // Cinema schedules (10 minutes)
        cacheConfigs.put("cinemaSchedule", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("showtimesByMovieAndDate", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("showtimesByCinemaAndDate", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Cinema lists & seats
        cacheConfigs.put("cinemas:all", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put("cinemasByRegion", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put("seatsByRoom", defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}