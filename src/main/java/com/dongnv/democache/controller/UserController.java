package com.dongnv.democache.controller;

import com.dongnv.democache.model.User;
import com.dongnv.democache.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User API - Demo Redis Cache (Distributed)
 *
 * Test Scenarios:
 * 1. GET /api/users/1 -> lần 1: Redis cache miss (chậm 800ms), lần 2: cache hit (nhanh <50ms)
 * 2. PUT /api/users -> update Redis cache
 * 3. DELETE /api/users/1 -> evict Redis cache
 *
 * Khác biệt với Caffeine:
 * - Redis: Distributed cache (share giữa nhiều instances)
 * - Redis: Persistent cache (survive qua restart)
 * - Redis: Slower (network latency ~1-5ms) nhưng scalable
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET User by ID - Redis Cache
     * Test: Call API 2 lần, lần 2 sẽ nhanh hơn (Redis cache hit)
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        User user = userService.getUserById(id);
        long duration = System.currentTimeMillis() - startTime;

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // Response header để debug cache performance.
        return ResponseEntity.ok()
                .header("X-Response-Time", duration + "ms")
                .header("X-Cache-Type", "Redis")
                .body(user);
    }

    /**
     * GET User by Username - Redis Cache
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        long startTime = System.currentTimeMillis();
        User user = userService.getUserByUsername(username);
        long duration = System.currentTimeMillis() - startTime;

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header("X-Response-Time", duration + "ms")
                .header("X-Cache-Type", "Redis")
                .body(user);
    }

    /**
     * UPDATE User - Cache Put (update Redis)
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        User updated = userService.updateUser(user);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE User - Cache Evict (xóa Redis cache)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * CLEAR ALL User Cache trong Redis - để test lại cache miss
     */
    @DeleteMapping("/cache/clear")
    public ResponseEntity<String> clearCache() {
        userService.clearAllUserCache();
        return ResponseEntity.ok("User Redis cache cleared");
    }
}
