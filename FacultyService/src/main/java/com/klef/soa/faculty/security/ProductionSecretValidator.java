package com.klef.soa.faculty.security;

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
        String jwt = environment.getProperty("JWT_SECRET", "");
        String mfa = environment.getProperty("MFA_ENCRYPTION_KEY", "");
        if (jwt.length() < 32 || jwt.chars().distinct().count() < 16 || DEV_SECRET.equals(jwt)) {
            throw new IllegalStateException("Production requires a unique JWT_SECRET of at least 32 characters");
        }
        if (mfa.length() < 32 || mfa.chars().distinct().count() < 16 || mfa.equals(jwt)) {
            throw new IllegalStateException("Production requires a separate stable MFA_ENCRYPTION_KEY of at least 32 characters");
        }
    }
}
