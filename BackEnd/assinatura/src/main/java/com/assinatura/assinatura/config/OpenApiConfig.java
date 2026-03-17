package com.assinatura.assinatura.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI assinaturaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Assinatura API")
                        .description("API para assinatura digital e verificacao de assinaturas")
                        .version("v1"));
    }
}