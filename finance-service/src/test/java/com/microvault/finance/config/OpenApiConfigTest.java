package com.microvault.finance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    @Test
    void describesTheServiceAndRequiresABearerToken() {
        OpenAPI api = new OpenApiConfig().financeOpenApi();

        assertEquals("MicroVault Finance Service", api.getInfo().getTitle());
        assertEquals("1.0", api.getInfo().getVersion());
        assertTrue(api.getInfo().getDescription().contains("affordability"));
        assertEquals(OpenApiConfig.BEARER, api.getSecurity().get(0).keySet().iterator().next());

        SecurityScheme scheme = api.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER);
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
    }
}
