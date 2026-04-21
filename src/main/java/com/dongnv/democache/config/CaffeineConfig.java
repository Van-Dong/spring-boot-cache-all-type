package com.dongnv.democache.config;

import com.dongnv.democache.config.properties.CaffeineCacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class CaffeineConfig {

    private final CaffeineCacheProperties caffeineCacheProperties;

    /**
     * Default CacheManager cho nhóm cache local in-memory.
     *
     * Mục tiêu của bean này là:
     * - cung cấp default policy cho các cache không khai báo riêng
     * - đăng ký custom policy theo từng cache domain như PRODUCT/CATEGORY/USER
     * - giữ toàn bộ Caffeine wiring ở 1 chỗ để service chỉ cần dùng cache name
     */
    @Bean
    @Primary
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        // Không cache null để tránh giữ lại kết quả "không tìm thấy" quá lâu trong local cache.
        caffeineCacheManager.setAllowNullValues(false);
        // Default builder áp dụng cho cache được tạo động nhưng không có custom registration riêng.
        caffeineCacheManager.setCaffeine(defaultCaffeine());

        caffeineCacheProperties.caches().forEach((cacheName, cacheProperties) ->
                caffeineCacheManager.registerCustomCache(cacheName, buildCache(cacheName, cacheProperties)));

        return caffeineCacheManager;
    }

    /**
     * Policy mặc định cho Caffeine cache.
     *
     * Các tham số quan trọng:
     * - maximumSize: chặn cache phình quá lớn trong heap
     * - expireAfterWrite(ttl): TTL đơn giản, dễ predict trong production
     * - recordStats: hữu ích khi cần theo dõi hit/miss qua metrics
     */
    private Caffeine<Object, Object> defaultCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(caffeineCacheProperties.defaultConfig().maximumSize())
                .expireAfterWrite(caffeineCacheProperties.defaultConfig().ttl())
                .recordStats();
    }

    /**
     * Tạo custom cache cho từng domain business.
     *
     * Domain cache thường override 2 thứ nhiều nhất:
     * - maximumSize: giới hạn footprint theo mức độ quan trọng / cardinality của data
     * - ttl: độ tươi dữ liệu theo business rule
     *
     * removalListener chỉ để hỗ trợ debug khi cache bị evict do size hoặc hết hạn.
     */
    private Cache<Object, Object> buildCache(String cacheName, CaffeineCacheProperties.CacheProperties cacheProperties) {
        return Caffeine.newBuilder()
                .maximumSize(cacheProperties.maximumSize() != null
                        ? cacheProperties.maximumSize()
                        : caffeineCacheProperties.defaultConfig().maximumSize())
                .expireAfterWrite(cacheProperties.ttl())
                .recordStats()
                .removalListener((key, value, cause) -> {
                    if (cause == RemovalCause.SIZE || cause == RemovalCause.EXPIRED) {
                        log.debug("Cache [{}] evicted: key={}, cause={}", cacheName, key, cause);
                    }
                })
                .build();
    }
}
