package com.example.bookstore.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class CatalogServiceConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
