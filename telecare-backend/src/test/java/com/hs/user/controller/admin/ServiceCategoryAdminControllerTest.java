package com.hs.user.controller.admin;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.user.advice.exception.GlobalException;
import com.hs.user.dto.request.ServiceCategoryRequest;
import com.hs.user.dto.response.ServiceCategoryResponse;
import com.hs.user.service.ServiceCategoryService;

@ExtendWith(MockitoExtension.class)
class ServiceCategoryAdminControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ServiceCategoryService serviceCategoryService;

    @InjectMocks
    private ServiceCategoryAdminController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalException())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void findAllCategories_ShouldReturnPagedResponse() throws Exception {
        ServiceCategoryResponse categoryResponse = ServiceCategoryResponse.builder()
                .id("cat-1")
                .code("INTERNET")
                .name("Internet")
                .active(true)
                .build();

        given(serviceCategoryService.findAllCategories(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(categoryResponse)));

        mockMvc.perform(get("/admin/service-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.result[0].id").value("cat-1"))
                .andExpect(jsonPath("$.result.result[0].code").value("INTERNET"));
    }

    @Test
    void createCategory_ShouldReturn201Created() throws Exception {
        ServiceCategoryRequest request = new ServiceCategoryRequest("INTERNET", "Internet Broadband", "Description", 1);
        ServiceCategoryResponse response = ServiceCategoryResponse.builder()
                .id("cat-1")
                .code("INTERNET")
                .name("Internet Broadband")
                .active(true)
                .build();

        given(serviceCategoryService.createCategory(any(ServiceCategoryRequest.class)))
                .willReturn(response);

        mockMvc.perform(post("/admin/service-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value("cat-1"))
                .andExpect(jsonPath("$.message").value("Service category created successfully"));
    }

    @Test
    void updateCategory_ShouldReturn200Ok() throws Exception {
        ServiceCategoryRequest request = new ServiceCategoryRequest("INTERNET_FTTH", "Internet FTTH", "Desc", 1);
        ServiceCategoryResponse response = ServiceCategoryResponse.builder()
                .id("cat-1")
                .code("INTERNET_FTTH")
                .name("Internet FTTH")
                .active(true)
                .build();

        given(serviceCategoryService.updateCategory(eq("cat-1"), any(ServiceCategoryRequest.class)))
                .willReturn(response);

        mockMvc.perform(put("/admin/service-categories/cat-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.code").value("INTERNET_FTTH"));
    }

    @Test
    void disableCategory_ShouldReturn200Ok() throws Exception {
        doNothing().when(serviceCategoryService).disableCategory("cat-1");

        mockMvc.perform(patch("/admin/service-categories/cat-1/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Service category disabled successfully"));
    }
}
