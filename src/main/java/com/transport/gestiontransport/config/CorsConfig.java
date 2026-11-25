package com.transport.gestiontransport.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    private final String[] originesAutorisees;

    public CorsConfig(@Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}")
                      String origines) {
        originesAutorisees = Arrays.stream(origines.split(","))
                .map(String::trim)
                .filter(origine -> !origine.isEmpty())
                .toArray(String[]::new);
        if (originesAutorisees.length == 0 || Arrays.stream(originesAutorisees)
                .anyMatch(origine -> origine.contains("*"))) {
            throw new IllegalArgumentException("Configurer au moins une origine CORS explicite.");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(originesAutorisees)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
