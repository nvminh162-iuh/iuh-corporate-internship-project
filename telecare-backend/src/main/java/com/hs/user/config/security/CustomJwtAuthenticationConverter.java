package com.hs.user.config.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.hs.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String userId = jwt.getSubject();
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Resolve Role and Permissions directly from local PostgreSQL database
        try {
            userRepository.findById(userId).ifPresent(user -> {
                if (user.getRole() != null && Boolean.TRUE.equals(user.getRole().getActive())) {
                    String roleName = normalizeRole(user.getRole().getName());
                    if (!roleName.isBlank()) {
                        authorities.add(new SimpleGrantedAuthority(roleName));
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                    }
                    if (user.getRole().getPermissions() != null) {
                        user.getRole().getPermissions().stream()
                                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                                .map(p -> new SimpleGrantedAuthority(p.getName()))
                                .forEach(authorities::add);
                    }
                }
            });
        } catch (Exception e) {
            log.warn("Could not load user permissions from database for userId {}: {}", userId, e.getMessage());
        }

        // 2. Extract roles from Keycloak JWT claims (realm_access & resource_access) as fallback
        extractKeycloakRoles(jwt).forEach(role -> {
            String normalized = normalizeRole(role);
            if (!normalized.isBlank()) {
                authorities.add(new SimpleGrantedAuthority(normalized));
                authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
            }
        });

        String principalClaimValue = jwt.getClaimAsString("preferred_username");
        if (principalClaimValue == null || principalClaimValue.isBlank()) {
            principalClaimValue = jwt.getClaimAsString("email");
        }
        if (principalClaimValue == null || principalClaimValue.isBlank()) {
            principalClaimValue = userId;
        }

        return new JwtAuthenticationToken(jwt, authorities, principalClaimValue);
    }

    private Set<String> extractKeycloakRoles(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roleList) {
            roleList.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .forEach(roles::add);
        }

        Object resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess instanceof Map<?, ?> clients) {
            for (Object clientAccess : clients.values()) {
                if (clientAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roleList) {
                    roleList.stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .forEach(roles::add);
                }
            }
        }

        return roles;
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.replaceFirst("^ROLE_", "").toUpperCase();
    }
}
