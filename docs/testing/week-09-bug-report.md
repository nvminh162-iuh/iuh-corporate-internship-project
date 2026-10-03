# Báo cáo lỗi Tuần 09

Ngày cập nhật: 2026-10-03

## Đã sửa

| ID | Mức độ | Vấn đề | Cách xử lý | Kiểm chứng |
|---|---|---|---|---|
| BUG-01 | High | Khi tra cứu user trong local database ném exception, JWT converter có thể coi user như chưa tồn tại và lấy role từ token làm fallback. | Chỉ fallback khi truy vấn thành công và xác nhận user không có trong local database. Khi lookup lỗi, không cấp authority. | `CustomJwtAuthenticationConverterTest`: lookup lỗi và user inactive không được cấp authority; user vắng mặt được kiểm tra fallback. |
| BUG-02 | Medium | Swagger/OpenAPI được bật theo cấu hình mặc định, không có profile production tắt rõ ràng. | Thêm biến cấu hình `SWAGGER_ENABLED` và `application-prod.properties` tắt API docs/UI. | Đã kiểm tra `/v3/api-docs` trả 200 ở môi trường dev; chưa chạy ứng dụng với profile prod. |
| BUG-03 | Medium | Security test trước đó cho phép bất kỳ status nào miễn không phải 401/403, và chạy toàn bộ application context. | Chuyển sang `@WebMvcTest`; kiểm tra chính xác 401, 403, 200 với SecurityFilterChain thực. | `SecurityFilterChainIntegrationTest`: 4/4 pass. |
| BUG-04 | Medium | Lint frontend có lỗi ở hook/effect, state đồng bộ modal, import thừa và exports ảnh hưởng Fast Refresh. | Refactor đồng bộ form theo props/query key, chuyển cập nhật sau request, tách Theme context và bỏ export helper không dùng bên ngoài component. | `npm run lint` pass ở cả hai frontend. |

## Còn theo dõi

| ID | Mức độ | Vấn đề | Trạng thái |
|---|---|---|---|
| BUG-05 | Medium | Một số `@SpringBootTest` hiện chạy với profile `dev`, kết nối PostgreSQL và khởi động Kafka. | Chưa cô lập toàn bộ integration suite; không chạy toàn suite trên DB dùng chung. Cần profile test/DB riêng trước khi chạy CI. |
| BUG-06 | Low | Admin bundle JS vượt ngưỡng cảnh báo 500 kB của Vite. | Build vẫn pass; cần tách code động nếu có thể kiểm chứng không đổi luồng UI. |
| BUG-07 | Medium | Postman collections có giá trị credential mẫu được nhúng tại collection variables. | Đã thêm environment local với token/password trống và kiểu secret để dùng thay thế. Giá trị mặc định trong các collection cũ vẫn còn; chỉ dùng khi đã chọn environment mới và tự nhập tài khoản test. |

## Ghi chú

Không ghi nhận bug Critical còn mở trong phạm vi đã kiểm tra. API Postman chưa được chạy với tài khoản thật; chỉ kiểm tra cấu trúc JSON và các request/test script hiện có.
