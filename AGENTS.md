# Agent Guidance for demoalltypecache

This is a **Spring Boot multi-tier caching demonstration** project showcasing production-grade cache strategies. This document helps AI agents understand the architecture and conventions to be immediately productive.

## Quick Start

| Command | Purpose |
|---------|---------|
| `./mvnw clean package` | Build the project |
| `./mvnw spring-boot:run` | Run the application (`:8080`) |
| `GET /api/products/{id}` | Test endpoint (returns cached product) |

**Key Files**:
- [pom.xml](pom.xml) - Maven configuration
- [src/main/resources/application.yaml](src/main/resources/application.yaml) - Cache and Redis configuration
- [DemoAllTypeCacheApplication.java](src/main/java/com/dongnv/democache/DemoAllTypeCacheApplication.java) - Entry point with `@EnableCaching`

## Architecture Overview

```
controllers/                    # HTTP endpoints (ProductController, UserController)
  └─ @GetMapping with @HttpCache annotations + X-Cache-Type headers

services/                       # Business logic with cache annotations
  ├─ @Cacheable("CACHE_NAME") on queries → checks cache before execution
  └─ @CacheEvict on mutations  → invalidates cache after updates

config/                         # Caching and infrastructure setup
  ├─ CaffeineConfig.java       → L1 cache: Caffeine (in-memory, per-server)
  ├─ RedisConfig.java          → L2 cache: Redis (distributed, persistent)
  ├─ HttpCacheConfiguration.java → HTTP cache headers (Cache-Control, Vary)
  └─ httpcache/
      ├─ HttpCache.java         → Custom annotation for HTTP header policies
      └─ HttpCacheResponseBodyAdvice.java → Injects cache headers into responses

models/                         # Entities (Product, User)
  └─ Must implement Serializable for Redis caching
```

## Key Conventions

### Cache Naming & Registration

All caches are registered in [application.yaml](src/main/resources/application.yaml):

```yaml
cache:
  caffeine:
    PRODUCT_CACHE: "expireAfterWrite=1h,maximumSize=5000"
    USER_CACHE: "expireAfterWrite=30m,maximumSize=10000"
    ORDER_CACHE: "expireAfterWrite=10m,maximumSize=1000"
```

**Naming pattern**: `UPPERCASE_CACHE` (e.g., `PRODUCT_CACHE`, `USER_CACHE`, `ORDER_CACHE`)

### Using @Cacheable in Services

```java
@Cacheable("PRODUCT_CACHE")
public Product getProductById(Long id) {
    return productRepository.findById(id).orElse(null);
}
```

- Checks `PRODUCT_CACHE` first
- If miss, executes method and stores result
- Uses method parameters as cache key (customizable with `key = "#id"`)

### Invalidating Cache with @CacheEvict

```java
@CacheEvict("PRODUCT_CACHE", allEntries = true)
public void updateProduct(Product product) {
    productRepository.save(product);
}
```

- `allEntries = true` → clears entire cache
- `key = "#id"` → clears specific entry

### HTTP Cache Headers

Use the `@HttpCache` annotation on controller methods:

```java
@GetMapping("/{id}")
@HttpCache(cacheControl = "public, max-age=3600")
public Product getProduct(@PathVariable Long id) {
    return service.getProductById(id);
}
```

Injected via [HttpCacheResponseBodyAdvice.java](src/main/java/com/dongnv/democache/config/httpcache/HttpCacheResponseBodyAdvice.java).

## TTL Strategy

| Data Type | Caffeine TTL | Redis TTL | Reasoning |
|-----------|-------------|-----------|-----------|
| Sensitive (User session, config) | 10-30m | 10-30m | Reduce stale data, security |
| Regular (Products, Orders) | 1h | 1h | Balance freshness & performance |
| Stable (Categories, reference data) | 6h | 6h | Rarely changes, long-lived |

Configure in [application.yaml](src/main/resources/application.yaml) under `cache.caffeine.*`

## Critical Constraints & Pitfalls

⚠️ **DO NOT IGNORE**:

1. **Serialization for Redis**: All cached entities must implement `Serializable`
   - See [Product.java](src/main/java/com/dongnv/democache/model/Product.java) for correct pattern

2. **Null value handling**: [CaffeineConfig.java](src/main/java/com/dongnv/democache/config/CaffeineConfig.java) sets `setAllowNullValues(false)`
   - Do NOT cache null values (prevents `null` pointer issues)

3. **Maximum size limits**: Every cache has `maximumSize` to prevent heap exhaustion
   - Example: `PRODUCT_CACHE: maximumSize=5000` in YAML
   - If adding new cache, always set `maximumSize`

4. **Redis Sentinel readiness**: [application.yaml](src/main/resources/application.yaml) defaults to `localhost:26379-26381`
   - Update before production deployment

5. **Cache stampede risk**: High-traffic queries may hit DB simultaneously on cache miss
   - Not currently handled; consider adding sync locks in service layer if needed

## Adding a New Cached Entity

Follow these steps:

1. **Register cache in [application.yaml](src/main/resources/application.yaml)**:
   ```yaml
   cache:
     caffeine:
       CATEGORY_CACHE: "expireAfterWrite=6h,maximumSize=500"
   ```

2. **Make model Serializable**: Implement `Serializable` if storing in Redis

3. **Add @Cacheable to service query method**:
   ```java
   @Cacheable("CATEGORY_CACHE")
   public Category getCategoryById(Long id) { ... }
   ```

4. **Add @CacheEvict to mutation methods**:
   ```java
   @CacheEvict("CATEGORY_CACHE", allEntries = true)
   public void saveCategory(Category category) { ... }
   ```

5. **Optional: Add @HttpCache to controller endpoint** for browser caching

## Related Documentation

- [CACHE_GUIDE.md](CACHE_GUIDE.md) - Best practices, monitoring, TTL tuning details
- [http-cache.md](http-cache.md) - HTTP cache header semantics
- [api-test.http](api-test.http) - Sample REST requests for testing

## Testing

- [DemoAllTypeCacheApplicationTests.java](src/test/java/com/dongnv/democache/DemoAllTypeCacheApplicationTests.java) - Basic test scaffold
- To test cache behavior: make repeated requests and observe response times and `X-Cache-Type` headers

## When to Ask for Clarification

Agents should ask for clarification if:
- Adding a cache with unclear TTL (ask: "Is this data sensitive or stable?")
- Unsure about serialization requirements (ask: "Will this be cached in Redis?")
- Cache strategy affects API contract (ask: "Should this endpoint be browser-cacheable?")

## Configuration Properties

See [application.yaml](src/main/resources/application.yaml) for:
- Redis Sentinel nodes and failover configuration
- Caffeine cache sizes and expiration policies
- Connection pooling (commons-pool2) settings
- HTTP cache default max-age values
