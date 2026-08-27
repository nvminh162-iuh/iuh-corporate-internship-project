package com.hs.user.filter;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;

@Component
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String userId = null;
        String email = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            userId = jwtAuth.getToken().getSubject();
            email = jwtAuth.getToken().getClaimAsString("email");
        }

        // Fallback for header-based testing or legacy requests if token not present
        if (!hasText(userId)) {
            userId = request.getHeader("X-User-Id");
        }
        if (!hasText(email)) {
            email = request.getHeader("X-User-Email");
        }

        if (hasText(userId) || hasText(email)) {
            UserContextHolder context = UserContextHolder.builder()
                    .userId(userId)
                    .email(email)
                    .build();
            UserContextHolder.set(context);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
