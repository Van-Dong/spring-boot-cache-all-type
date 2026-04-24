package com.dongnv.democache.controller;

import com.dongnv.democache.config.httpcache.HttpCache;
import com.dongnv.democache.model.Product;
import com.dongnv.democache.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Product API - Demo Caffeine Cache + simple HTTP cache headers. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** GET Product by ID - Caffeine Cache + Cache-Control header. */
    @HttpCache(cacheControl = "private, max-age=1800")
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        Product product = productService.getProductById(id);
        long duration = System.currentTimeMillis() - startTime;

        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header("X-Response-Time", duration + "ms")
                .header("X-Cache-Type", "Caffeine")
                .body(product);
    }

    /** GET products by category - Cache-Control + Vary headers. */
    @HttpCache(cacheControl = "private, max-age=300", vary = {"Accept-Encoding"})
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
