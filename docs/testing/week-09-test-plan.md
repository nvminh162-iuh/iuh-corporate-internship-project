# Ke hoach Kiem thu Tuan 09 - TeleCare Backend

> **Du an:** TeleCare Corporate Internship
> **Tuan:** 09
> **Ngay lap:** 2026-10-03
> **Nguoi lap:** nvminh162
> **Nhanh:** master (commit: ee7cc71)

---

## 1. Pham vi kiem thu

| Module | Loai test | Uu tien |
|---|---|---|
| Authentication (JWT/OAuth2) | Integration, Security | P0 |
| Role / Permission (RBAC) | Integration, Security | P0 |
| Support Request - Customer | Unit, Integration, API | P0 |
| Support Request - Admin/Staff | Unit, Integration, API | P0 |
| Notification | Unit, Integration, API | P0 |
| Exception / Validation / Logging | Unit, Integration | P1 |
| Service Plan - Public | Unit, API | P1 |
| Service Category - Admin | Unit, Integration | P1 |
| User / Profile | Unit, API | P2 |
| OpenAPI / Swagger | Manual | P2 |
| Frontend lint / build | CI | P1 |

---

## 2. Moi truong kiem thu

| Thanh phan | Moi truong Unit | Moi truong Integration |
|---|---|---|
| Database | Khong can (Mockito) | PostgreSQL (docker-compose, port 55432) |
| Keycloak | Khong can (Mock JWT via spring-security-test) | Khong can (Mock JWT) |
| Kafka | Khong can (Mock) | Khong can (Mock) |
| AWS S3 | Khong can (Mock) | Khong can (Mock) |
| Spring Context | Khong load | Load day du voi @MockitoBean |

### Cau hinh test

- Unit test: `@ExtendWith(MockitoExtension.class)` - khong phu thuoc PostgreSQL/Keycloak/Kafka
- Integration test: `@SpringBootTest + @AutoConfigureMockMvc` voi `@MockitoBean` cho services phu thuoc ha tang
- JWT mock: `SecurityMockMvcRequestPostProcessors.jwt()` tu `spring-security-test`
- Khong goi Keycloak thuc trong automated test

---

## 3. Chien luoc kiem thu bao mat

| Scenario | Expected | Ghi chu |
|---|---|---|
| Request khong co Authorization header | 401 | Di qua SecurityFilterChain thuc te |
| Token khong hop le / sai chu ky | 401 | |
| Token hop le nhung user khong co permission | 403 | |
| Token co du permission | 2xx | |
| Customer goi Admin API | 403 | |
| Customer truy cap ticket nguoi khac | 404 | Anti-IDOR |
| Customer truy cap notification nguoi khac | 404 | Anti-IDOR |
| Public endpoint khong can token | 200 | |
| Endpoint private khong bi mo | 401 khi khong co token | |

---

## 4. Test Cases

### 4.1 Module: Authentication

| TC-ID | Muc tieu | Input | Expected | Loai |
|---|---|---|---|---|
| AUTH-001 | Khong co token -> 401 | GET /support-requests/me, no header | 401, code=1002 | Integration |
| AUTH-002 | Token khong hop le -> 401 | Bearer invalid_token | 401 | Integration |
| AUTH-003 | Token hop le -> 2xx | Bearer valid_jwt | 200/201 | Integration |
| AUTH-004 | User khong ton tai trong local DB -> dung Keycloak fallback | JWT khong map user local | Roles tu JWT claims | Unit |
| AUTH-005 | Local user inactive -> khong co authority | User.active=false | 403 cho protected endpoint | Unit |

### 4.2 Module: Support Request - Customer

| TC-ID | Muc tieu | Input | Expected | Loai |
|---|---|---|---|---|
| SR-C-001 | Tao yeu cau hop le | categoryCode, subject>=10, content>=20 | 201, ticketCode=SR-yyyyMMdd-XXXXX | Unit/Integration |
| SR-C-002 | Thieu subject -> 400 | subject=null | 400, fieldErrors.subject | Unit/Integration |
| SR-C-003 | subject qua ngan (<10) -> 400 | subject="abc" | 400 | Unit |
| SR-C-004 | content qua ngan (<20) -> 400 | content="short" | 400 | Unit |
| SR-C-005 | Category khong ton tai -> 404 | categoryCode=INVALID | 404, code=1601 | Unit |
| SR-C-006 | customerId null/blank -> 401 | customerId="" | 401, code=1002 | Unit |
| SR-C-007 | ticketCode dung dinh dang SR-yyyyMMdd-XXXXX | valid request | startsWith("SR-") | Unit |
| SR-C-008 | Sinh unique code khi concurrent | 10 threads | 10 unique codes | Unit |
| SR-C-009 | Lich su CREATED duoc ghi | valid request | history.action=CREATED | Unit |
| SR-C-010 | Lay danh sach cua minh | GET /support-requests/me, X-User-Id | 200, chi ticket cua user do | Unit/Integration |
| SR-C-011 | Xem chi tiet ticket cua minh | GET /support-requests/me/{id}, dung owner | 200, detail | Unit |
| SR-C-012 | Truy cap ticket nguoi khac -> 404 | GET /support-requests/me/{id}, sai owner | 404, code=1602 | Unit/Integration |
| SR-C-013 | Xem lich su cua minh | GET /support-requests/me/{id}/histories | 200, list history | Unit |
| SR-C-014 | Khong co token -> 401 | GET /support-requests/me, no auth | 401, code=1002 | Integration |
| SR-C-015 | Date range hop le | fromDate<=toDate | 200 | Unit |
| SR-C-016 | Date range sai (fromDate > toDate) | from=2026-12-31, to=2026-01-01 | 400, code=1609 | Unit |

### 4.3 Module: Support Request - Admin/Staff

| TC-ID | Muc tieu | Input | Expected | Loai |
|---|---|---|---|---|
| SR-A-001 | Lay danh sach co phan trang, bo loc | query, pageable | 200, page result | Unit |
| SR-A-002 | fromDate > toDate -> 400 | INVALID_DATE_RANGE | 400, code=1609 | Unit |
| SR-A-003 | Tiep nhan ticket NEW -> RECEIVED | sr-new-id, actorId | 200, status=RECEIVED, receivedAt set | Unit |
| SR-A-004 | Tiep nhan ticket khong phai NEW -> 400 | sr-received-id | 400, code=1603 | Unit |
| SR-A-005 | Phan cong tu RECEIVED -> IN_PROGRESS | assignedTo=staffId | 200, status=IN_PROGRESS | Unit |
| SR-A-006 | Phan cong lai khi IN_PROGRESS | reassign | 200, status van IN_PROGRESS | Unit |
| SR-A-007 | Phan cong ticket NEW -> 400 | sr-new-id | 400, code=1606 | Unit |
| SR-A-008 | Phan cong ticket COMPLETED -> 400 | | 400, code=1603 | Unit |
| SR-A-009 | Phan cong ticket CLOSED -> 400 | | 400, code=1604 | Unit |
| SR-A-010 | Staff khong ton tai -> 404 | assignedTo=invalid | 404, code=1607 | Unit |
| SR-A-011 | Staff inactive -> 400 | user.active=false | 400, code=1608 | Unit |
| SR-A-012 | Staff role USER -> 400 | role.name=USER | 400, code=1608 | Unit |
| SR-A-013 | Staff khong co permission SUPPORT_REQUEST_PROCESS -> 400 | | 400, code=1608 | Unit |
| SR-A-014 | Permission inactive -> 400 | permission.active=false | 400, code=1608 | Unit |
| SR-A-015 | Chuyen RECEIVED->IN_PROGRESS hop le | | 200, status=IN_PROGRESS | Unit |
| SR-A-016 | WAITING_CUSTOMER thieu note -> 400 | note=" " | 400, code=1605 | Unit |
| SR-A-017 | COMPLETED thieu resolution -> 400 | resolution="" | 400, code=1610 | Unit |
| SR-A-018 | COMPLETED voi resolution thanh cong | | 200, completedAt set | Unit |
| SR-A-019 | CLOSED thanh cong | | 200, closedAt set | Unit |
| SR-A-020 | Cap nhat ticket CLOSED -> 400 | | 400, code=1604 | Unit |
| SR-A-021 | Them ghi chu tu NEW -> 400 | | 400, code=1603 | Unit |
| SR-A-022 | Them ghi chu tu CLOSED -> 400 | | 400, code=1604 | Unit |
| SR-A-023 | Them ghi chu IN_PROGRESS thanh cong | | 200, note saved | Unit |
| SR-A-024 | Them ghi chu blank -> 400 | note=" " | 400, code=1605 | Unit |
| SR-A-025 | Danh sach assignees eligible | keyword search | 200, list | Unit |
| SR-A-026 | HTTP 401 khi khong co token (admin endpoint) | no auth | 401 | Integration |
| SR-A-027 | HTTP 403 khi Customer goi Admin API | CUSTOMER role | 403 | Integration |
| SR-A-028 | HTTP 403 khi thieu SUPPORT_REQUEST_VIEW permission | wrong perm | 403 | Integration |
| SR-A-029 | HTTP 200 khi co du permission | ADMIN role + perm | 200 | Integration |

### 4.4 Module: Notification

| TC-ID | Muc tieu | Input | Expected | Loai |
|---|---|---|---|---|
| NOTIF-001 | createNotification khi sourceHistoryId moi | historyId="hist-1", not exists | save() duoc goi | Unit |
| NOTIF-002 | Khong tao trung khi sourceHistoryId da ton tai | historyId="hist-1", exists=true | save() khong goi | Unit |
| NOTIF-003 | markAsRead cap nhat read=true va readAt | owner request | read=true, readAt!=null | Unit |
| NOTIF-004 | markAsRead cua user khac -> 404 | notifId khong cua user | 404, code=1701 | Unit |
| NOTIF-005 | markAsRead idempotent (da doc roi) | read=true, call again | save() khong goi lai | Unit |
| NOTIF-006 | getUnreadCount dung | | count=5 | Unit |
| NOTIF-007 | getMyNotifications tra PageResponse dung | | result.size=1 | Unit |
| NOTIF-008 | markAllAsRead chi tac dong recipient hien tai | userId="cust-1" | markAllAsRead(cust-1) | Unit |
| NOTIF-009 | recipientId null -> log warn, khong save | recipientId=null | save() khong goi | Unit |
| NOTIF-010 | Notification tao khi ticket RECEIVED | receiveTicket | createNotification called | Integration |
| NOTIF-011 | Notification tao khi status changed | updateStatus | createNotification called | Integration |
| NOTIF-012 | HTTP 401 khi khong co token | no auth | 401 | Integration |
| NOTIF-013 | HTTP 200 khi co token | valid JWT | 200 | Integration |

### 4.5 Module: RBAC Security

| TC-ID | Muc tieu | Input | Expected | Loai |
|---|---|---|---|---|
| RBAC-001 | Khong co token -> 401 thuc qua SecurityFilterChain | GET /support-requests/me | 401, code=1002 | Integration |
| RBAC-002 | Token sai cu phap -> 401 | Bearer INVALID | 401 | Integration |
| RBAC-003 | Token hop le, role CUSTOMER -> admin API -> 403 | JWT customer, GET /admin/* | 403, code=1003 | Integration |
| RBAC-004 | Token ADMIN -> admin API -> 200 | JWT admin + perm | 200 | Integration |
| RBAC-005 | Public endpoint khong can token | GET /plans, GET /service-categories | 200 | Integration |
| RBAC-006 | Swagger khong can token | GET /v3/api-docs | 200 | Integration |

### 4.6 Module: Exception / Validation

| TC-ID | Muc tieu | Input | Expected | Loai |
|---|---|---|---|---|
| EX-001 | MethodArgumentNotValidException -> 400 + fieldErrors | body invalid | 400, details map | Unit |
| EX-002 | HttpMessageNotReadable -> 400 | malformed JSON | 400, code=1023 | Unit |
| EX-003 | Type mismatch -> 400 | enum=INVALID | 400, code=1001 | Unit |
| EX-004 | OptimisticLocking -> 409 | concurrent update | 409, code=1611 | Unit |
| EX-005 | DataIntegrityViolation -> 409 | db constraint | 409 | Unit |
| EX-006 | AccessDenied -> 403 | | 403, code=1003 | Unit |
| EX-007 | RuntimeException -> 500 khong lo stack trace | NPE | 500, message khong chua class name | Unit |
| EX-008 | AppException -> HTTP status theo ErrorCode | SUPPORT_REQUEST_NOT_EXISTED | 404 | Unit |

---

## 5. Coverage Targets

| Package | Target | Priority |
|---|---|---|
| `service.impl.*` | >= 80% line | P0 |
| `controller.*` | >= 70% line | P1 |
| `advice.*` | >= 90% line | P0 |
| Tong the | >= 70% line | P1 |

---

## 6. Cong cu

| Cong cu | Muc dich |
|---|---|
| JUnit 5 + Mockito | Unit test |
| MockMvc + spring-security-test | Integration test |
| JaCoCo | Coverage report |
| Postman (manual) | API testing |
| Newman (neu co) | API test tu dong |

---

## 7. Dinh nghia Done

- [ ] Tat ca test pass (`mvnw test`)
- [ ] So test > baseline 148
- [ ] Co HTTP integration test thuc te cho 401/403
- [ ] Luong Support Request day du da duoc test
- [ ] Notification test du
- [ ] Coverage report duoc tao
- [ ] Admin Portal lint pass va build pass
- [ ] Customer Web lint pass va build pass
- [ ] Khong con bug Critical/High chua xu ly
- [ ] Tai lieu Tuan 9 hoan chinh
