package com.example.bookstore.catalog.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    KeyPair testKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Failed to generate RSA key pair for tests", ex);
        }
    }

    @Bean
    @Primary
    JwtDecoder testJwtDecoder(KeyPair testKeyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) testKeyPair.getPublic();
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    @Primary
    JwtEncoder testJwtEncoder(KeyPair testKeyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) testKeyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) testKeyPair.getPrivate();
        RSAKey jwk = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("test-key")
                .build();
        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }
}
