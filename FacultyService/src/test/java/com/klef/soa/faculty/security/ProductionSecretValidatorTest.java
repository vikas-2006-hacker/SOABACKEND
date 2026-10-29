package com.klef.soa.faculty.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecretValidatorTest {
    @Test
    void productionRequiresSeparateJwtAndMfaKeys() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.withProperty("JWT_SECRET", "production-jwt-signing-key-with-more-than-32-characters")
                .withProperty("MFA_ENCRYPTION_KEY", "production-mfa-encryption-key-with-more-than-32-characters");
        ProductionSecretValidator validator = new ProductionSecretValidator(environment);
        assertDoesNotThrow(validator::afterPropertiesSet);
        environment.setProperty("MFA_ENCRYPTION_KEY", environment.getProperty("JWT_SECRET"));
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
        environment.setProperty("MFA_ENCRYPTION_KEY", "short");
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
    }
}
