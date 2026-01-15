package com.aide.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aideOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aide API")
                        .description("AI-powered documentation engine API")
                        .version("v1"));
    }
}