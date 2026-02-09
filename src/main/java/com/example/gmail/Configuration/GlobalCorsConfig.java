package com.example.gmail.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class GlobalCorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // allow all endpoints
                        .allowedOrigins("https://zam-gmailtemp.vercel.app") // your frontend URL
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // all methods
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
