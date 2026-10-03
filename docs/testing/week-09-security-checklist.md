# Security checklist Tuần 09

| Kiểm tra | Kết quả | Bằng chứng / ghi chú |
|---|---|---|
| Endpoint private không authentication | PASS | MVC HTTP test: `/notifications` → 401 |
| Customer không gọi admin support endpoint | PASS | MVC HTTP test: `CUSTOMER` → 403 |
| Permission phù hợp được chấp nhận | PASS | MVC HTTP test: `SUPPORT_REQUEST_VIEW` → 200 |
| Public category endpoint truy cập không token | PASS | MVC HTTP test: `/service-categories` → 200 |
| User inactive không nhận authority từ token role | PASS | Unit test converter |
| Local authority lookup lỗi không fallback sang role trong token | PASS | Unit test converter; sửa nhánh fail-open |
| Chống IDOR cho customer ticket | PASS ở unit | Repository lookup theo cả ticket ID và customer ID; cần thêm HTTP/Postman case |
| Notification ownership | Có unit test hiện hữu/bổ sung | Chưa chạy toàn bộ suite |
| Swagger tắt trên production profile | Cấu hình đã thêm | Chưa xác minh runtime với `prod` |
| CORS production origins | Chưa xác minh | Security config hiện có danh sách localhost; cần rà cấu hình deploy |
| Không có stack trace trong API error | Có unit test handler hiện hữu | Chưa chạy full suite lượt này |
| Postman credentials | Cần xử lý | Collection đang nhúng credential mẫu; chuyển vào environment secret trước khi chia sẻ |
| Dependency audit | Chưa chạy | Chưa chạy npm audit/Maven vulnerability scan |

Đây là kiểm tra bảo mật cơ bản, không phải penetration test.
