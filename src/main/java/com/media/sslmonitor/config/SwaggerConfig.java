package com.media.sslmonitor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Domain SSL Monitor API")
                        .version("1.0")
                        .description("API for monitoring SSL certificate expiry")
                        .contact(new Contact()
                                .name("Media")
                                .email("support@media.com")));
    }
}