package com.dongnv.democache.config;

import com.dongnv.democache.config.properties.RedisCacheProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisCacheProperties redisCacheProperties;

    /**
     * RedisTemplate phục vụ thao tác Redis mức thấp nếu cần custom logic ngoài Spring Cache abstraction.
     *
     * Serializer được chọn theo hướng production-friendly:
     * - key/hash key dùng String để dễ inspect trực tiếp trên Redis
     * - value/hash value dùng JSON để tránh JDK serialization khó debug và tốn dung lượng
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.setHashValueSerializer(jsonSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * RedisCacheManager là CacheManager cho cache distributed/shared giữa nhiều instance.
     *
     * Các cấu hình quan trọng:
     * - entryTtl(defaultTtl): TTL mặc định cho cache không có rule riêng
     * - computePrefixWith(keyPrefix): namespace key để tránh collision giữa app/cache domains
     * - disableCachingNullValues(): tránh giữ null trong Redis và gây stale negative cache
     * - serializeValuesWith(JSON): dữ liệu trong Redis dễ đọc hơn, giảm phụ thuộc JDK serialization
     * - withInitialCacheConfigurations(...): override TTL theo từng cache domain
     * - transactionAware(): chỉ update cache sau khi transaction thành công, giảm risk lệch dữ liệu
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(redisCacheProperties.defaultTtl())
                .computePrefixWith(cacheName -> redisCacheProperties.keyPrefix() + cacheName + "::")
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(redisObjectMapper())
                ));

        Map<String, RedisCacheConfiguration> cacheConfigs = redisCacheProperties.caches().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> defaultConfig.entryTtl(entry.getValue().ttl())
                ));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }

    /**
     * ObjectMapper riêng cho Redis JSON serialization.
     *
     * - JavaTimeModule: hỗ trợ LocalDateTime/Instant/... ổn định
     * - WRITE_DATES_AS_TIMESTAMPS disabled: giữ date ở dạng ISO dễ đọc
     * - FAIL_ON_UNKNOWN_PROPERTIES disabled: giảm rủi ro khi model evolve giữa các version deploy
     * - NON_NULL: giảm payload không cần thiết trong Redis
     */
    private ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setDefaultPropertyInclusion(
                JsonInclude.Value.construct(
                        JsonInclude.Include.NON_NULL,
                        JsonInclude.Include.NON_NULL
                )
        );

        return objectMapper;
    }
}
