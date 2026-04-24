package com.dongnv.democache.config.httpcache;

import java.lang.annotation.*;

/**
 * HTTP Cache annotation - simple policy for demo
 *
 * Chỉ khai báo headers tĩnh cần set trên response.
 * 
 * Usage:
 * 
 * @HttpCache(cacheControl = "private, max-age=60", vary = {"Accept-Encoding"})
 * @GetMapping("/products")
 * public List<ProductDto> getProducts(...) { ... }
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HttpCache {
    
    /**
     * Cache-Control header value
     * 
     * Examples:
     * - "private, max-age=60"
     * - "public, max-age=3600, s-maxage=7200"
     * - "no-cache, must-revalidate"
     * 
     * Default: empty (no cache-control header)
     */
    String cacheControl() default "";
    
    /**
     * Vary header
     * 
     * Examples:
     * - {"Accept-Encoding", "Accept-Language"}
     * - {"Origin"}
     * 
     * Default: empty (no Vary header)
     */
    String[] vary() default {};
}
