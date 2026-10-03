# Hướng dẫn Thiết lập Hạ tầng Staging AWS EC2 & CI/CD TeleCare (Tuần 10)

> **Dự án:** TeleCare Corporate Internship  
> **Tài liệu:** Staging Deployment & Infrastructure Setup Guide  
> **Mục tiêu:** Thiết lập CI/CD GitHub Actions và triển khai hệ thống an toàn trên AWS EC2 Staging  
> **Nguyên tắc bảo mật:** Không lưu secret vào git/images/docker layers; xác thực AWS bằng GitHub OIDC AssumeRole; EC2 pull image bằng IAM Instance Profile.

---

## 1. Kiến trúc Triển khai Staging

```
+-----------------------------------------------------------------------------------+
| GitHub Actions (CI/CD)                                                           |
|  - CI: Unit/Integration Tests, Lint, Next.js/Vite Build, Docker Build Dry-run    |
|  - CD: OIDC AssumeRole -> Build & Push Tagged Images to Amazon ECR                |
|       -> SSH Deploy with Strict Host Key Verification                             |
+-----------------------------------------+-----------------------------------------+
                                          |
                        (Image Push via OIDC)
                                          v
+-----------------------------------------------------------------------------------+
| Amazon Elastic Container Registry (ECR)                                           |
|  - telecare-backend:<git_sha>                                                     |
|  - telecare-admin-portal:<git_sha>                                                |
|  - telecare-web-app:<git_sha>                                                     |
+-----------------------------------------+-----------------------------------------+
                                          |
                       (Image Pull via Instance Profile)
                                          v
+-----------------------------------------------------------------------------------+
| AWS EC2 Staging Instance (Ubuntu 24.04 LTS / 22.04 LTS)                           |
|                                                                                   |
|  +------------------------ docker-compose.staging.yml -------------------------+  |
|  | [telecare-backend:55080] [telecare-admin-portal:55000] [telecare-web-app:53000] |  |
|  +-------------------------------------+----------------------------------------+  |
|                                        | (Internal Docker Network: telecare-network)
|  +-------------------------------------v----------------------------------------+  |
|  | [PostgreSQL:5432] [Redis:6379] [Kafka:19092] [Keycloak:8080] (No Public IP)  |  |
|  +------------------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## 2. Danh mục Biến & Secrets cần cấu hình trên GitHub

Vào repository GitHub: **Settings** -> **Secrets and variables** -> **Actions** -> Tạo Environment có tên **`staging`**.

### A. GitHub Environment Secrets (Chỉ nhập trong giao diện GitHub Settings, KHÔNG chia sẻ ra ngoài)

| Tên Secret | Mô tả chi tiết |
|---|---|
| `AWS_OIDC_ROLE_ARN` | ARN của IAM Role cho phép GitHub OIDC AssumeRole (ví dụ: `arn:aws:iam::123456789012:role/GitHubActionsStagingDeployRole`) |
| `STAGING_SSH_HOST` | Địa chỉ IP công cộng (Elastic IP) hoặc DNS của EC2 Staging |
| `STAGING_SSH_USER` | Tên người dùng SSH trên EC2 (mặc định: `ubuntu`) |
| `STAGING_SSH_PRIVATE_KEY` | Nội dung OpenSSH Private Key để deploy (tương ứng với Public Key trong `~/.ssh/authorized_keys` của EC2) |
| `STAGING_SSH_KNOWN_HOSTS` | Dòng host key xác thực của EC2 (lấy bằng `ssh-keyscan -H <STAGING_SSH_HOST>` và kiểm tra fingerprint) |

### B. GitHub Environment Variables

| Tên Variable | Giá trị mẫu | Mục đích |
|---|---|---|
| `AWS_REGION` | `ap-southeast-1` | AWS Region chứa ECR và EC2 |
| `ECR_BACKEND_REPO` | `telecare-backend` | Tên ECR Repository cho backend |
| `ECR_ADMIN_PORTAL_REPO` | `telecare-admin-portal` | Tên ECR Repository cho admin portal |
| `ECR_WEB_APP_REPO` | `telecare-web-app` | Tên ECR Repository cho customer web app |
| `STAGING_APP_DIR` | `/home/ubuntu/telecare` | Thư mục chứa cấu hình triển khai trên EC2 |
| `STAGING_KEYCLOAK_URL` | `https://auth.staging.yourdomain.com` (hoặc `http://<EC2_IP>:59000`) | URL Keycloak mà browser người dùng gọi tới |
| `STAGING_BACKEND_URL` | `https://api.staging.yourdomain.com` (hoặc `http://<EC2_IP>:55080`) | URL Backend API Gateway mà frontend gọi tới |
| `STAGING_KEYCLOAK_REALM` | `telecare-platform` | Realm Keycloak của dự án |
| `STAGING_KEYCLOAK_CLIENT_ID` | `telecare-id` | Client ID cho Customer Web App |
| `STAGING_KEYCLOAK_ADMIN_CLIENT_ID` | `telecare-admin-id` | Client ID cho Admin Portal |

---

## 3. Thiết lập AWS IAM & Quyền Hạn (Best Practices)

### Bước 3.1: Tạo OpenID Connect (OIDC) Identity Provider trên AWS IAM
1. Vào AWS IAM -> **Identity providers** -> **Add provider**.
2. Chọn **OpenID Connect**.
3. **Provider URL**: `https://token.actions.githubusercontent.com`
4. **Audience**: `sts.amazonaws.com`
5. Bấm **Get thumbprint** và lưu lại.

### Bước 3.2: Tạo IAM Role cho GitHub Actions (`GitHubActionsStagingDeployRole`)
Tạo Role với Trust Policy sau (thay thế `<ACCOUNT_ID>`, `<GITHUB_ORG_OR_USER>`, `<REPO_NAME>`):
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:<GITHUB_ORG_OR_USER>/<REPO_NAME>:*"
        }
      }
    }
  ]
}
```

Gán Permission Policy cho phép Push image lên ECR:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "*"
    }
  ]
}
```

### Bước 3.3: Gán IAM Instance Profile cho máy ảo EC2 Staging
Tạo IAM Role cho EC2 (`EC2StagingReadOnlyECRRole`) và đính kèm Policy `AmazonEC2ContainerRegistryReadOnly`.
Gán Instance Profile này vào máy ảo EC2.  
*Lợi ích:* EC2 có thể pull image từ ECR bằng lệnh `aws ecr get-login-password` mà **hoàn toàn không cần lưu AWS Access Key / Secret Key trên server**.

---

## 4. Chuẩn bị Máy Ảo EC2 Staging

1. **Cài đặt Docker & Docker Compose trên EC2:**
   ```bash
   sudo apt-get update
   sudo apt-get install -y ca-certificates curl gnupg awscli
   sudo install -m 0755 -d /etc/apt/keyrings
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
   sudo chmod a+r /etc/apt/keyrings/docker.gpg
   echo \
     "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
     "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
     sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
   sudo apt-get update
   sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
   sudo usermod -aG docker ubuntu
   ```

2. **Khởi tạo thư mục và hạ tầng:**
   ```bash
   mkdir -p /home/ubuntu/telecare
   cd /home/ubuntu/telecare
   ```
   Tạo file `.env` từ mẫu `telecare-infrastructure/.env.staging.example`.

3. **Security Group Inbound Rules khuyến nghị:**
   - Cổng `22`: SSH (giới hạn theo IP của quản trị viên hoặc GitHub Actions IP ranges).
   - Cổng `80` / `443`: HTTP/HTTPS (Nginx Reverse Proxy / Load Balancer).
   - Cổng `55080`: Backend API (nếu không đi qua Reverse Proxy).
   - Cổng `55000`: Admin Portal.
   - Cổng `53000`: Customer Web App.
   - Cổng `59000`: Keycloak (nếu cần quản trị trực tiếp).
   - **Tuyệt đối KHÔNG mở cổng `55432` (Postgres), `56379` (Redis), `59092` (Kafka) ra Internet (`0.0.0.0/0`).**

---

## 5. Quy trình Kiểm chứng Tự động & Rollback An Toàn

Trong workflow `.github/workflows/deploy-staging.yml`:
1. **Kiểm tra sức khỏe tự động:** Sau khi chạy `docker compose -f docker-compose.staging.yml up -d`, script chạy vòng lặp kiểm tra:
   - Backend health: `curl -f http://localhost:55080/api/v1/actuator/health`
   - Admin Portal: `wget --spider http://localhost:55000/health`
   - Customer Web App: `wget --spider http://localhost:53000`
2. **Cơ chế Rollback:** Nếu bất kỳ service nào không healthy sau 60 giây:
   - Pipeline ghi log lỗi chi tiết.
   - Tự động khôi phục biến `IMAGE_TAG` về version trước đó.
   - Chạy `docker compose up -d` với tag cũ.
   - **Bảo toàn 100% dữ liệu database và volumes.** Không bao giờ gọi `docker compose down -v`.

---

## 6. Khuyến nghị Bảo Mật Quan Trọng

> [!WARNING]
> Vì một số thông tin cấu hình và credentials thử nghiệm (AWS Key, Gmail App Password) đã từng xuất hiện trong lịch sử lệnh hoặc trao đổi trước đó, khuyến nghị:
> 1. **Thu hồi ngay lập tức (Revoke/Rotate)** AWS Access Key và Gmail App Password cũ trên AWS Console và Google Account Security.
> 2. Sử dụng IAM OIDC AssumeRole như đã thiết lập trong workflow để không bao giờ cần lưu trữ AWS Long-term Access Key nữa.
