# Image Keycloak TeleCare

## Luồng tự động

Push lên `master` khi thay đổi source/plugin/theme/realm trong `telecare-infrastructure/keycloak`, Dockerfile, Docker ignore, script kiểm tra hoặc workflow Keycloak sẽ chạy workflow **TeleCare - Build & Push Keycloak Image**.
Có thể chạy thủ công bằng Actions > Run workflow > branch `master`; các branch khác không được publish.

Workflow thực hiện:

1. Kiểm tra repository secrets `DOCKER_USERNAME=nvminh162` và `DOCKER_PASSWORD` (Docker Hub PAT có quyền Read & Write).
2. Chạy kiểm thử guard và kiểm tra các trường credential trong realm; không in giá trị bí mật.
3. Dùng Java 21 và Maven build cả Kafka event publisher và phone login từ source.
4. Build image Keycloak 26.5.2 cho EC2 x86_64 (`linux/amd64`), PostgreSQL, health và metrics.
5. Push `nvminh162/telecare-keycloak:latest` và `nvminh162/telecare-keycloak:<commit-sha>`.

Docker Hub có thể tự tạo repository ở lần push đầu; visibility theo default repository privacy của tài khoản. Đặt default Public nếu muốn Dokploy pull không cần credentials. Nếu Private, cấu hình registry credentials trong Dokploy.
Không cần thêm database/Kafka/SMTP/Google secrets vào GitHub Actions. Job không khởi động database hoặc ứng dụng.
Workflow chỉ có hiệu lực sau commit/push; build local không chứng minh đã publish thành công trên GitHub.
Workflow backend hiện vẫn chạy trên mọi push `master`, vì vậy cùng một push có thể chạy hai workflow độc lập.

## Thành phần trong image

- `/opt/keycloak/providers/keycloak-kafka-listener.jar`: freshly built từ `keycloak-event-publisher/target`.
- `/opt/keycloak/providers/keycloak-phone-login.jar`: freshly built từ `keycloak-phone-login/target`.
- Theme `telecare`, `telecare-admin`.
- `/opt/keycloak/data/import/telecare-platform-realm.json`: bản realm export hiện có.
- Entrypoint `kc.sh`; lệnh mặc định `start --optimized --import-realm`, không phải `start-dev`.

`.dockerignore` dùng allowlist, không gửi `.env`, source Java, các JAR cũ trong `keycloak/provider` hoặc dữ liệu PostgreSQL vào build context.
Maven chạy `-Dmaven.antrun.skip=true` để không ghi đè hai JAR đã track trong Git; Dockerfile lấy kết quả mới từ `target`.
Giữ version Keycloak trong hai POM và Dockerfile/workflow đồng bộ khi nâng cấp.

## Kiểm tra và build local (PowerShell, tại workspace root)

```powershell
$env:PYTHONDONTWRITEBYTECODE = '1'
python -m unittest discover -s telecare-infrastructure/scripts -p 'test_*.py'
python telecare-infrastructure/scripts/validate_keycloak_realm.py
mvn --batch-mode --no-transfer-progress -f telecare-infrastructure/keycloak/keycloak-event-publisher/pom.xml clean verify '-Dmaven.antrun.skip=true'
mvn --batch-mode --no-transfer-progress -f telecare-infrastructure/keycloak/keycloak-phone-login/pom.xml clean verify '-Dmaven.antrun.skip=true'
docker build --platform linux/amd64 -f telecare-infrastructure/Dockerfile.keycloak-custom -t nvminh162/telecare-keycloak:local telecare-infrastructure
docker run --rm nvminh162/telecare-keycloak:local start --optimized --help
```

Các lệnh trên chưa push image và chưa khởi động server Keycloak. Lệnh `--help` chỉ kiểm tra CLI; không xác nhận kết nối database/Kafka hay import realm thành công.

## Các việc phải cấu hình khi triển khai Dokploy (bước sau)

- PostgreSQL riêng cho dự án, database `keycloak` và `telecare`; không dùng database nội bộ của Dokploy.
- Runtime Keycloak: `KC_DB_URL`, `KC_DB_USERNAME`, `KC_DB_PASSWORD`, `KC_BOOTSTRAP_ADMIN_USERNAME`, `KC_BOOTSTRAP_ADMIN_PASSWORD`, hostname HTTPS, reverse proxy headers, `KAFKA_BOOTSTRAP_SERVER`, `KEYCLOAK_KAFKA_TOPIC` và giới hạn tài nguyên.
- Không ghi đè command thành `start-dev`; không mount thư mục providers cũ đè lên plugin đã build trong image.
- Kết nối mạng nội bộ với backend; không công khai PostgreSQL, Kafka hoặc port management 9000. `EXPOSE` trong Dockerfile không tự mở port EC2.
- Realm hiện chỉ có redirect/web origins localhost: thêm đúng domain frontend và admin Vercel, đồng thời cấu hình CORS backend.
- Mật khẩu SMTP và Google client secret trong export hiện là dấu `*`, không phải credentials thật. Cấp giá trị thật riêng khi triển khai (Admin Console hoặc realm template dùng environment placeholders), không commit chúng vào realm/image.
- `--import-realm` chỉ tạo realm khi chưa tồn tại. Push image/realm mới không tự cập nhật realm đang nằm trong database; cập nhật qua Admin Console/Admin API với kế hoạch riêng, không xóa realm hay volume để ép import lại.
- Build/push tự động không đồng nghĩa deploy tự động. Webhook/API và bước kiểm tra runtime Dokploy sẽ được thiết lập riêng.

## Tham khảo

- Mẫu workflow: https://github.com/Tea4Life-Group/Tea4Life-Local-Dev-Infrastucture/blob/main/.github/workflows/build-keycloak-image.yml
- Keycloak containers: https://www.keycloak.org/server/containers
- Realm import và environment placeholders: https://www.keycloak.org/server/importExport
- Docker Hub default repository privacy: https://docs.docker.com/docker-hub/settings/
