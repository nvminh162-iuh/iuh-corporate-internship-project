package com.hs.user.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;

class ApplicationEnvironmentConfigurationTest {

    @TempDir
    Path directory;

    @Test
    void loadsDotenvIncludingUtf8ValuesWithoutProfileOverrides() throws IOException {
        Properties values = exampleValues();
        values.setProperty("SPRING_PROFILES_ACTIVE", "prod");
        values.setProperty("JPA_DDL_AUTO", "update");
        values.setProperty("BOOTSTRAP_ADMIN_ENABLED", "true");
        values.setProperty("BOOTSTRAP_ADMIN_LAST_NAME", "Nguyễn");
        values.setProperty("SWAGGER_ENABLED", "true");

        MockEnvironment environment = loadConfiguration(writeDotenv(values), Map.of());

        assertThat(environment.getActiveProfiles()).containsExactly("prod");
        assertThat(environment.getRequiredProperty("server.port", Integer.class)).isEqualTo(55080);
        assertThat(environment.getRequiredProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(environment.getRequiredProperty("homespace.bootstrap.admin.enabled", Boolean.class)).isTrue();
        assertThat(environment.getRequiredProperty("homespace.bootstrap.admin.last-name")).isEqualTo("Nguyễn");
        assertThat(environment.getRequiredProperty("springdoc.swagger-ui.enabled", Boolean.class)).isTrue();
        assertThat(environment.getRequiredProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                .isEqualTo("http://localhost:59000/realms/telecare-platform");
        for (String property : applicationProperties().stringPropertyNames()) {
            environment.getRequiredProperty(property);
        }
    }

    @Test
    void runtimeEnvironmentOverridesDotenv() throws IOException {
        MockEnvironment environment = loadConfiguration(writeDotenv(exampleValues()), Map.of(
                "DATASOURCE_USERNAME", "runtime-database-user",
                "SERVER_PORT", "55100",
                "SWAGGER_ENABLED", "false"));

        assertThat(environment.getRequiredProperty("spring.datasource.username")).isEqualTo("runtime-database-user");
        assertThat(environment.getRequiredProperty("server.port", Integer.class)).isEqualTo(55100);
        assertThat(environment.getRequiredProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
    }

    @Test
    void containerCanUseInjectedVariablesWithoutDotenvFile() throws IOException {
        Map<String, Object> runtime = new LinkedHashMap<>();
        exampleValues().forEach((key, value) -> runtime.put(key.toString(), value));
        MockEnvironment environment = loadConfiguration(directory.resolve("absent.env"), runtime);

        for (String property : applicationProperties().stringPropertyNames()) {
            environment.getRequiredProperty(property);
        }
        assertThat(environment.getRequiredProperty("server.servlet.context-path")).isEqualTo("/api/v1");
    }

    @Test
    void exampleContainsEveryRequiredVariableAndProfileResourcesAreRemoved() throws IOException {
        assertThat(exampleValues().stringPropertyNames()).containsAll(requiredVariables(applicationProperties()).keySet());
        assertThat(new ClassPathResource("application-dev.properties").exists()).isFalse();
        assertThat(new ClassPathResource("application-prod.properties").exists()).isFalse();
    }

    @ParameterizedTest(name = "Missing {0} must fail when resolving {1}")
    @MethodSource("requiredEnvironmentVariables")
    void rejectsMissingEnvironmentVariable(String missingVariable, String property) throws IOException {
        Properties properties = applicationProperties();
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(new PropertiesPropertySource("application-config", properties));
        requiredVariables(properties).keySet().stream()
                .filter(variable -> !variable.equals(missingVariable))
                .forEach(variable -> environment.setProperty(variable, "configured-test-value"));

        assertThatThrownBy(() -> environment.getRequiredProperty(property))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(missingVariable);
    }

    private Path writeDotenv(Properties values) throws IOException {
        Path dotenv = directory.resolve(".env");
        try (Writer writer = Files.newBufferedWriter(dotenv, StandardCharsets.UTF_8)) {
            values.store(writer, "Synthetic test settings only");
        }
        return dotenv;
    }

    private MockEnvironment loadConfiguration(Path dotenv, Map<String, Object> runtime) throws IOException {
        Properties properties = applicationProperties();
        properties.setProperty("spring.config.import", "optional:" + dotenv.toUri() + "[.properties][encoding=utf-8]");
        Path configuration = directory.resolve("application.properties");
        try (Writer writer = Files.newBufferedWriter(configuration, StandardCharsets.UTF_8)) {
            properties.store(writer, "Isolated application configuration");
        }

        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("runtime-env", runtime));
        environment.setProperty("spring.config.location", configuration.toUri().toString());
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        return environment;
    }

    private static Properties applicationProperties() throws IOException {
        return PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
    }

    private static Properties exampleValues() throws IOException {
        Path example = Path.of(".env.example");
        if (!Files.exists(example)) {
            example = Path.of("telecare-backend", ".env.example");
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(example, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static Map<String, String> requiredVariables(Properties properties) {
        Pattern pattern = Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)(?::[^}]*)?}");
        Map<String, String> variables = new LinkedHashMap<>();
        properties.stringPropertyNames().stream().sorted().forEach(property -> {
            Matcher matcher = pattern.matcher(properties.getProperty(property));
            while (matcher.find()) {
                variables.putIfAbsent(matcher.group(1), property);
            }
        });
        return variables;
    }

    private static Stream<Arguments> requiredEnvironmentVariables() throws IOException {
        return requiredVariables(applicationProperties()).entrySet().stream()
                .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
    }
}
