# TeleCare Error Catalog & Exception Handling Guide

## 1. Exception Handling Architecture

The TeleCare backend features a centralized, uniform exception handling architecture via `@ControllerAdvice` in `com.hs.user.advice.exception.GlobalException`.

### Key Principles:
1. **Never leak stack traces** or internal implementation details to the client in production responses.
2. **Structured validation errors**: Multiple field validation errors return a `details` map containing all offending fields and their constraint violation messages.
3. **Traceability**: Every error includes a `traceId` mapped from `X-Request-Id` (or generated UUID) and a UTC `timestamp` alongside the request `path`.
4. **Proper HTTP Status Codes**:
   - `400 Bad Request`: Validation failure, missing parameters, malformed JSON, invalid enums/dates.
   - `401 Unauthorized`: Missing, expired, or invalid JWT bearer token.
   - `403 Forbidden`: Authenticated user lacks required role/permission.
   - `404 Not Found`: Entity not found, or ownership check failure on customer endpoints.
   - `409 Conflict`: Unique constraint violation, state conflicts, or optimistic locking collision.
   - `500 Internal Server Error`: Unhandled runtime exceptions with correlated trace ID logged on server.

---

## 2. Standard Error Response Envelope

```json
{
  "code": 1001,
  "message": "Validation failed",
  "result": null,
  "details": {
    "subject": "Subject must be between 10 and 200 characters",
    "content": "Content must be between 20 and 1000 characters"
  },
  "timestamp": "2026-10-03T06:40:00Z",
  "path": "/api/v1/support-requests",
  "traceId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
}
```

---

## 3. Comprehensive Error Code Catalog

### 3.1 Common & System Errors
| Error Code | HTTP Status | Message | Description |
|---|---|---|---|
| `9999` | 500 Internal Server Error | Uncategorized error | Unhandled system exception |
| `1001` | 400 Bad Request | Uncategorized error / Validation failed | General validation error |
| `1002` | 401 Unauthorized | Unauthenticated | Missing or invalid authentication token |
| `1003` | 403 Forbidden | You do not have permission | Insufficient role or permission |
| `1012` | 404 Not Found | Route not found | Request path does not map to any endpoint |
| `1023` | 400 Bad Request | Invalid request body or malformed payload | Unparseable JSON, invalid enum value, or type mismatch |
| `1024` | 409 Conflict | Data conflict or integrity constraint violation | Database unique constraint violation |

### 3.2 User & Profile Errors (1004–1022)
| Error Code | HTTP Status | Message |
|---|---|---|
| `1004` | 400 Bad Request | User existed |
| `1005` | 404 Not Found | User not existed |
| `1006` | 400 Bad Request | User already onboarded |
| `1007` | 400 Bad Request | Invalid old password |
| `1008` | 400 Bad Request | Password is required |
| `1009` | 400 Bad Request | New password is required |
| `1010` | 400 Bad Request | Password must be at least 8 characters |
| `1011` | 400 Bad Request | Password must include at least 1 uppercase letter, 1 digit and 1 special character |
| `1013` | 400 Bad Request | Username existed |
| `1014` | 400 Bad Request | Email existed |
| `1015` | 400 Bad Request | Email already verified |
| `1016` | 400 Bad Request | User already has this role |
| `1017` | 409 Conflict | Phone number existed |
| `1018` | 400 Bad Request | User is disabled |
| `1019` | 409 Conflict | Password has already been set |
| `1020` | 409 Conflict | User already completed invitation actions |
| `1021` | 409 Conflict | You cannot disable your own account |
| `1022` | 409 Conflict | You cannot update your own role |

### 3.3 Role & Permission Errors (1101–1204)
| Error Code | HTTP Status | Message |
|---|---|---|
| `1101` | 400 Bad Request | Role existed |
| `1102` | 404 Not Found | Role not existed |
| `1103` | 409 Conflict | Cannot delete system role |
| `1104` | 409 Conflict | Role is currently assigned to active users |
| `1201` | 400 Bad Request | Permission existed |
| `1202` | 404 Not Found | Permission not existed |
| `1203` | 409 Conflict | Cannot delete system permission |
| `1204` | 409 Conflict | Permission is currently assigned to active roles |

### 3.4 Service Plan & Category Errors (1301–1411)
| Error Code | HTTP Status | Message |
|---|---|---|
| `1301` | 400 Bad Request | Category code existed |
| `1302` | 404 Not Found | Category not existed |
| `1303` | 409 Conflict | Category contains active service plans |
| `1401` | 400 Bad Request | Plan code existed |
| `1402` | 400 Bad Request | Plan slug existed |
| `1403` | 404 Not Found | Plan not existed |
| `1404` | 400 Bad Request | Duplicate plan feature |
| `1405` | 400 Bad Request | Deleted plan cannot be updated |
| `1410` | 400 Bad Request | Minimum price cannot be greater than maximum price |
| `1411` | 400 Bad Request | Invalid sort field |

### 3.5 Support Request Errors (1601–1611)
| Error Code | HTTP Status | Message |
|---|---|---|
| `1601` | 404 Not Found | Support category does not exist or is inactive |
| `1602` | 404 Not Found | Support request not existed (also returned on ownership mismatch) |
| `1603` | 400 Bad Request | Invalid support request status transition |
| `1604` | 400 Bad Request | Closed support request cannot be updated |
| `1605` | 400 Bad Request | Note or resolution details required for this status change |
| `1606` | 400 Bad Request | Ticket must be received before processing or assigning |
| `1607` | 404 Not Found | Assigned staff user does not exist |
| `1608` | 400 Bad Request | Assigned user is not an active staff member |
| `1609` | 400 Bad Request | From date cannot be after to date |
| `1610` | 400 Bad Request | Resolution details are required when completing a ticket |
| `1611` | 409 Conflict | Support request was modified concurrently. Please refresh and try again. |

### 3.6 Notification Errors (1701)
| Error Code | HTTP Status | Message |
|---|---|---|
| `1701` | 404 Not Found | Notification not existed (or not owned by current user) |
