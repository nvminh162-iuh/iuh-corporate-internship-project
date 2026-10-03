package com.hs.user.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import com.hs.user.config.security.CustomJwtAuthenticationConverter;
import com.hs.user.model.User;
import com.hs.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomJwtAuthenticationConverterTest {

    @Mock
    private UserRepository userRepository;

    private CustomJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CustomJwtAuthenticationConverter(userRepository);
    }

    @Test
    void convert_whenLocalUserDoesNotExist_UsesTokenRoleFallback() {
        when(userRepository.findById("external-user")).thenReturn(Optional.empty());

        var authentication = converter.convert(jwtWithAdminRole("external-user"));

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ADMIN", "ROLE_ADMIN");
    }

    @Test
    void convert_whenLocalLookupFails_DoesNotGrantTokenRoleFallback() {
        when(userRepository.findById("external-user")).thenThrow(new IllegalStateException("database unavailable"));

        var authentication = converter.convert(jwtWithAdminRole("external-user"));

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    @Test
    void convert_whenLocalUserIsInactive_DoesNotGrantTokenRoleFallback() {
        User inactiveUser = new User();
        inactiveUser.setId("external-user");
        inactiveUser.setActive(false);
        when(userRepository.findById("external-user")).thenReturn(Optional.of(inactiveUser));

        var authentication = converter.convert(jwtWithAdminRole("external-user"));

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    private Jwt jwtWithAdminRole(String subject) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .claim("realm_access", Map.of("roles", List.of("admin")))
                .build();
    }
}
