# HTTP Cache (Simple Mode)

Tài liệu này mô tả luồng HTTP cache đã được tối giản cho demo: không dùng ETag và không dùng Last-Modified.

Mục tiêu:

- code gọn
- dễ đọc, dễ maintain
- tránh over-engineering với bài toán demo đơn giản

## 1. Kiến trúc hiện tại

Chỉ còn 2 thành phần:

1. `@HttpCache` annotation để khai báo policy header
2. `HttpCacheResponseBodyAdvice` để set header vào response

Không còn:

- Interceptor check conditional request
- ETag provider layer
- Last-Modified validator
- So sánh If-None-Match / If-Modified-Since

## 2. Luồng xử lý request

Ví dụ `GET /api/products/1`:

1. Request vào controller
2. Controller xử lý business và trả body
3. `HttpCacheResponseBodyAdvice` đọc `@HttpCache`
4. Advice set header:
   - `Cache-Control`
   - `Vary` (nếu có)
5. Response trả về `200 OK` + body

Không có short-circuit `304 Not Modified`.

## 3. Annotation su dung

File: `src/main/java/com/dongnv/democache/config/httpcache/HttpCache.java`

Annotation hiện tại chỉ giữ:

- `cacheControl`
- `vary`

Ví dụ:

```java
@HttpCache(cacheControl = "private, max-age=300", vary = {"Accept-Encoding"})
@GetMapping("/category/{category}")
public ResponseEntity<List<Product>> getProductsByCategory(...) {
	...
}
```

## 4. ResponseBodyAdvice

File: `src/main/java/com/dongnv/democache/config/httpcache/HttpCacheResponseBodyAdvice.java`

Trách nhiệm:

- set `Cache-Control`
- set `Vary`

Không tính validator và không cần metadata query phụ.

## 5. Trade-off hien tai

Ưu điểm:

- gọn hơn rõ ràng
- không tốn thêm effort query metadata để tính validator
- phù hợp bài toán demo và team nhỏ

Đánh đổi:

- mỗi request vẫn vào controller/service
- không có 304 để giảm băng thông response body

## 6. Checklist them endpoint moi

1. Gắn `@HttpCache` vào endpoint cần cache browser.
2. Đặt `cacheControl` theo mục tiêu (private/public, max-age).
3. Thêm `vary` nếu representation phụ thuộc header (ví dụ `Accept-Encoding`).
4. Không cần viết provider, không cần tính hash.

## 7. Test nhanh

```bash
curl -i http://localhost:8080/api/products/1
```

Kỳ vọng:

- Có `Cache-Control`
- Có `Vary` nếu endpoint có khai báo
- Không có `ETag`
- Không có `Last-Modified`
