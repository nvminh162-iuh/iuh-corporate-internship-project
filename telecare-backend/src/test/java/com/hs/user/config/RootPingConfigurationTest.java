package com.hs.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.servlet.ContextPath;

class RootPingConfigurationTest {

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1", ""})
    void servesOnlyRootWithoutMovingApiRoutes(String contextPath) throws Exception {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
        factory.getSettings().setContextPath(ContextPath.of(contextPath));
        new RootPingConfiguration().rootPingWebServerCustomizer().customize(factory);

        WebServer server = factory.getWebServer(servletContext -> {
            servletContext.addServlet("test-api", new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                    if ("/actuator/health".equals(request.getServletPath())) {
                        response.setContentType("application/json");
                        response.getWriter().write("{\"status\":\"UP\"}");
                    } else if ("/users/me/profile".equals(request.getServletPath())) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                }
            }).addMapping("/");
        });
        try {
            server.start();
            try (HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build()) {
                String baseUrl = "http://localhost:" + server.getPort();
                HttpResponse<String> root = send(client, baseUrl + "/", "GET");
                assertThat(root.statusCode()).isEqualTo(200);
                assertThat(root.body()).isEqualTo("ping - pong");
                assertThat(root.headers().firstValue("Content-Type")).hasValue("text/plain;charset=UTF-8");
                assertThat(root.headers().firstValue("Cache-Control")).hasValue("no-store");
                if (!contextPath.isEmpty()) {
                    assertThat(root.headers().firstValue("X-Request-Id")).isPresent();
                    HttpResponse<String> correlated = client.send(HttpRequest.newBuilder(URI.create(baseUrl + "/"))
                                    .header("X-Request-Id", "test-root-ping").GET().build(),
                            HttpResponse.BodyHandlers.ofString());
                    assertThat(correlated.headers().firstValue("X-Request-Id")).hasValue("test-root-ping");
                }
                assertThat(send(client, baseUrl + "/?ping=1", "GET").body()).isEqualTo("ping - pong");

                HttpResponse<String> head = send(client, baseUrl + "/", "HEAD");
                assertThat(head.statusCode()).isEqualTo(200);
                assertThat(head.body()).isEmpty();
                assertThat(send(client, baseUrl + "/", "POST").statusCode()).isEqualTo(405);
                assertThat(send(client, baseUrl + "/not-an-endpoint", "GET").statusCode()).isEqualTo(404);

                HttpResponse<String> health = send(client, baseUrl + contextPath + "/actuator/health", "GET");
                assertThat(health.statusCode()).isEqualTo(200);
                assertThat(health.body()).isEqualTo("{\"status\":\"UP\"}");
                assertThat(send(client, baseUrl + contextPath + "/users/me/profile", "GET").statusCode())
                        .isEqualTo(401);
                if (!contextPath.isEmpty()) {
                    assertThat(send(client, baseUrl + "/users/me/profile", "GET").statusCode()).isEqualTo(404);
                    assertThat(send(client, baseUrl + "/actuator/health", "GET").statusCode()).isEqualTo(404);
                }
            }
        } finally {
            server.stop();
            server.destroy();
        }
    }

    private HttpResponse<String> send(HttpClient client, String url, String method) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .method(method, HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
