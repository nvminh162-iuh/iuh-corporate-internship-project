package com.hs.user.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import com.hs.user.controller.internal.UserInternalController;

class AdminRBACAnnotationVerificationTest {

    @Test
    @DisplayName("ServicePlanAdminController methods have method-level PreAuthorize and class has no restrictive annotation")
    void verifyServicePlanAdminController_Permissions() {
        assertThat(ServicePlanAdminController.class.getAnnotation(PreAuthorize.class)).isNull();

        Method create = Arrays.stream(ServicePlanAdminController.class.getMethods())
                .filter(m -> m.getName().equals("createPlan"))
                .findFirst().orElseThrow();
        PreAuthorize createAuth = create.getAnnotation(PreAuthorize.class);
        assertThat(createAuth).isNotNull();
        assertThat(createAuth.value()).contains("ADMIN").contains("PLAN_CREATE");

        Method get = Arrays.stream(ServicePlanAdminController.class.getMethods())
                .filter(m -> m.getName().equals("findAllPlans"))
                .findFirst().orElseThrow();
        PreAuthorize getAuth = get.getAnnotation(PreAuthorize.class);
        assertThat(getAuth).isNotNull();
        assertThat(getAuth.value()).contains("ADMIN").contains("PLAN_VIEW");

        Method delete = Arrays.stream(ServicePlanAdminController.class.getMethods())
                .filter(m -> m.getName().equals("softDeletePlan"))
                .findFirst().orElseThrow();
        PreAuthorize delAuth = delete.getAnnotation(PreAuthorize.class);
        assertThat(delAuth).isNotNull();
        assertThat(delAuth.value()).contains("ADMIN").contains("PLAN_DELETE");
    }

    @Test
    @DisplayName("ServiceCategoryAdminController methods have method-level PreAuthorize and class has no restrictive annotation")
    void verifyServiceCategoryAdminController_Permissions() {
        assertThat(ServiceCategoryAdminController.class.getAnnotation(PreAuthorize.class)).isNull();

        Method create = Arrays.stream(ServiceCategoryAdminController.class.getMethods())
                .filter(m -> m.getName().equals("createCategory"))
                .findFirst().orElseThrow();
        PreAuthorize createAuth = create.getAnnotation(PreAuthorize.class);
        assertThat(createAuth).isNotNull();
        assertThat(createAuth.value()).contains("ADMIN").contains("CATEGORY_CREATE");

        Method get = Arrays.stream(ServiceCategoryAdminController.class.getMethods())
                .filter(m -> m.getName().equals("findAllCategories"))
                .findFirst().orElseThrow();
        PreAuthorize getAuth = get.getAnnotation(PreAuthorize.class);
        assertThat(getAuth).isNotNull();
        assertThat(getAuth.value()).contains("ADMIN").contains("CATEGORY_VIEW");

        Method disable = Arrays.stream(ServiceCategoryAdminController.class.getMethods())
                .filter(m -> m.getName().equals("disableCategory"))
                .findFirst().orElseThrow();
        PreAuthorize disAuth = disable.getAnnotation(PreAuthorize.class);
        assertThat(disAuth).isNotNull();
        assertThat(disAuth.value()).contains("ADMIN").contains("CATEGORY_DELETE");
    }

    @Test
    @DisplayName("UserAdminController, RoleAdminController, PermissionAdminController have no class-level ADMIN restriction")
    void verifyUserRolePermissionAdminControllers_NoClassLevelAdmin() {
        assertThat(UserAdminController.class.getAnnotation(PreAuthorize.class)).isNull();
        assertThat(RoleAdminController.class.getAnnotation(PreAuthorize.class)).isNull();
        assertThat(PermissionAdminController.class.getAnnotation(PreAuthorize.class)).isNull();
    }

    @Test
    @DisplayName("UserInternalController is secured with INTERNAL_SERVICE authority")
    void verifyUserInternalController_SecuredWithInternalService() {
        Method method = Arrays.stream(UserInternalController.class.getMethods())
                .filter(m -> m.getName().equals("getUserPermissions"))
                .findFirst().orElseThrow();
        PreAuthorize auth = method.getAnnotation(PreAuthorize.class);
        assertThat(auth).isNotNull();
        assertThat(auth.value()).contains("INTERNAL_SERVICE");
    }
}
