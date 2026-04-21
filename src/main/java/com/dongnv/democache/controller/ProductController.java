package com.dongnv.democache.controller;

import com.dongnv.democache.model.Product;
import com.dongnv.democache.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Product API - Demo Caffeine Cache
 *
 * Test Scenarios:
 * 1. GET /api/products/1 -> lần 1: cache miss (chậm 1s), lần 2: cache hit (nhanh)
 * 2. PUT /api/products -> update cache
 * 3. DELETE /api/products/1 -> evict cache
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET Product by ID - Caffeine Cache
     * Test: Call API 2 lần, lần 2 sẽ nhanh hơn (cache hit)
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        Product product = productService.getProductById(id);
        long duration = System.currentTimeMillis() - startTime;

        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        // Response header để debug cache performance.
        return ResponseEntity.ok()
                .header("X-Response-Time", duration + "ms")
                .header("X-Cache-Type", "Caffeine")
                .body(product);
    }

    /**
     * GET Products by Category - Category Cache
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        long startTime = System.currentTimeMillis();
        List<Product> products = productService.getProductsByCategory(category);
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok()
                .header("X-Response-Time", duration + "ms")
                .header("X-Cache-Type", "Caffeine-Category")
                .body(products);
    }

    /**
     * UPDATE Product - Cache Put
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        Product updated = productService.updateProduct(product);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE Product - Cache Evict
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * CLEAR ALL Product Cache - để test lại cache miss
     */
    @DeleteMapping("/cache/clear")
    public ResponseEntity<String> clearCache() {
        productService.clearAllProductCache();
        return ResponseEntity.ok("Product cache cleared");
    }
}
