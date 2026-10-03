# Kết quả kiểm thử Tuần 09

Ngày: 2026-10-03
Workspace: `D:\Workspace\iuh-corporate-internship-project`
Branch: `master`

## Kết quả đã xác minh

| Hạng mục | Lệnh / phép kiểm tra | Kết quả |
|---|---|---|
| Backend focused tests | `mvnw.cmd -Dtest=CustomJwtAuthenticationConverterTest,NotificationServiceAdditionalTest,SupportRequestServiceAdditionalTest,SecurityFilterChainIntegrationTest test` | 21 pass, 0 fail, 0 skipped |
| JaCoCo focused report | `mvnw.cmd jacoco:report` sau focused tests | Line 49.1% (950/1933), branch 33.3% (269/808); chỉ tính phần code đã được lượt focused suite thực thi, không đại diện coverage tổng thể |
| Admin lint | `npm run lint` trong `telecare-admin-portal` | PASS |
| Admin build | `npm run build` trong `telecare-admin-portal` | PASS; Vite cảnh báo bundle JS khoảng 657 kB |
| Customer Web lint | `npm run lint` trong `telecare-web-app` | PASS |
| Customer Web build | `npm run build` trong `telecare-web-app` | PASS; Next.js tạo thành công các route hiện có |
| Health endpoint | GET `http://localhost:55080/api/v1/actuator/health` | HTTP 200 |
| OpenAPI | GET `http://localhost:55080/api/v1/v3/api-docs` | HTTP 200, JSON khoảng 59.6 kB |
| Postman JSON | Parse 3 collection hiện có | Cả 3 hợp lệ; TeleCare Backend API có 80 request, 63 request có test script |
| Postman environment | Parse `TeleCare Local.postman_environment.json` | JSON hợp lệ; secret/token variables để trống, chỉ có URL/realm/client ID mặc định local |
| Diff whitespace | `git diff --check` | PASS; chỉ có cảnh báo line-ending LF/CRLF trên Windows |

## Giới hạn của lượt kiểm thử

- Không chạy toàn bộ Maven suite: các lớp `@SpringBootTest` hiện dùng profile `dev`, kết nối PostgreSQL local và khởi động Kafka. Chạy suite đầy đủ có thể tác động DB dùng chung; cần tạo profile/database test riêng trước.
- Chưa chạy Newman hoặc gửi API thay đổi dữ liệu bằng Postman. Chọn environment local mới và nhập token/tài khoản test của bạn trước khi chạy; không chia sẻ collection cũ một mình vì còn giá trị credential mẫu ở collection variables.
- Chưa kiểm tra giao diện trực tiếp bằng trình duyệt.
- Swagger production đã có cấu hình tắt, nhưng chưa khởi động ứng dụng với profile `prod` để xác nhận runtime.
- JaCoCo report đã tạo; coverage trên chỉ phản ánh focused suite, chưa đạt và không đại diện coverage tổng thể vì chưa chạy toàn bộ suite an toàn.
- Lint và build là kiểm chứng tĩnh/build, không thay thế kiểm thử UI hoặc end-to-end.

## Kết luận

Các sửa đổi security và lint đã được xác minh bằng test tập trung, cả hai frontend lint/build đều pass. Chưa thể kết luận hoàn tất toàn bộ Tuần 09 do thiếu kiểm thử Postman end-to-end, UI browser test, production profile runtime check và cô lập integration suite.
