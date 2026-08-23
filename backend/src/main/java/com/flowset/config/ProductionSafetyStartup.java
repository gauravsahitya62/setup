package com.flowset.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProductionSafetyStartup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionSafetyStartup.class);

    private final AppEnvironmentProperties app;
    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;
    private final CookieProperties cookieProperties;
    private final StorageProperties storageProperties;
    private final Environment environment;

    public ProductionSafetyStartup(
            AppEnvironmentProperties app,
            JwtProperties jwtProperties,
            CorsProperties corsProperties,
            CookieProperties cookieProperties,
            StorageProperties storageProperties,
            Environment environment
    ) {
        this.app = app;
        this.jwtProperties = jwtProperties;
        this.corsProperties = corsProperties;
        this.cookieProperties = cookieProperties;
        this.storageProperties = storageProperties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String env = app.env() == null ? "development" : app.env().toLowerCase();
        boolean restricted = "production".equals(env) || "staging".equals(env);
        if (!restricted) {
            log.info("FlowSet starting env={}", env);
            return;
        }
        requireSecret(jwtProperties.accessSecret(), "JWT_ACCESS_SECRET");
        requireSecret(jwtProperties.refreshSecret(), "JWT_REFRESH_SECRET");
        String origins = corsProperties.allowedOrigins() == null ? "" : corsProperties.allowedOrigins().trim();
        if (origins.isEmpty() || "*".equals(origins)) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must be an explicit origin list in " + env);
        }
        if ("production".equals(env) && !cookieProperties.secure()) {
            throw new IllegalStateException("COOKIE_SECURE must be true in production");
        }
        if ("s3".equalsIgnoreCase(storageProperties.provider())
                && (isBlank(storageProperties.s3Bucket()) || isBlank(storageProperties.s3Region()))) {
            throw new IllegalStateException("AWS_S3_BUCKET and AWS_REGION are required when STORAGE_PROVIDER=s3");
        }
        if ("production".equals(env) && environment.getProperty("flowset.demo-seed.enabled", Boolean.class, false)) {
            throw new IllegalStateException("DEMO_SEED_ENABLED must be false in production");
        }
        if ("production".equals(env) && !"s3".equalsIgnoreCase(storageProperties.provider())) {
            log.warn("Production STORAGE_PROVIDER is not s3. Local disk is not suitable for a multi-instance pilot.");
        }
        log.info("FlowSet starting env={} corsOriginsConfigured=true cookieSecure={}", env, cookieProperties.secure());
    }

    private static void requireSecret(String value, String name) {
        if (value == null || value.length() < 32 || value.contains("change-me") || value.contains("dev-")) {
            throw new IllegalStateException(name + " must be a unique secret of at least 32 characters");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
