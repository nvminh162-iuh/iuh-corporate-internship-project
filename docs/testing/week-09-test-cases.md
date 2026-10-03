# Test cases Tuần 09

## Automated tests đã chạy

| ID | Module | Loại | Điều kiện / thao tác | Kết quả mong đợi | Kết quả |
|---|---|---|---|---|---|
| SEC-HTTP-01 | Security | MVC integration | GET `/notifications` không có authentication | 401 | PASS |
| SEC-HTTP-02 | RBAC | MVC integration | Customer authority gọi GET `/admin/support-requests` | 403 | PASS |
| SEC-HTTP-03 | RBAC | MVC integration | Có `SUPPORT_REQUEST_VIEW` gọi GET `/admin/support-requests` | 200 | PASS |
| SEC-HTTP-04 | Public API | MVC integration | GET `/service-categories` không có authentication | 200 | PASS |
| JWT-01 | Authentication | Unit | User không tồn tại sau lookup thành công; JWT có role claim | Áp dụng fallback role theo thiết kế hiện tại | PASS |
| JWT-02 | Authentication | Unit | Lookup local user ném exception; JWT có role admin | Không cấp authority | PASS |
| JWT-03 | Authentication | Unit | Local user inactive; JWT có role admin | Không cấp authority | PASS |
| NOTIF-05/08/09 | Notification | Unit | Mark-read đã đọc, mark-all-read theo user hiện tại, thiếu recipient | Idempotent/đúng owner/bỏ qua recipient rỗng | PASS |
| SR-C-010..016 | Support Request | Unit | Danh sách/chi tiết/history ownership, date range | Chỉ dữ liệu của customer; date range sai bị từ chối | PASS |

Tổng: 21 test, 21 pass, 0 fail, 0 skipped trong lượt chạy tập trung.

## API và kiểm tra thủ công còn cần chạy

| ID | Module | Thao tác | Kết quả mong đợi | Trạng thái |
|---|---|---|---|---|
| API-01 | Swagger | GET `/api/v1/v3/api-docs` ở dev | 200, JSON OpenAPI hợp lệ | PASS bằng HTTP local |
| API-02 | Health | GET `/api/v1/actuator/health` | 200 | PASS bằng HTTP local |
| API-03 | Support Request | Customer tạo ticket, admin nhận, phân công, cập nhật trạng thái, customer đọc history/notification | History và trạng thái đầy đủ | Chưa chạy Postman end-to-end |
| API-04 | Negative API | Body thiếu field, enum sai, ticket không tồn tại, sai owner | 400/404 theo contract | Chưa chạy Postman |
| UI-01 | Customer Web | Tạo ticket, xem danh sách/chi tiết, history, notification | Hiển thị state mới nhất | Chưa test trình duyệt |
| UI-02 | Admin Portal | Lọc ticket, nhận, phân công, đổi trạng thái, thêm ghi chú | Backend phản ánh thay đổi | Chưa test trình duyệt |
| SEC-05 | Production config | Chạy backend với `prod` profile | Swagger UI/API docs tắt | Chưa chạy ứng dụng prod profile |
