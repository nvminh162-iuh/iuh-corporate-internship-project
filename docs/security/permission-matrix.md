# TeleCare RBAC Permission Matrix & Security Specification

Tài liệu đặc tả ma trận phân quyền (Role-Based Access Control - RBAC), quyền hạn (Authorities & Permissions) và quy tắc bảo mật của hệ thống TeleCare.

---

## 1. Danh sách Quyền hạn (Permissions) Trong Hệ thống

Hệ thống TeleCare phân định các quyền chi tiết (`Permission`) được gán cho `Role` (nhóm quyền) và lưu trong cơ sở dữ liệu (`permissions`, `roles`, `roles_permissions`):

| Nhóm chức năng | Mã Permission | Tên hiển thị / Mô tả |
|---|---|---|
| **User Management** | `USER_VIEW` | Xem danh sách và chi tiết người dùng |
| | `USER_CREATE` | Tạo mới người dùng, gửi lại thư mời |
| | `USER_UPDATE` | Cập nhật thông tin, trạng thái enable/disable, phân quyền người dùng |
| **Role Management** | `ROLE_VIEW` | Xem danh sách và chi tiết nhóm quyền (Role) |
| | `ROLE_CREATE` | Tạo nhóm quyền mới |
| | `ROLE_UPDATE` | Cập nhật thông tin và quyền hạn của nhóm quyền |
| | `ROLE_DELETE` | Xóa nhóm quyền |
| **Permission Management** | `PERMISSION_VIEW` | Xem danh sách và chi tiết quyền hệ thống |
| | `PERMISSION_CREATE` | Tạo mới quyền hệ thống |
| | `PERMISSION_UPDATE` | Cập nhật thông tin quyền hệ thống |
| | `PERMISSION_DELETE` | Xóa quyền hệ thống |
| **Service Plan Management** | `PLAN_VIEW` | Xem danh sách và chi tiết gói cước |
| | `PLAN_CREATE` | Tạo mới gói cước dịch vụ |
| | `PLAN_UPDATE` | Cập nhật thông tin và trạng thái gói cước |
| | `PLAN_DELETE` | Xóa mềm gói cước |
| **Category Management** | `CATEGORY_VIEW` | Xem danh sách và chi tiết nhóm dịch vụ |
| | `CATEGORY_CREATE` | Tạo mới nhóm dịch vụ |
| | `CATEGORY_UPDATE` | Cập nhật thông tin và kích hoạt nhóm dịch vụ |
| | `CATEGORY_DELETE` | Vô hiệu hóa (disable) nhóm dịch vụ |
| **Support Request Management** | `SUPPORT_REQUEST_VIEW` | Xem danh sách, chi tiết và lịch sử ticket hỗ trợ |
| | `SUPPORT_REQUEST_PROCESS` | Tiếp nhận, cập nhật trạng thái và thêm ghi chú xử lý ticket |
| | `SUPPORT_REQUEST_ASSIGN` | Xem danh sách nhân viên hợp lệ và phân công xử lý ticket |
| **System Internal** | `INTERNAL_SERVICE` | Quyền gọi các endpoint nội bộ giữa các microservices |

---

## 2. Ma trận Phân quyền Endpoint Chi tiết (API Permission Matrix)

Mọi endpoint quản trị (`/admin/**`) được cấu hình bằng annotation `@PreAuthorize("hasAnyAuthority(...)")` ở cấp phương thức (method-level) để đảm bảo các Role tùy chỉnh sở hữu Permission tương ứng có thể thực thi mà không bắt buộc phải có vai trò `ADMIN`. Vai trò `ADMIN` luôn có toàn quyền trên các endpoint quản trị.

### 2.1. Quản lý Người dùng (`/admin/users`)

| Method | Endpoint | Authority yêu cầu | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/admin/users` | `ADMIN`, `USER_VIEW` | Danh sách người dùng (phân trang, lọc) |
| `GET` | `/api/v1/admin/users/{userId}` | `ADMIN`, `USER_VIEW` | Xem chi tiết người dùng theo ID |
| `POST` | `/api/v1/admin/users` | `ADMIN`, `USER_CREATE` | Tạo mới người dùng quản trị/nhân viên |
| `PUT` | `/api/v1/admin/users/{userId}` | `ADMIN`, `USER_UPDATE` | Cập nhật thông tin người dùng |
| `PATCH` | `/api/v1/admin/users/{userId}/enable` | `ADMIN`, `USER_UPDATE` | Kích hoạt tài khoản người dùng |
| `PATCH` | `/api/v1/admin/users/{userId}/disable` | `ADMIN`, `USER_UPDATE` | Vô hiệu hóa tài khoản người dùng |
| `POST` | `/api/v1/admin/users/assign-role` | `ADMIN`, `USER_UPDATE` | Gán nhóm quyền cho người dùng |
| `POST` | `/api/v1/admin/users/{userId}/resend-invitation` | `ADMIN`, `USER_CREATE` | Gửi lại email kích hoạt / đặt mật khẩu |

### 2.2. Quản lý Nhóm quyền (`/admin/roles`)

| Method | Endpoint | Authority yêu cầu | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/admin/roles` | `ADMIN`, `ROLE_VIEW` | Danh sách nhóm quyền phân trang |
| `GET` | `/api/v1/admin/roles/all` | `ADMIN`, `ROLE_VIEW` | Danh sách tất cả nhóm quyền cho dropdown |
| `GET` | `/api/v1/admin/roles/{id}` | `ADMIN`, `ROLE_VIEW` | Xem chi tiết nhóm quyền và các permissions |
| `POST` | `/api/v1/admin/roles` | `ADMIN`, `ROLE_CREATE` | Tạo mới nhóm quyền |
| `POST` | `/api/v1/admin/roles/{id}` | `ADMIN`, `ROLE_UPDATE` | Cập nhật nhóm quyền |
| `DELETE` | `/api/v1/admin/roles/{id}` | `ADMIN`, `ROLE_DELETE` | Xóa nhóm quyền |

### 2.3. Quản lý Quyền hệ thống (`/admin/permissions`)

| Method | Endpoint | Authority yêu cầu | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/admin/permissions` | `ADMIN`, `PERMISSION_VIEW` | Danh sách quyền phân trang |
| `GET` | `/api/v1/admin/permissions/all` | `ADMIN`, `PERMISSION_VIEW` | Danh sách tất cả quyền cho dropdown |
| `GET` | `/api/v1/admin/permissions/{id}` | `ADMIN`, `PERMISSION_VIEW` | Xem chi tiết quyền |
| `POST` | `/api/v1/admin/permissions` | `ADMIN`, `PERMISSION_CREATE` | Tạo mới quyền hệ thống |
| `POST` | `/api/v1/admin/permissions/{id}` | `ADMIN`, `PERMISSION_UPDATE` | Cập nhật thông tin quyền |
| `DELETE` | `/api/v1/admin/permissions/{id}` | `ADMIN`, `PERMISSION_DELETE` | Xóa quyền hệ thống |

### 2.4. Quản lý Gói cước Dịch vụ (`/admin/plans`)

| Method | Endpoint | Authority yêu cầu | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/admin/plans` | `ADMIN`, `PLAN_VIEW` | Danh sách gói cước (tìm kiếm, lọc, phân trang) |
| `GET` | `/api/v1/admin/plans/{id}` | `ADMIN`, `PLAN_VIEW` | Xem chi tiết gói cước |
| `POST` | `/api/v1/admin/plans` | `ADMIN`, `PLAN_CREATE` | Tạo mới gói cước |
| `PUT` | `/api/v1/admin/plans/{id}` | `ADMIN`, `PLAN_UPDATE` | Cập nhật thông tin gói cước |
| `PATCH` | `/api/v1/admin/plans/{id}/status` | `ADMIN`, `PLAN_UPDATE` | Cập nhật trạng thái gói cước (DRAFT, PUBLISHED, ARCHIVED) |
| `DELETE` | `/api/v1/admin/plans/{id}` | `ADMIN`, `PLAN_DELETE` | Xóa mềm gói cước |

### 2.5. Quản lý Nhóm dịch vụ (`/admin/service-categories`)

| Method | Endpoint | Authority yêu cầu | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/admin/service-categories` | `ADMIN`, `CATEGORY_VIEW` | Danh sách nhóm dịch vụ phân trang |
| `GET` | `/api/v1/admin/service-categories/all` | `ADMIN`, `CATEGORY_VIEW` | Danh sách tất cả nhóm dịch vụ active |
| `POST` | `/api/v1/admin/service-categories` | `ADMIN`, `CATEGORY_CREATE` | Tạo mới nhóm dịch vụ |
| `PUT` | `/api/v1/admin/service-categories/{id}` | `ADMIN`, `CATEGORY_UPDATE` | Cập nhật thông tin nhóm dịch vụ |
| `PATCH` | `/api/v1/admin/service-categories/{id}/enable` | `ADMIN`, `CATEGORY_UPDATE` | Kích hoạt nhóm dịch vụ |
| `PATCH` | `/api/v1/admin/service-categories/{id}/disable` | `ADMIN`, `CATEGORY_DELETE` | Vô hiệu hóa nhóm dịch vụ |

### 2.6. Quản lý Yêu cầu Hỗ trợ Admin (`/admin/support-requests`)

| Method | Endpoint | Authority yêu cầu | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/admin/support-requests` | `ADMIN`, `SUPPORT_REQUEST_VIEW` | Danh sách yêu cầu hỗ trợ (lọc keyword, status, category, date) |
| `GET` | `/api/v1/admin/support-requests/{id}` | `ADMIN`, `SUPPORT_REQUEST_VIEW` | Chi tiết yêu cầu hỗ trợ |
| `GET` | `/api/v1/admin/support-requests/{id}/histories` | `ADMIN`, `SUPPORT_REQUEST_VIEW` | Lịch sử xử lý chi tiết (timeline admin) |
| `POST` | `/api/v1/admin/support-requests/{id}/receive` | `ADMIN`, `SUPPORT_REQUEST_PROCESS` | Tiếp nhận yêu cầu hỗ trợ (`NEW` -> `RECEIVED`) |
| `PUT` | `/api/v1/admin/support-requests/{id}/status` | `ADMIN`, `SUPPORT_REQUEST_PROCESS` | Chuyển đổi trạng thái (`IN_PROGRESS`, `WAITING_CUSTOMER`, `COMPLETED`, `CLOSED`) |
| `POST` | `/api/v1/admin/support-requests/{id}/histories` | `ADMIN`, `SUPPORT_REQUEST_PROCESS` | Thêm ghi chú xử lý vào timeline |
| `POST` | `/api/v1/admin/support-requests/{id}/assign` | `ADMIN`, `SUPPORT_REQUEST_ASSIGN` | Phân công hoặc phân công lại nhân viên |
| `GET` | `/api/v1/admin/support-requests/assignees` | `ADMIN`, `SUPPORT_REQUEST_ASSIGN` | Danh sách nhân viên đủ điều kiện phân công |

### 2.7. Yêu cầu Hỗ trợ Khách hàng (`/support-requests`)

| Method | Endpoint | Authority yêu cầu | Quy tắc Ownership |
|---|---|---|---|
| `POST` | `/api/v1/support-requests` | Authenticated | Khách hàng tạo ticket hỗ trợ cho chính mình |
| `GET` | `/api/v1/support-requests/me` | Authenticated | Danh sách ticket của chính user đăng nhập |
| `GET` | `/api/v1/support-requests/me/{id}` | Authenticated | Chi tiết ticket (chỉ truy cập ticket của chính mình, sai trả 404) |
| `GET` | `/api/v1/support-requests/me/{id}/histories` | Authenticated | Timeline an toàn cho khách hàng (ẩn ghi chú nội bộ và staff ID) |

### 2.8. Thông báo In-App (`/notifications`)

| Method | Endpoint | Authority yêu cầu | Quy tắc Ownership |
|---|---|---|---|
| `GET` | `/api/v1/notifications` | Authenticated | Danh sách thông báo của chính user đăng nhập |
| `GET` | `/api/v1/notifications/unread-count` | Authenticated | Đếm số thông báo chưa đọc của chính user |
| `PATCH` | `/api/v1/notifications/{id}/read` | Authenticated | Đánh dấu đã đọc 1 thông báo (sai ownership trả 404) |
| `PATCH` | `/api/v1/notifications/read-all` | Authenticated | Đánh dấu đã đọc tất cả thông báo của chính user |

### 2.9. Quyền Người dùng Hiện tại & Internal Endpoints

| Method | Endpoint | Authority yêu cầu | Mục đích |
|---|---|---|---|
| `GET` | `/api/v1/users/me/permissions` | Authenticated | Trả về role và danh sách permissions của chính user đăng nhập (dùng cho UI dynamic authorization) |
| `GET` | `/api/v1/internal/users/{userId}/permissions` | `INTERNAL_SERVICE` | Endpoint dịch vụ nội bộ (bảo vệ bằng authority chuyên biệt) |

---

## 3. Quy tắc Xác thực Token JWT & Quyền Hạn (JWT Authority Resolution)

`CustomJwtAuthenticationConverter` áp dụng nguyên tắc **Fail-Closed** và kiểm tra nghiêm ngặt tính hợp lệ của tài khoản trong PostgreSQL:

1. **Kiểm tra trạng thái User**:
   - Nếu `user.active == false`, không cấp bất kỳ `GrantedAuthority` quản trị nào (`ADMIN`, permissions).
2. **Kiểm tra trạng thái Role**:
   - Chỉ cấp role name và `ROLE_{name}` nếu `role.active == true`.
3. **Kiểm tra trạng thái Permission**:
   - Chỉ cấp permission nếu `permission.active == true`.
4. **Ngăn chặn Keycloak Fallback Bypass**:
   - Nếu user đã tồn tại trong local database nhưng bị vô hiệu hóa hoặc không có quyền `ADMIN`, hệ thống **không** lấy role `ADMIN` từ token Keycloak để ghi đè lên quyền đã thu hồi trong hệ thống.
