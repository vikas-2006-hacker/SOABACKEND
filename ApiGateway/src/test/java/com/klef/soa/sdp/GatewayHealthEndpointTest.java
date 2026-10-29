package com.klef.soa.sdp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.gateway.discovery.locator.enabled=false",
        "jwt.secret=TestOnlyGatewayJwtSecretKeyNotForProduction2026"
})
@AutoConfigureWebTestClient
class GatewayHealthEndpointTest {
    @Autowired WebTestClient webTestClient;

    @Test
    void actuatorHealthIsPublicAndRedacted() {
        webTestClient.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.components").doesNotExist();
    }
}
