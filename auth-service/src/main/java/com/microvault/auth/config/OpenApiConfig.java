package com.microvault.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI authOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MicroVault Auth Service")
                        .version("1.0")
                        .description("Registration, login, OTP password reset and user accounts."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(jwtComponents());
    }

    static Components jwtComponents() {
        return new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT from POST /api/auth/login. Paste the token only; Swagger adds Bearer."));
    }
}
