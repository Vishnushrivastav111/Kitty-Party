package com.microvault.finance.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebConfigTest {

    @Test
    void apiRoutesAcceptTheFinanceMethodsFromAnyOrigin() {
        VisibleCorsRegistry registry = new VisibleCorsRegistry();

        new WebConfig().addCorsMappings(registry);

        var configuration = registry.mappings().get("/api/**");
        assertNotNull(configuration);
        assertTrue(configuration.getAllowedOriginPatterns().contains("*"));
        assertTrue(configuration.getAllowedMethods().contains("GET"));
        assertTrue(configuration.getAllowedMethods().contains("POST"));
        assertTrue(configuration.getAllowedMethods().contains("PUT"));
        assertTrue(configuration.getAllowedMethods().contains("DELETE"));
        assertTrue(configuration.getAllowedMethods().contains("OPTIONS"));
        assertEquals("*", configuration.getAllowedHeaders().get(0));
    }

    private static final class VisibleCorsRegistry extends CorsRegistry {
        private Map<String, org.springframework.web.cors.CorsConfiguration> mappings() {
            return getCorsConfigurations();
        }
    }
}
