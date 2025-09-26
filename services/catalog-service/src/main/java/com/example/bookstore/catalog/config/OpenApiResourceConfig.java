package com.example.bookstore.catalog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OpenApiResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/openapi/**")
                .addResourceLocations("classpath:/openapi/")
                .setCachePeriod(0);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/swagger-ui/")
                .setViewName("forward:/webjars/swagger-ui/index.html");
        registry.addViewController("/swagger-ui/index.html")
                .setViewName("forward:/webjars/swagger-ui/index.html");
    }
}
