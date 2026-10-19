package com.hs.user.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hs.user.config.security.CustomAccessDeniedHandler;
import com.hs.user.config.security.CustomAuthenticationEntryPoint;
import com.hs.user.config.security.CustomJwtAuthenticationConverter;
import com.hs.user.config.security.SecurityConfig;
import com.hs.user.controller.NotificationController;
import com.hs.user.controller.admin.SupportRequestAdminController;
import com.hs.user.controller.publicapi.ServiceCategoryPublicController;
import com.hs.user.dto.response.PublicCategoryResponse;
import com.hs.user.dto.response.SupportRequestAdminSummaryResponse;
import com.hs.user.service.NotificationService;
import com.hs.user.service.PublicPlanService;
import com.hs.user.service.SupportRequestAdminService;
import com.hs.user.utils.CurrentUserUtils;
import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;

@WebMvcTest(controllers = {
        SecurityFilterChainIntegrationTest.RootTestController.class,
        NotificationController.class,
        SupportRequestAdminController.class,
        ServiceCategoryPublicController.class
})
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
        SecurityFilterChainIntegrationTest.RootTestController.class,
        SecurityFilterChainIntegrationTest.Jackson2TestConfiguration.class})
@DisplayName("Security filter chain HTTP tests")
class SecurityFilterChainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomJwtAuthenticationConverter jwtAuthenticationConverter;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockitoBean(name = "auditorAware")
    private AuditorAware<String> auditorAware;

    @MockitoBean
    private SupportRequestAdminService supportRequestAdminService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private PublicPlanService publicPlanService;

    @MockitoBean
    private CurrentUserUtils currentUserUtils;

    @TestConfiguration
    static class Jackson2TestConfiguration {
        @Bean
        com.fasterxml.jackson.databind.ObjectMapper jackson2ObjectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper();
        }
    }

    @RestController
    static class RootTestController {
        @GetMapping(value = "/", produces = "text/plain")
        String ping() {
            return "ping - pong";
        }

        @GetMapping("/public/logging-validation")
        String invalidInput() {
            throw new AppException(ErrorCode.INVALID_PRICE_RANGE);
        }

        @GetMapping("/public/logging-failure")
        String unexpectedFailure() {
            throw new IllegalStateException("synthetic-private-exception-message");
        }
    }

    @Test
    @DisplayName("Root GET and HEAD do not require authentication")
    void root_withoutAuthentication_returns200() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string("ping - pong"));
        mockMvc.perform(head("/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Root public access does not permit unauthenticated writes")
    void rootPost_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Private support endpoint rejects requests without authentication")
    void privateEndpoint_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/notifications").header("X-Request-Id", "test-private-401"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", "test-private-401"))
                .andExpect(jsonPath("$.traceId").value("test-private-401"));
    }

    @Test
    @DisplayName("Admin support endpoint rejects a customer authority")
    void adminEndpoint_customerAuthority_returns403() throws Exception {
        mockMvc.perform(get("/admin/support-requests")
                        .header("X-Request-Id", "test-admin-403")
                        .with(jwt().authorities(new SimpleGrantedAuthority("CUSTOMER"))))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Request-Id", "test-admin-403"))
                .andExpect(jsonPath("$.traceId").value("test-admin-403"));
    }

    @Test
    void browserCanReadRequestIdAcrossOrigins() throws Exception {
        when(publicPlanService.findAllPublicCategories()).thenReturn(List.<PublicCategoryResponse>of());
        mockMvc.perform(get("/service-categories")
                        .header("Origin", "http://localhost:53000")
                        .header("X-Request-Id", "test-browser-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Expose-Headers", "X-Request-Id"))
                .andExpect(header().string("X-Request-Id", "test-browser-id"));
    }

    @Test
    void mvcErrorsCorrelateHeaderAndErrorBody() throws Exception {
        mockMvc.perform(get("/public/logging-validation").header("X-Request-Id", "test-validation-400"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Request-Id", "test-validation-400"))
                .andExpect(jsonPath("$.traceId").value("test-validation-400"));
        mockMvc.perform(get("/public/logging-failure").header("X-Request-Id", "test-error-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Request-Id", "test-error-500"))
                .andExpect(jsonPath("$.traceId").value("test-error-500"))
                .andExpect(jsonPath("$.message").value(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage()));
    }

    @Test
    @DisplayName("Admin support endpoint accepts the required view permission")
    void adminEndpoint_viewPermission_returns200() throws Exception {
        when(supportRequestAdminService.findAllAdminSupportRequests(any(), any()))
                .thenReturn(Page.<SupportRequestAdminSummaryResponse>empty());

        mockMvc.perform(get("/admin/support-requests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SUPPORT_REQUEST_VIEW"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public service categories are accessible without authentication")
    void publicCategories_withoutAuthentication_returns200() throws Exception {
        when(publicPlanService.findAllPublicCategories()).thenReturn(List.<PublicCategoryResponse>of());

        mockMvc.perform(get("/service-categories"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://telecare.nvminh162.id.vn",
            "https://telecare-seven.vercel.app",
            "https://telecare-admin.nvminh162.id.vn",
            "https://telecare-admin.vercel.app",
            "http://localhost:53000",
            "http://localhost:55000",
            "http://localhost:3000",
            "http://localhost:5000"
    })
    @DisplayName("Configured frontend origins can preflight authenticated API requests")
    void corsPreflight_allowedOrigin_returns200(String origin) throws Exception {
        mockMvc.perform(options("/notifications")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "authorization, content-type"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://telecare.nvminh162.id.vn",
            "https://telecare-seven.vercel.app",
            "https://telecare-admin.nvminh162.id.vn",
            "https://telecare-admin.vercel.app",
            "http://localhost:53000",
            "http://localhost:55000",
            "http://localhost:3000",
            "http://localhost:5000"
    })
    @DisplayName("Public API responses include CORS headers for configured origins")
    void publicCategories_allowedOrigin_returnsCorsHeaders(String origin) throws Exception {
        when(publicPlanService.findAllPublicCategories()).thenReturn(List.<PublicCategoryResponse>of());

        mockMvc.perform(get("/service-categories").header(HttpHeaders.ORIGIN, origin))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://telecare.nvminh162.id.vn",
            "https://telecare-seven.vercel.app",
            "https://telecare-admin.nvminh162.id.vn",
            "https://telecare-admin.vercel.app"
    })
    @DisplayName("Allowing a production origin does not bypass authentication")
    void privateEndpoint_allowedOrigin_withoutAuthentication_returns401(String origin) throws Exception {
        mockMvc.perform(get("/notifications").header(HttpHeaders.ORIGIN, origin))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://telecare.nvminh162.id.vn",
            "http://telecare-seven.vercel.app",
            "https://telecare.nvminh162.id.vn.untrusted.example",
            "https://unlisted-preview.vercel.app",
            "null"
    })
    @DisplayName("CORS rejects origins outside the explicit allowlist")
    void corsPreflight_unlistedOrigin_returns403(String origin) throws Exception {
        mockMvc.perform(options("/notifications")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
