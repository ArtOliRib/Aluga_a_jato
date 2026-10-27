package org.arthur.genericapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aluga A Jato API")
                        .description("API de locação de carros com proteção de dados pessoais (AES-256-GCM + índices HMAC).")
                        .version("v1")
                        .contact(new Contact().name("Aluga A Jato"))
                        .license(new License().name("MIT")));
    }
}
