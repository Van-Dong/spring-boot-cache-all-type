package com.dongnv.democache.service;

import com.dongnv.democache.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Product Service - Demo Caffeine Cache
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final Map<Long, Product> database = new HashMap<>();

    public ProductService() {
        database.put(1L, new Product(1L, "iPhone 15 Pro", "Latest Apple flagship", new BigDecimal("999.99"), "Electronics"));
        database.put(2L, new Product(2L, "MacBook Pro M3", "16-inch professional laptop", new BigDecimal("2499.99"), "Electronics"));
        database.put(3L, new Product(3L, "AirPods Pro 2", "Premium wireless earbuds", new BigDecimal("249.99"), "Electronics"));
        database.put(4L, new Product(4L, "Nike Air Max", "Running shoes", new BigDecimal("129.99"), "Fashion"));
        database.put(5L, new Product(5L, "Levi's Jeans", "Classic blue jeans", new BigDecimal("59.99"), "Fashion"));
    }

    @Cacheable(value = "PRODUCT_CACHE", key = "#productId", cacheManager = "caffeineCacheManager")
    public Product getProductById(Long productId) {
        log.info("CACHE MISS - Fetching product {} from DATABASE (slow operation)", productId);
        simulateSlowQuery();
        return database.get(productId);
    }

    @Cacheable(value = "CATEGORY_CACHE", key = "#category", cacheManager = "caffeineCacheManager")
    public List<Product> getProductsByCategory(String category) {
        log.info("CACHE MISS - Fetching products by category '{}' from DATABASE", category);
        simulateSlowQuery();
        return database.values().stream()
                .filter(p -> p.getCategory().equals(category))
                .toList();
    }

    @CachePut(value = "PRODUCT_CACHE", key = "#product.id", cacheManager = "caffeineCacheManager")
    public Product updateProduct(Product product) {
        log.info("UPDATE product {} and refresh cache", product.getId());
        database.put(product.getId(), product);
        return product;
    }

    @CacheEvict(value = "PRODUCT_CACHE", key = "#productId", cacheManager = "caffeineCacheManager")
    public void deleteProduct(Long productId) {
        log.info("DELETE product {} and evict cache", productId);
        database.remove(productId);
    }

    @CacheEvict(value = "PRODUCT_CACHE", allEntries = true, cacheManager = "caffeineCacheManager")
    public void clearAllProductCache() {
        log.info("CLEAR all product cache");
    }

    private void simulateSlowQuery() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
