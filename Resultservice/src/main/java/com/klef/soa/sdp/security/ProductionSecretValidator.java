package com.klef.soa.sdp.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class ProductionSecretValidator implements InitializingBean {
    private static final String DEV_SECRET = "campuscore-local-development-signing-key-not-for-production";
    private final Environment environment;

    public ProductionSecretValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (!environment.acceptsProfiles(Profiles.of("prod", "production"))
                && !Boolean.parseBoolean(environment.getProperty("RENDER", "false"))) return;
        String secret = environment.getProperty("JWT_SECRET", "");
        if (secret.length() < 32 || secret.chars().distinct().count() < 16 || DEV_SECRET.equals(secret)) {
            throw new IllegalStateException("Production requires a unique JWT_SECRET of at least 32 characters");
        }
    }
}
