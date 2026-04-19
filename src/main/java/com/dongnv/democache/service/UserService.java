package com.dongnv.democache.service;

import com.dongnv.democache.config.CacheNames;
import com.dongnv.democache.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * User Service - Demo Redis Cache
 * Cache policy for users is externalized in application.yaml.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final Map<Long, User> database = new HashMap<>();

    public UserService() {
        database.put(1L, new User(1L, "john_doe", "john@example.com", "John Doe", "ADMIN"));
        database.put(2L, new User(2L, "jane_smith", "jane@example.com", "Jane Smith", "USER"));
        database.put(3L, new User(3L, "bob_wilson", "bob@example.com", "Bob Wilson", "USER"));
        database.put(4L, new User(4L, "alice_brown", "alice@example.com", "Alice Brown", "MODERATOR"));
    }

    @Cacheable(value = CacheNames.Redis.USERS, key = "#userId", cacheManager = "redisCacheManager")
    public User getUserById(Long userId) {
        log.info("REDIS CACHE MISS - Fetching user {} from DATABASE", userId);
        simulateSlowQuery();
        return database.get(userId);
    }

    @Cacheable(value = CacheNames.Redis.USERS, key = "'username:' + #username", cacheManager = "redisCacheManager")
    public User getUserByUsername(String username) {
        log.info("REDIS CACHE MISS - Fetching user by username '{}' from DATABASE", username);
        simulateSlowQuery();
        return database.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    @CachePut(value = CacheNames.Redis.USERS, key = "#user.id", cacheManager = "redisCacheManager")
    public User updateUser(User user) {
        log.info("UPDATE user {} and refresh Redis cache", user.getId());
        database.put(user.getId(), user);
        return user;
    }

    @CacheEvict(value = CacheNames.Redis.USERS, key = "#userId", cacheManager = "redisCacheManager")
    public void deleteUser(Long userId) {
        log.info("DELETE user {} and evict Redis cache", userId);
        database.remove(userId);
    }

    @CacheEvict(value = CacheNames.Redis.USERS, allEntries = true, cacheManager = "redisCacheManager")
    public void clearAllUserCache() {
        log.info("CLEAR all user cache in Redis");
    }

    private void simulateSlowQuery() {
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
