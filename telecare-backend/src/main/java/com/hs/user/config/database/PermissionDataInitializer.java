package com.hs.user.config.database;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hs.user.constant.PermissionConstants;
import com.hs.user.constant.RoleConstants;
import com.hs.user.model.Permission;
import com.hs.user.model.Role;
import com.hs.user.repository.PermissionRepository;
import com.hs.user.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(2)
@RequiredArgsConstructor
public class PermissionDataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, String> permissions = Map.ofEntries(
                Map.entry(PermissionConstants.Admin.USER_VIEW, "Xem danh sách người dùng"),
                Map.entry(PermissionConstants.Admin.USER_CREATE, "Tạo người dùng"),
                Map.entry(PermissionConstants.Admin.USER_UPDATE, "Cập nhật người dùng"),
                Map.entry(PermissionConstants.Admin.ROLE_VIEW, "Xem danh sách chức vụ"),
                Map.entry(PermissionConstants.Admin.ROLE_CREATE, "Tạo chức vụ"),
                Map.entry(PermissionConstants.Admin.ROLE_UPDATE, "Cập nhật chức vụ"),
                Map.entry(PermissionConstants.Admin.ROLE_DELETE, "Xóa chức vụ"),
                Map.entry(PermissionConstants.Admin.PERMISSION_VIEW, "Xem danh sách quyền"),
                Map.entry(PermissionConstants.Admin.PERMISSION_CREATE, "Tạo quyền"),
                Map.entry(PermissionConstants.Admin.PERMISSION_UPDATE, "Cập nhật quyền"),
                Map.entry(PermissionConstants.Admin.PERMISSION_DELETE, "Xóa quyền"));

        permissions.forEach((name, description) -> {
            if (!permissionRepository.existsByName(name)) {
                permissionRepository.save(Permission.builder()
                        .name(name)
                        .description(description)
                        .build());
            }
        });

        assignAllPermissionsToAdmin(permissions.keySet());
    }

    private void assignAllPermissionsToAdmin(Set<String> permissionNames) {
        Role adminRole = roleRepository
                .findByName(RoleConstants.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Missing default role: " + RoleConstants.ADMIN));

        Set<Permission> adminPermissions = permissionRepository
                .findAll()
                .stream()
                .filter(permission -> permissionNames.contains(permission.getName()))
                .collect(Collectors.toSet());

        if (adminPermissions.size() != permissionNames.size()) {
            throw new IllegalStateException("Missing seeded permissions for admin role");
        }

        Set<Permission> currentPermissions = adminRole.getPermissions() == null
                ? new HashSet<>()
                : new HashSet<>(adminRole.getPermissions());

        currentPermissions.addAll(adminPermissions);
        adminRole.setPermissions(currentPermissions);
        roleRepository.save(adminRole);
    }
}
