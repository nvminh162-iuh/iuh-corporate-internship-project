package com.hs.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class ProductionProfileConfigurationTest {

    @Test
    @DisplayName("Production profile: swagger must be disabled and ddl-auto must be validate")
    void prodProfile_disablesSwaggerAndEnforcesValidation() throws IOException {
        Properties prodProps = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-prod.properties"));

        assertThat(prodProps.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(prodProps.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
        assertThat(prodProps.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(prodProps.getProperty("homespace.bootstrap.admin.enabled")).isEqualTo("false");
        assertThat(prodProps.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
        assertThat(prodProps.getProperty("spring.datasource.hikari.maximum-pool-size")).isEqualTo("20");
    }

    @Test
    @DisplayName("Development profile: swagger must be enabled and ddl-auto must be update")
    void devProfile_enablesSwaggerAndAllowsUpdate() throws IOException {
        Properties devProps = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-dev.properties"));

        assertThat(devProps.getProperty("springdoc.api-docs.enabled")).isEqualTo("true");
        assertThat(devProps.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("true");
        assertThat(devProps.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
    }
}
