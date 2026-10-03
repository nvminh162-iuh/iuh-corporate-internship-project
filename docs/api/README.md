# TeleCare Platform API Documentation

## 1. Overview

The TeleCare Platform provides a secure, role-based RESTful API designed for telecommunications customer care and healthcare service coordination.

- **Base Context Path**: `/api/v1`
- **Default Port**: `55080` (Direct) / `58080` (Gateway)
- **API Protocol**: HTTP/1.1 & HTTP/2 over TLS
- **Security Standard**: OAuth2 Resource Server / Keycloak JWT Bearer Tokens

---

## 2. Interactive API Documentation (OpenAPI / Swagger)

Springdoc OpenAPI 3 is integrated into the backend:

- **Swagger UI**: [http://localhost:55080/api/v1/swagger-ui/index.html](http://localhost:55080/api/v1/swagger-ui/index.html)
- **OpenAPI JSON**: [http://localhost:55080/api/v1/v3/api-docs](http://localhost:55080/api/v1/v3/api-docs)

To authenticate in Swagger UI:
1. Click the **Authorize** button (padlock icon).
2. Enter your JWT Bearer token: `Bearer <your_token>`.
3. Click **Authorize** and then **Close**.

---

## 3. Standard API Response Structure

All endpoints return a uniform JSON envelope:

```json
{
  "code": 1000,
  "message": "Success message (or null on standard queries)",
  "result": { ... },
  "details": null,
  "timestamp": "2026-10-03T06:30:00Z",
  "path": "/api/v1/support-requests/me",
  "traceId": "7b2c918a-4d22-4919-91a1-cf0b9d99a012"
}
```

### Fields:
- `code`: `1000` for success; non-1000 numeric code on error.
- `message`: Human-readable description.
- `result`: Response payload (single object, list, or pagination envelope).
- `details`: Optional validation errors map (e.g. `{"subject": "Subject is required"}`).
- `timestamp`: UTC ISO-8601 timestamp.
- `path`: Request URI path.
- `traceId`: Correlation ID matching `X-Request-Id`.

---

## 4. Pagination Standard (`PageResponse<T>`)

Paginated endpoints accept standard query parameters:
- `page`: 0-indexed page number (default: `0`).
- `size`: Items per page (default: `10`, maximum: `100`).
- `sort`: Field and direction (e.g. `createdAt,desc`).

```json
{
  "code": 1000,
  "result": {
    "content": [ ... ],
    "page": 0,
    "size": 10,
    "totalElements": 42,
    "totalPages": 5,
    "last": false
  }
}
```

---

## 5. Correlation & Tracing (`X-Request-Id`)

All requests accept an optional `X-Request-Id` HTTP header. If omitted, the backend generates a UUID.
- The `X-Request-Id` is bound to SLF4J MDC (`requestId`).
- The response returns the `X-Request-Id` header.
- On error, `traceId` is included in the JSON body.

---

## 6. Core Endpoints Summary

### 6.1 Customer Support Requests
| Method | Endpoint | Authority | Description |
|---|---|---|---|
| `POST` | `/api/v1/support-requests` | Authenticated | Create a new support ticket |
| `GET` | `/api/v1/support-requests/me` | Authenticated | List customer's own tickets (filter, paginate) |
| `GET` | `/api/v1/support-requests/me/{id}` | Authenticated | View own ticket details (404 if not owner) |
| `GET` | `/api/v1/support-requests/me/{id}/histories` | Authenticated | View customer-safe timeline (no internal notes) |

### 6.2 Notifications
| Method | Endpoint | Authority | Description |
|---|---|---|---|
| `GET` | `/api/v1/notifications` | Authenticated | List recipient notifications (unreadOnly filter) |
| `GET` | `/api/v1/notifications/unread-count` | Authenticated | Get unread notifications badge count |
| `PATCH` | `/api/v1/notifications/{id}/read` | Authenticated | Mark notification as read (idempotent, 404 if not owner) |
| `PATCH` | `/api/v1/notifications/read-all` | Authenticated | Mark all notifications as read for current user |

### 6.3 Admin Support Management
| Method | Endpoint | Authority | Description |
|---|---|---|---|
| `GET` | `/api/v1/admin/support-requests` | `ADMIN` or `SUPPORT_REQUEST_VIEW` | Search and filter all support tickets |
| `GET` | `/api/v1/admin/support-requests/{id}` | `ADMIN` or `SUPPORT_REQUEST_VIEW` | Full admin ticket details |
| `PATCH` | `/api/v1/admin/support-requests/{id}/receive` | `ADMIN` or `SUPPORT_REQUEST_PROCESS` | Receive ticket (NEW -> RECEIVED) |
| `PATCH` | `/api/v1/admin/support-requests/{id}/assign` | `ADMIN` or `SUPPORT_REQUEST_ASSIGN` | Assign / reassign ticket |
| `PATCH` | `/api/v1/admin/support-requests/{id}/status` | `ADMIN` or `SUPPORT_REQUEST_PROCESS` | Transition ticket status |
| `POST` | `/api/v1/admin/support-requests/{id}/histories` | `ADMIN` or `SUPPORT_REQUEST_PROCESS` | Add internal processing note |
| `GET` | `/api/v1/admin/support-requests/assignees` | `ADMIN` or `SUPPORT_REQUEST_ASSIGN` | List eligible assignees |

### 6.4 Service Plans & Categories (Admin & Public)
| Method | Endpoint | Authority | Description |
|---|---|---|---|
| `GET` | `/api/v1/plans` | Public | List active service plans |
| `GET` | `/api/v1/admin/plans` | `ADMIN` or `PLAN_VIEW` | Admin plan management list |
| `POST` | `/api/v1/admin/plans` | `ADMIN` or `PLAN_CREATE` | Create new plan |
| `PUT` | `/api/v1/admin/plans/{id}` | `ADMIN` or `PLAN_UPDATE` | Update plan |
| `DELETE` | `/api/v1/admin/plans/{id}` | `ADMIN` or `PLAN_DELETE` | Soft delete plan |
| `GET` | `/api/v1/admin/service-categories` | `ADMIN` or `CATEGORY_VIEW` | List categories |
| `POST` | `/api/v1/admin/service-categories` | `ADMIN` or `CATEGORY_CREATE` | Create category |
| `PUT` | `/api/v1/admin/service-categories/{id}` | `ADMIN` or `CATEGORY_UPDATE` | Update category |
| `PATCH` | `/api/v1/admin/service-categories/{id}/disable` | `ADMIN` or `CATEGORY_DELETE` | Disable category |

### 6.5 User & Permissions
| Method | Endpoint | Authority | Description |
|---|---|---|---|
| `GET` | `/api/v1/users/me/permissions` | Authenticated | Current user's role and permissions |
| `GET` | `/api/v1/internal/users/{id}/permissions` | `INTERNAL_SERVICE` | Internal service-to-service permission check |
| `GET` | `/api/v1/admin/users` | `ADMIN` or `USER_VIEW` | List users |
| `GET` | `/api/v1/admin/roles` | `ADMIN` or `ROLE_VIEW` | List roles |
| `GET` | `/api/v1/admin/permissions` | `ADMIN` or `PERMISSION_VIEW` | List permissions |

---

## 7. Example cURL

### Fetching Customer's Own Tickets:
```bash
curl -X GET "http://localhost:55080/api/v1/support-requests/me?page=0&size=10" \
  -H "Authorization: Bearer <customer_jwt_token>" \
  -H "X-Request-Id: test-req-123"
```

### Checking Notifications Unread Count:
```bash
curl -X GET "http://localhost:55080/api/v1/notifications/unread-count" \
  -H "Authorization: Bearer <customer_jwt_token>"
```
