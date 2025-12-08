package com.example.camunda.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC Configuration for serving embedded forms from the classpath.
 * 
 * This configuration maps the /forms/ URL path to the classpath resources/forms/ directory,
 * allowing Camunda embedded forms to be properly resolved and served.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /forms/** URLs to classpath:forms/ resources
        registry.addResourceHandler("/forms/**")
                .addResourceLocations("classpath:/forms/")
                .setCachePeriod(3600); // Cache for 1 hour
    }
}

