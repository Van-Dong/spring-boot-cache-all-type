package com.dongnv.democache.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.cache.redis")
public record RedisCacheProperties(
        Duration defaultTtl,
        String keyPrefix,
        Map<String, CacheProperties> caches
) {

    public RedisCacheProperties {
        defaultTtl = defaultTtl != null ? defaultTtl : Duration.ofMinutes(10);
        keyPrefix = keyPrefix != null ? keyPrefix : "myapp::";
        caches = caches != null ? new LinkedHashMap<>(caches) : new LinkedHashMap<>();
    }

    public record CacheProperties(Duration ttl) { // Tuy chỉ có 1 trường ttl nhưng vẫn dùng record để cho mở rộng sau này
    }
}
