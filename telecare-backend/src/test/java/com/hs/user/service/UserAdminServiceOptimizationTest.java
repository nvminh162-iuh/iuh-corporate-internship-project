package com.hs.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.hs.user.advice.base.AppException;
import com.hs.user.constant.base.ErrorCode;
import com.hs.user.dto.response.UserResponse;
import com.hs.user.model.Role;
import com.hs.user.model.User;
import com.hs.user.repository.RoleRepository;
import com.hs.user.repository.UserRepository;
import com.hs.user.service.impl.UserServiceImpl;
import com.hs.user.utils.CurrentUserUtils;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceOptimizationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private CurrentUserUtils currentUserUtils;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("findAllUsers: should query with specification and clamp oversized page to 100")
    void findAllUsers_oversizedPage_clampedTo100() {
        Pageable requested = PageRequest.of(0, 500, Sort.by("createdAt").descending());
        Role role = Role.builder().id("role-1").name("CUSTOMER").build();
        User user = new User();
        user.setId("u-1");
        user.setUsername("testuser");
        user.setEmail("test@telecare.vn");
        user.setActive(true);
        user.setRole(role);

        Page<User> mockPage = new PageImpl<>(List.of(user), PageRequest.of(0, 100), 1);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        Page<UserResponse> result = userService.findAllUsers("test", true, "role-1", requested);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).username()).isEqualTo("testuser");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("findAllUsers: invalid sort field should throw INVALID_SORT_FIELD")
    void findAllUsers_invalidSortField_throwsAppException() {
        Pageable requested = PageRequest.of(0, 10, Sort.by("invalidPasswordHash").descending());

        assertThatThrownBy(() -> userService.findAllUsers(null, null, null, requested))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_SORT_FIELD);
    }

    @Test
    @DisplayName("findAllUsers: valid sort field should append tie-breaker id sort")
    void findAllUsers_validSortField_appendsTieBreaker() {
        Pageable requested = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "username"));
        Page<User> mockPage = new PageImpl<>(List.of(), requested, 0);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        userService.findAllUsers(null, null, null, requested);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Sort sort = pageableCaptor.getValue().getSort();
        assertThat(sort.getOrderFor("username")).isNotNull();
        assertThat(sort.getOrderFor("id")).isNotNull();
    }
}
