package com.dongnv.democache.config.httpcache;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * HTTP Cache Response Body Advice - simple version
 *
 * Chỉ set header tĩnh từ @HttpCache.
 */
@RestControllerAdvice
public class HttpCacheResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    
    /**
     * Check if this advice applies
     */
    @Override
    public boolean supports(MethodParameter returnType,
                          Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(HttpCache.class);
    }
    
    /**
     * Set HTTP cache headers before writing response body
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                 MethodParameter returnType,
                                 MediaType selectedContentType,
                                 Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                 ServerHttpRequest request,
                                 ServerHttpResponse response) {
        
        HttpCache annotation = returnType.getMethodAnnotation(HttpCache.class);
        if (annotation == null) {
            return body;
        }
        
        HttpHeaders headers = response.getHeaders();
        
        // 1. Cache-Control header
        if (!annotation.cacheControl().isBlank()) {
            headers.set(HttpHeaders.CACHE_CONTROL, annotation.cacheControl());
        }

        // 2. Vary header
        if (annotation.vary().length > 0) {
            headers.set(HttpHeaders.VARY, String.join(", ", annotation.vary()));
        }
        
        return body;
    }
}
