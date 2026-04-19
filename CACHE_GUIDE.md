# Demo Cache: Caffeine (Local) vs Redis (Distributed)

## 📋 Tổng quan

Project demo Spring Cache với 2 loại cache:
- **Caffeine Cache** (L1): Local in-memory cache - nhanh nhất
- **Redis Cache** (L2): Distributed cache - scalable, persistent

## ✅ Review CaffeineConfig - Production Best Practices

### 1. **Multi-Domain Cache Configuration** ✅
```java
// Mỗi domain có TTL và size riêng
- USER_CACHE: 30 phút, 1K entries
- PRODUCT_CACHE: 1 giờ, 5K entries  
- ORDER_CACHE: 10 phút, 2K entries (sensitive data)
- CONFIG_CACHE: 2 giờ, 500 entries (stable data)
- SESSION_CACHE: 1 giờ, 1K entries
- CATEGORY_CACHE: 3 giờ, 200 entries
- DICTIONARY_CACHE: 6 giờ, 100 entries
```

### 2. **Production Features** ✅
- ✅ `@Primary` - Ưu tiên Caffeine làm default cache manager
- ✅ `removalListener()` - Log cache evictions để monitoring
- ✅ `recordStats()` - Track hit/miss ratio cho metrics
- ✅ `expireAfterWrite()` - TTL per domain
- ✅ `expireAfterAccess()` - LRU eviction
- ✅ `maximumSize()` - Memory limit (tránh OOM)
- ✅ `setAllowNullValues(false)` - Không cache null
- ✅ `setCaffeine(caffeineConfig())` - Fallback config

### 3. **Best Practices Tuning** ✅
```java
// TTL theo business rule:
- Sensitive data (order, user): TTL ngắn (10-30 phút)
- Stable data (config, dictionary): TTL dài (2-6 giờ)
- Frequently accessed (product): TTL vừa (1 giờ)

// Memory Management:
- maximumSize theo RAM available và data size
- expireAfterAccess để evict data không dùng
- softValues() có thể dùng nếu cần GC cleanup
```

### 4. **Monitoring & Observability** ✅
```java
// Production monitoring:
- removalListener → log evictions (SIZE, EXPIRED)
- recordStats() → expose metrics qua /actuator/metrics
- SLF4J Logger → debug cache behavior
```

---

## 🚀 API Endpoints

### **Product API** (Caffeine Cache)
```bash
# GET Product (cache miss → cache hit)
GET http://localhost:8080/api/products/1
GET http://localhost:8080/api/products/2

# GET by Category
GET http://localhost:8080/api/products/category/Electronics

# UPDATE Product (cache put)
PUT http://localhost:8080/api/products/1
{
  "name": "iPhone 15 Pro Max",
  "description": "Updated description",
  "price": 1099.99,
  "category": "Electronics"
}

# DELETE Product (cache evict)
DELETE http://localhost:8080/api/products/1

# CLEAR ALL Product Cache
DELETE http://localhost:8080/api/products/cache/clear
```

### **User API** (Redis Cache)
```bash
# GET User (Redis cache miss → cache hit)
GET http://localhost:8080/api/users/1
GET http://localhost:8080/api/users/2

# GET by Username
GET http://localhost:8080/api/users/username/john_doe

# UPDATE User (Redis cache put)
PUT http://localhost:8080/api/users/1
{
  "username": "john_doe_updated",
  "email": "john.new@example.com",
  "fullName": "John Doe Updated",
  "role": "SUPER_ADMIN"
}

# DELETE User (Redis cache evict)
DELETE http://localhost:8080/api/users/1

# CLEAR ALL User Redis Cache
DELETE http://localhost:8080/api/users/cache/clear
```

---

## 🧪 Test Scenarios

### **Scenario 1: Cache Hit Performance**
```bash
# Lần 1: Cache miss (chậm ~1000ms)
curl -w "\nTime: %{time_total}s\n" http://localhost:8080/api/products/1

# Lần 2: Cache hit (nhanh <10ms)
curl -w "\nTime: %{time_total}s\n" http://localhost:8080/api/products/1
```

**Kết quả mong đợi:**
- Lần 1: ~1000ms (query DB)
- Lần 2: <10ms (cache hit từ Caffeine)

### **Scenario 2: Cache Eviction**
```bash
# 1. GET product → cache miss
curl http://localhost:8080/api/products/1

# 2. GET lại → cache hit (nhanh)
curl http://localhost:8080/api/products/1

# 3. DELETE product → evict cache
curl -X DELETE http://localhost:8080/api/products/1

# 4. GET lại → cache miss (không tìm thấy)
curl http://localhost:8080/api/products/1
```

### **Scenario 3: Cache Update**
```bash
# 1. GET product → cache data
curl http://localhost:8080/api/products/1

# 2. UPDATE product → update cache
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Updated","price":999.99,"category":"Electronics"}'

# 3. GET lại → cache hit với data mới
curl http://localhost:8080/api/products/1
```

### **Scenario 4: Redis vs Caffeine Performance**
```bash
# Caffeine (Local): <10ms
curl -w "\nTime: %{time_total}s\n" http://localhost:8080/api/products/1

# Redis (Distributed): ~20-50ms (network latency)
curl -w "\nTime: %{time_total}s\n" http://localhost:8080/api/users/1
```

---

## 📊 Caffeine vs Redis - Khi nào dùng gì?

### **Caffeine Cache** (L1 - Local)
✅ **Dùng khi:**
- Read-heavy workload (đọc nhiều gấp 10-100 lần ghi)
- Data size nhỏ (fit vào RAM 1 instance)
- Latency cực thấp (<5ms)
- Single instance hoặc stateless app

❌ **KHÔNG dùng khi:**
- Cần share cache giữa nhiều instances
- Data size lớn hơn RAM của 1 instance
- Cần persistent cache (survive restart)

### **Redis Cache** (L2 - Distributed)
✅ **Dùng khi:**
- Cần share cache giữa nhiều instances (distributed)
- Data size lớn (scalable)
- Cần persistent cache
- Session management
- Rate limiting, distributed lock

❌ **KHÔNG dùng khi:**
- Latency requirement <5ms (Redis ~20-50ms)
- Single instance app (Caffeine đủ)
- Cost-sensitive (Redis cần infrastructure)

---

## 🔍 Monitoring Cache Performance

### **1. Check Caffeine Stats**
```bash
# Thêm Spring Actuator để expose metrics
GET http://localhost:8080/actuator/metrics/cache.gets
GET http://localhost:8080/actuator/metrics/cache.puts
GET http://localhost:8080/actuator/metrics/cache.evictions
```

### **2. Log Analysis**
```log
# Cache miss (chậm)
⚠️ CACHE MISS - Fetching product 1 from DATABASE (slow operation)

# Cache eviction (monitoring)
DEBUG Cache [PRODUCT_CACHE] evicted: key=1, cause=EXPIRED
```

### **3. Response Headers**
```http
X-Response-Time: 1000ms  # Cache miss
X-Response-Time: 5ms     # Cache hit
X-Cache-Type: Caffeine
```

---

## 🎯 Production Checklist

### **Caffeine Config**
- [x] Multi-domain cache với TTL riêng
- [x] maximumSize để tránh OOM
- [x] recordStats() cho monitoring
- [x] removalListener() để log evictions
- [x] expireAfterWrite/expireAfterAccess
- [x] @Primary để set default cache manager
- [x] setAllowNullValues(false)

### **Redis Config**
- [x] Jackson serializer (JSON)
- [x] TTL per domain
- [x] transactionAware()
- [x] Connection pool tuning
- [x] Sentinel/Cluster setup

### **Application**
- [x] @EnableCaching
- [x] @Cacheable với cacheManager rõ ràng
- [x] @CachePut cho update
- [x] @CacheEvict cho delete
- [x] Response time logging

---

## 💡 Tips & Tricks

### **1. Cache Key Design**
```java
// ✅ Good: Simple, readable
@Cacheable(value = "PRODUCT_CACHE", key = "#productId")

// ✅ Good: Composite key
@Cacheable(value = "ORDER_CACHE", key = "#userId + ':' + #orderId")

// ❌ Bad: Complex SpEL
@Cacheable(key = "#root.methodName + #root.args[0]")
```

### **2. TTL Tuning**
```java
// Sensitive data: TTL ngắn
ORDER_CACHE: 10 phút

// Stable data: TTL dài
CONFIG_CACHE: 2-6 giờ

// Frequently updated: TTL vừa
PRODUCT_CACHE: 1 giờ
```

### **3. Memory Sizing**
```java
// Formula: maxSize = (Available RAM * 0.3) / Avg Entry Size
// Example: 4GB RAM, 1KB entry → maxSize = (4000MB * 0.3) / 1KB = 1,200,000
```

### **4. Monitoring Alerts**
```yaml
# Alert khi:
- Hit ratio < 80% → cache không hiệu quả
- Eviction rate cao → cần tăng maxSize
- Response time > 100ms → cache miss nhiều
```

---

## 🚀 Run Application

```bash
# 1. Start Redis (nếu cần test Redis cache)
docker-compose up -d redis

# 2. Build & Run
mvn clean install
mvn spring-boot:run

# 3. Test API
curl http://localhost:8080/api/products/1
```

---

## 📚 Tài liệu tham khảo

- [Caffeine Cache](https://github.com/ben-manes/caffeine)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html)
- [Redis Cache Best Practices](https://redis.io/docs/manual/client-side-caching/)
