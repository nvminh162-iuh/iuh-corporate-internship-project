# HTTP request logging — week 10

## What is recorded

Each completed request through the backend application emits one summary:

```text
INFO [requestId=week10-demo] HTTP requestId=week10-demo method=GET path=/api/v1/plans status=200 durationMs=24 actorId=anonymous
WARN [requestId=week10-private] HTTP requestId=week10-private method=GET path=[unmapped] status=401 durationMs=2 actorId=anonymous
```

These are illustrative examples, not benchmark results.

- Status below 400: INFO; 4xx: WARN; 5xx: ERROR.
- Duration uses a monotonic clock. It measures server-side filter processing,
  not browser download time or end-to-end latency.
- The request ID is returned as `X-Request-Id`. Clients may supply 1–64 ASCII
  letters, digits, underscores or hyphens; other values are replaced with a UUID.
  This is a correlation label, not an authentication or authorization credential.
- Service/error logs on the request thread carry the same ID via MDC. Thread
  context is restored afterwards; no ID is leaked into the next request.
- A verified JWT subject is captured before user context cleanup. Legacy
  `X-User-Id` / `X-User-Email` headers are not trusted as logging identities.
- MVC paths use route templates, e.g. `/api/v1/users/{id}`. Requests rejected
  before mapping, or requests without a route template, use `[unmapped]`.
- Asynchronous summaries wait for completion and use the final response status.
  This does not automatically propagate MDC into arbitrary background tasks.
- The separate root ping servlet context also has the logging filter.

No request/response body, query string, Authorization header, cookie or arbitrary
header is collected by the request logger. It does not buffer bodies or uploads.
Expected exception logs contain error types/codes/counts rather than raw messages,
SQL values or validation values. Unexpected MVC errors retain a maximum of 20 code
locations, without exception/cause messages. Existing business-service logs are
separate from the HTTP summary and should also be reviewed before sharing logs.

## Configuration

No new environment variables are required. Existing `LOGGING_LEVEL_ROOT` and
`LOGGING_LEVEL_APP` control logging; use `INFO` to see successful HTTP summaries.
Console output works both locally and with container logs. This change does not
enable SQL parameter logging, file logging, or a production log collector.
Restrict access and retention for logs, especially authenticated subject IDs.

## Local demonstration

Start the backend with its existing local environment, then run:

```powershell
curl.exe -i -H "X-Request-Id: week10-demo" "http://localhost:55080/api/v1/plans?page=0&size=10"
curl.exe -i -H "X-Request-Id: week10-private" "http://localhost:55080/api/v1/notifications"
curl.exe -i -H "X-Request-Id: week10-root" "http://localhost:55080/"
```

Verify that the header and console summary have the same request ID. The private
request without a token returns 401, with the same ID in the error `traceId`.
MVC errors and security-handler 401/403 responses are correlated. CORS exposes
`X-Request-Id` so an allowed frontend can read it.

To test validation, use an invalid price range and check that no raw input is logged:

```powershell
curl.exe -i -H "X-Request-Id: week10-validation" "http://localhost:55080/api/v1/plans?minPrice=200000&maxPrice=50000"
```

Do not trigger destructive operations or intentionally break production services
for a logging demo. Automated tests use synthetic secrets and mock requests.

```powershell
mvn --batch-mode --no-transfer-progress clean test "-Dtest=RequestIdFilterTest,GlobalExceptionHandlingTest,SecurityFilterChainIntegrationTest,RootPingConfigurationTest,ApplicationEnvironmentConfigurationTest"
```

The complete existing test suite also contains full-context integration tests
that require the local infrastructure (including PostgreSQL on port 55432).

References:

- https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html
- https://docs.spring.io/spring-boot/reference/features/logging.html
