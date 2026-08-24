package com.flowset.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Hosts such as Render and Railway provide {@code postgres://} URLs. Spring needs JDBC form plus
 * split username/password. Also prefixes {@code https://} on host-only CORS/frontend values.
 */
public class JdbcUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> map = new HashMap<>();
        normalizeJdbc(environment, map);
        prefixHttps(environment, map, "CORS_ALLOWED_ORIGINS", "flowset.cors.allowed-origins");
        prefixHttps(environment, map, "FRONTEND_URL", "flowset.app.frontend-url");
        String storageHost = firstNonBlank(environment, "STORAGE_PUBLIC_HOST");
        if (storageHost != null && !storageHost.contains("://")) {
            map.put("flowset.storage.public-base-url", "https://" + storageHost + "/api/v1/files");
        }
        if (!map.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("flowset-paas-normalize", map));
        }
    }

    private static void normalizeJdbc(ConfigurableEnvironment environment, Map<String, Object> map) {
        String raw = firstNonBlank(environment, "DATABASE_URL", "SPRING_DATASOURCE_URL");
        if (raw == null || raw.startsWith("jdbc:") || !raw.startsWith("postgres")) {
            return;
        }
        URI uri = URI.create(raw.replaceFirst("^postgres(ql)?:", "http:"));
        String user = "";
        String password = "";
        if (uri.getUserInfo() != null) {
            String[] parts = uri.getUserInfo().split(":", 2);
            user = decode(parts[0]);
            password = parts.length > 1 ? decode(parts[1]) : "";
        }
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String path = uri.getPath() == null || uri.getPath().isBlank() ? "/flowset" : uri.getPath();
        String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + port + path;
        if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
            jdbc += "?" + uri.getQuery();
        }
        map.put("spring.datasource.url", jdbc);
        if (!user.isBlank()) {
            map.put("spring.datasource.username", user);
        }
        if (!password.isBlank()) {
            map.put("spring.datasource.password", password);
        }
    }

    private static void prefixHttps(
            ConfigurableEnvironment environment, Map<String, Object> map, String envKey, String property) {
        String value = firstNonBlank(environment, envKey);
        if (value != null && !value.contains("://") && !"*".equals(value)) {
            map.put(property, "https://" + value);
        }
    }

    private static String firstNonBlank(ConfigurableEnvironment environment, String... keys) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
