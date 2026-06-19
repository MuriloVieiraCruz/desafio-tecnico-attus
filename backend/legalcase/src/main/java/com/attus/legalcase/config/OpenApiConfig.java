package com.attus.legalcase.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI legalCaseOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Legal Case API")
                .description("Legal case management for digital attorney services (Attus technical test)")
                .version("v1"));
    }
}
