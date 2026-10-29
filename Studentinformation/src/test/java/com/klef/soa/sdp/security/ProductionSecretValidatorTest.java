package com.klef.soa.sdp.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecretValidatorTest {
    @Test
    void productionRequiresAUniqueSigningKeyOfMinimumLength() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ProductionSecretValidator validator = new ProductionSecretValidator(environment);
        assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
        environment.setProperty("JWT_SECRET", "a-different-production-key-with-more-than-32-characters");
        assertDoesNotThrow(validator::afterPropertiesSet);
    }

    @Test
    void renderRuntimeRequiresProductionKeyEvenWithoutAnActiveProfile() {
        MockEnvironment environment = new MockEnvironment().withProperty("RENDER", "true");
        assertThrows(IllegalStateException.class, () -> new ProductionSecretValidator(environment).afterPropertiesSet());
    }
}
