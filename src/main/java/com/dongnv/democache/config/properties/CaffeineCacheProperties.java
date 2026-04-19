package com.dongnv.democache.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.cache.caffeine")
public record CaffeineCacheProperties(
        @Name("default") DefaultCacheProperties defaultConfig,
        Map<String, CacheProperties> caches
) {

    public CaffeineCacheProperties {
        defaultConfig = defaultConfig != null
                ? defaultConfig
                : new DefaultCacheProperties(10_000L, Duration.ofMinutes(10));
        caches = caches != null ? new LinkedHashMap<>(caches) : new LinkedHashMap<>();
    }

    public record DefaultCacheProperties(
            Long maximumSize,
            Duration ttl
    ) {

        public DefaultCacheProperties {
            maximumSize = maximumSize != null ? maximumSize : 10_000L;
            ttl = ttl != null ? ttl : Duration.ofMinutes(10);
        }
    }

    public record CacheProperties(
            Long maximumSize,
            Duration ttl
    ) {
    }
}
