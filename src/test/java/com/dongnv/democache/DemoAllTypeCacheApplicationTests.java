package com.dongnv.democache;

import com.dongnv.democache.config.properties.CaffeineCacheProperties;
import com.dongnv.democache.config.properties.RedisCacheProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DemoAllTypeCacheApplicationTests {

    @Autowired
    private CaffeineCacheManager caffeineCacheManager;

    @Autowired
    private RedisCacheManager redisCacheManager;

    @Autowired
    private CaffeineCacheProperties caffeineCacheProperties;

    @Autowired
    private RedisCacheProperties redisCacheProperties;

    @Test
    void contextLoads() {
        assertThat(caffeineCacheManager).isNotNull();
        assertThat(redisCacheManager).isNotNull();
    }

    @Test
    void caffeineCachePoliciesComeFromApplicationYaml() {
        CaffeineCache productCache = (CaffeineCache) caffeineCacheManager.getCache("PRODUCT_CACHE");
        CaffeineCache categoryCache = (CaffeineCache) caffeineCacheManager.getCache("CATEGORY_CACHE");
        CaffeineCache fallbackCache = (CaffeineCache) caffeineCacheManager.getCache("UNCONFIGURED_CACHE");

        assertThat(productCache).isNotNull();
        assertThat(categoryCache).isNotNull();
        assertThat(fallbackCache).isNotNull();

        assertThat(productCache.getNativeCache().policy().eviction().orElseThrow().getMaximum())
                .isEqualTo(5_000L);
        assertThat(productCache.getNativeCache().policy().expireAfterWrite().orElseThrow().getExpiresAfter())
                .isEqualTo(Duration.ofHours(1));
        assertThat(categoryCache.getNativeCache().policy().eviction().orElseThrow().getMaximum())
                .isEqualTo(200L);
        assertThat(categoryCache.getNativeCache().policy().expireAfterWrite().orElseThrow().getExpiresAfter())
                .isEqualTo(Duration.ofHours(3));
        assertThat(fallbackCache.getNativeCache().policy().eviction().orElseThrow().getMaximum())
                .isEqualTo(caffeineCacheProperties.defaultConfig().maximumSize());
        assertThat(fallbackCache.getNativeCache().policy().expireAfterWrite().orElseThrow().getExpiresAfter())
                .isEqualTo(caffeineCacheProperties.defaultConfig().ttl());
    }

    @Test
    void redisCachePoliciesComeFromApplicationYaml() {
        RedisCacheConfiguration defaultConfig = (RedisCacheConfiguration) ReflectionTestUtils.invokeMethod(
                redisCacheManager,
                "getDefaultCacheConfiguration"
        );
        @SuppressWarnings("unchecked")
        Map<String, RedisCacheConfiguration> initialConfigs = (Map<String, RedisCacheConfiguration>) ReflectionTestUtils.invokeMethod(
                redisCacheManager,
                "getInitialCacheConfiguration"
        );

        assertThat(initialConfigs).containsKeys("users", "products");
        assertThat(initialConfigs.get("users").getTtlFunction().getTimeToLive("k", "v"))
                .isEqualTo(Duration.ofMinutes(5));
        assertThat(initialConfigs.get("products").getTtlFunction().getTimeToLive("k", "v"))
                .isEqualTo(Duration.ofHours(1));
        assertThat(initialConfigs.get("users").getKeyPrefixFor("users"))
                .isEqualTo("myapp::users::");
        assertThat(initialConfigs.get("users").getAllowCacheNullValues())
                .isFalse();
        assertThat(defaultConfig.getTtlFunction().getTimeToLive("k", "v"))
                .isEqualTo(redisCacheProperties.defaultTtl());
        assertThat(defaultConfig.getKeyPrefixFor("unknown"))
                .isEqualTo("myapp::unknown::");
        assertThat(defaultConfig.getAllowCacheNullValues())
                .isFalse();
    }
}
