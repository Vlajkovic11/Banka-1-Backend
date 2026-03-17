package com.banka1.clientService.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguracija Swagger/OpenAPI dokumentacije za client-service.
 * URL servera se ucitava iz konfiguracije kako bi bio zamenljiv po okruzenju
 * (lokalno, Docker, staging, produkcija).
 */
@Configuration
public class SwaggerConfig {

    /** URL servera koji se prikazuje u generisanoj OpenAPI specifikaciji. */
    @Value("${springdoc.server-url}")
    private String serverUrl;

    /**
     * Kreira OpenAPI konfiguraciju sa JWT Bearer autentifikacijom i konfigurisanim URL serverom.
     *
     * @return konfigurisani OpenAPI objekat
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url(serverUrl))
                .info(new Info()
                        .title("Client Service API")
                        .description("Servis za upravljanje klijentima banke. Dostupan samo zaposlenima.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuthentication"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuthentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
