# Build image backend và cấu hình Dokploy

## GitHub Actions: chỉ cần thông tin xác thực Docker Hub

Workflow `.github/workflows/deploy.yml` chạy khi push lên `master`, bất kể file thay đổi, và hỗ trợ `workflow_dispatch`.
Runner dùng JDK 21 Corretto, Maven Wrapper trong `telecare-backend` và Jib 3.5.1.
Lệnh `clean compile jib:build -DskipTests` biên dịch và publish image; không khởi động Spring Boot hay chạy test.
Vì vậy runner không cần database, Kafka, Keycloak, thông tin xác thực AWS hoặc file `.env`.

Trong GitHub repository, vào Settings > Secrets and variables > Actions > Secrets > New repository secret:

| Secret | Giá trị nhập trực tiếp trên GitHub |
| --- | --- |
| `DOCKER_USERNAME` | `nvminh162` |
| `DOCKER_PASSWORD` | Docker Hub personal access token có quyền đọc và push/write image |

Đây là repository secrets; workflow chưa gắn GitHub Environment nên không dùng environment secrets.
Không commit token vào YAML, `.env`, `pom.xml` hoặc gửi giá trị token trong chat.
GitHub repository public không loại bỏ yêu cầu xác thực khi push Docker Hub.
Public/private của Docker Hub repository là thiết lập riêng với GitHub repository.

Tạo Docker Hub repository `nvminh162/telecare-backend` nếu chưa có.
Workflow publish hai tag cho cùng một lần build:

- `nvminh162/telecare-backend:latest` để Dokploy theo dõi.
- `nvminh162/telecare-backend:<commit-sha>` để truy vết và chọn lại phiên bản.

Concurrency ngăn các lần build cùng branch push image song song.
Workflow chỉ có hiệu lực sau khi file được commit và push lên GitHub. Kiểm tra local không thay thế kết quả chạy GitHub Actions.

## Dokploy: biến môi trường khi chạy ứng dụng

Dùng cấu trúc và các nhóm biến của `telecare-backend/.env` hiện có, rồi nhập trực tiếp vào Dokploy > Environment.
Giữ nguyên thông tin tài khoản, realm, client và S3 theo cấu hình của bạn; thay endpoint local bằng endpoint dịch vụ thật trên server.
Backend chỉ còn `src/main/resources/application.properties`, không còn file cấu hình riêng cho dev/prod.
File này tự nạp `.env` khi chạy từ thư mục `telecare-backend`, hoặc `telecare-backend/.env` khi chạy từ workspace root, bằng Spring Config Data với UTF-8.
Định dạng `.env` là `KEY=value`, không dùng `export KEY=...`; không bao giá trị trong dấu nháy vì file được đọc theo định dạng Java properties.
Biến môi trường của tiến trình được ưu tiên hơn giá trị trong `.env`.
Trong Dokploy, nhập toàn bộ biến ở tab Environment; container không cần chứa file `.env`. Không commit file thật hoặc đóng gói nó vào image.
`optional:` ở cấu hình import chỉ cho phép không có file khi container đã được cấp biến; không cung cấp giá trị mặc định cho các placeholder.
Không đưa thông tin xác thực khi chạy ứng dụng vào job build hoặc Docker image.

| Nhóm | Biến / giá trị cần xác định |
| --- | --- |
| Profile và port | `SPRING_PROFILES_ACTIVE=prod`, `SERVER_PORT=55080` |
| PostgreSQL | `DATASOURCE_URL` là JDBC URL đầy đủ; `DATASOURCE_USERNAME`, `DATASOURCE_PASSWORD` |
| Kafka | `KAFKA_BOOTSTRAP_SERVERS` |
| Keycloak | `KEYCLOAK_SERVER_URL`, `KEYCLOAK_ADMIN_USERNAME`, `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_ADMIN_REALM`, `KEYCLOAK_ADMIN_CLIENT_ID`, `KEYCLOAK_CURRENT_REALM`, `KEYCLOAK_CLIENT_ID` |
| S3 | `AWS_REGION`, `AWS_S3_BUCKET`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` theo cơ chế credentials mà code hiện tại sử dụng |
| Logging | `LOGGING_LEVEL_ROOT`, `LOGGING_LEVEL_APP` |

Bảng trên chỉ tóm tắt các nhóm chính. Danh sách đầy đủ nằm trong `.env.example`; cần cấp tất cả biến được `application.properties` tham chiếu, gồm pool kết nối, HTTP compression, health/metrics và Swagger.

`localhost` trong container backend trỏ về chính container đó, không trỏ về máy developer hay container database/Keycloak/Kafka.
Dùng hostname nội bộ và port của dịch vụ thực tế; không suy ra hostname từ tên hiển thị trong UI.
Keycloak public issuer phải khớp URL dùng để frontend đăng nhập; backend cũng phải truy cập được URL/JWKS đó.

Với cấu hình hợp nhất hiện tại:

- Các placeholder trong `application.properties` không có giá trị fallback; biến được sử dụng phải được cung cấp rõ ràng.
- `SPRING_PROFILES_ACTIVE` vẫn được giữ để không đổi giá trị đang có, nhưng `dev`/`prod` không còn chọn file cấu hình riêng hay tự ghi đè các thiết lập. Hành vi ứng dụng do các biến quyết định.
- `spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO}`: phải đặt chính sách schema; `update` sẽ cho Hibernate cập nhật schema khi ứng dụng chạy.
- `homespace.bootstrap.admin.enabled=${BOOTSTRAP_ADMIN_ENABLED}`: phải đặt `true` hoặc `false`; `true` bật tác vụ bootstrap dùng các biến tài khoản trong `.env`.
- Phải cung cấp các biến cấu hình rõ ràng ở cả local và server, không chỉ các biến mật khẩu.
- `SWAGGER_ENABLED=true` bật API docs và Swagger UI, `false` tắt cả hai, không phụ thuộc tên profile. Khi mở docs trên Internet cần xem xét quyền truy cập phù hợp.
- Nếu dùng validate, database rỗng chưa có schema sẽ không khởi động được. Chọn chính sách schema trước khi deploy; việc đổi cấu hình local chưa chạy DDL trên database.
- Có script `db/migration/V1.1__performance_indexes.sql`, nhưng chưa có Flyway/Liquibase trong POM; đây chưa phải migration tự động hoàn chỉnh hay baseline schema.
- `REDIS_*`, `SMTP_*`, `OTP_HASH_SECRET` không được backend hiện tại tham chiếu; không cần cho job publish image.

Chạy local từ thư mục backend, không cần tự gán từng biến PowerShell:

```powershell
cd D:\Workspace\iuh-corporate-internship-project\telecare-backend
.\mvnw.cmd spring-boot:run
```

Lệnh trên khởi động ứng dụng thật nên PostgreSQL, Kafka và Keycloak phải sẵn sàng theo endpoint trong `.env`.
Nếu đã gán biến trong terminal hoặc cấu hình VS Code, các giá trị đó sẽ ưu tiên hơn `.env`; hãy bỏ các giá trị cũ nếu muốn dùng file làm nguồn cấu hình.
Sau khi đổi hoặc xóa các file cấu hình, dùng `clean` ở lần build tiếp theo để loại bỏ resource cũ trong `target`.

## Phần cần hoàn thiện trước khi backend phục vụ Vercel

- Provision PostgreSQL/schema, Kafka và Keycloak hoặc cung cấp endpoint của dịch vụ đã có.
- `SecurityConfig.java` hiện chỉ cho phép CORS localhost. Cần cấu hình origins Vercel cụ thể trước khi test browser; chưa có biến `CORS_ALLOWED_ORIGINS` được code đọc.
- Cấu hình domain/HTTPS và port nội bộ `55080`; health endpoint là `/api/v1/actuator/health`.
- Nếu Docker Hub image private, cấu hình registry auth trong Dokploy. Image public có thể pull không cần credentials; push vẫn cần token.
- Workflow này chỉ build/push. Auto deploy Dokploy cần Docker Hub webhook/API được cấu hình riêng.

## Kiểm tra lần build đầu

1. Tạo hai repository secrets trên GitHub và repository image trên Docker Hub.
2. Review, commit và push workflow lên `master`.
3. Mở tab Actions, chọn workflow `TeleCare - Build & Push Backend Image` và xem log.
4. Xác nhận job thành công và hai tag xuất hiện trên Docker Hub.
5. Khi các dịch vụ và biến môi trường sẵn sàng, deploy image trong Dokploy và kiểm tra health/log.

Nếu job báo thiếu secrets, kiểm tra đúng tên và đúng mục repository secrets.
Nếu Docker Hub từ chối push, kiểm tra username, hiệu lực/quyền token và repository namespace.
