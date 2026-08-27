package com.hs.user.config.database;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.hs.user.filter.UserContextHolder;

import java.util.Optional;

@Configuration
public class JpaAuditing {

    @Bean
    public AuditorAware<String> auditorAware() {
        return this::resolveCurrentUserId;
    }

    private Optional<String> resolveCurrentUserId() {
        Optional<String> fromContext = Optional.ofNullable(UserContextHolder.get())
                .map(UserContextHolder::getUserId)
                .filter(this::hasText);

        if (fromContext.isPresent()) {
            return fromContext;
        }

        return getAuthenticationUserId();
    }

    private Optional<String> getAuthenticationUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof String principalValue && hasText(principalValue)) {
            return Optional.of(principalValue);
        }
        if (principal instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            if (hasText(subject)) {
                return Optional.of(subject);
            }
        }

        String name = authentication.getName();
        return hasText(name) ? Optional.of(name) : Optional.empty();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
