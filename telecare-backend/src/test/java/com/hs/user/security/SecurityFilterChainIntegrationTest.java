package com.hs.user.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(controllers = {
        NotificationController.class,
        SupportRequestAdminController.class,
        ServiceCategoryPublicController.class
})
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
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

    @Test
    @DisplayName("Private support endpoint rejects requests without authentication")
    void privateEndpoint_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin support endpoint rejects a customer authority")
    void adminEndpoint_customerAuthority_returns403() throws Exception {
        mockMvc.perform(get("/admin/support-requests")
                        .with(jwt().authorities(new SimpleGrantedAuthority("CUSTOMER"))))
                .andExpect(status().isForbidden());
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
}
