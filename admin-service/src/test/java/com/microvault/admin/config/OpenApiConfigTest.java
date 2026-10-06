package com.microvault.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiConfigTest {

    @Test
    void describesTheServiceAndRequiresABearerToken() {
        OpenAPI api = new OpenApiConfig().adminOpenApi();

        assertEquals("MicroVault Admin Service", api.getInfo().getTitle());
        assertEquals("1.0", api.getInfo().getVersion());
        assertEquals(OpenApiConfig.BEARER, api.getSecurity().get(0).keySet().iterator().next());

        SecurityScheme scheme = api.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER);
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
    }
}
