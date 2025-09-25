package com.example.bookstore.catalog.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@Profile("local")
public class LocalSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(LocalSecurityConfig.class);
    private static final String ISSUER = "local-bookstore";
    private static final String SHARED_SECRET = "local-development-secret-key-32-bytes!!";
    private static final Duration TOKEN_TTL = Duration.ofHours(12);

    @Bean
    SecretKey localJwtSecretKey() {
        return new SecretKeySpec(SHARED_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder localJwtDecoder(SecretKey localJwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(localJwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        return decoder;
    }

    @Bean
    JwtEncoder localJwtEncoder(SecretKey localJwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(localJwtSecretKey));
    }

    @Bean
    CommandLineRunner localJwtSampleLogger(JwtEncoder localJwtEncoder) {
        return args -> {
            logSampleToken(localJwtEncoder, "admin", List.of("ADMIN"));
            logSampleToken(localJwtEncoder, "staff", List.of("STAFF"));
        };
    }

    private void logSampleToken(JwtEncoder encoder, String subject, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_TTL))
                .subject(subject)
                .claim("preferred_username", subject)
                .claim("realm_access", Map.of("roles", roles))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        logger.info("[local] Generated {} token with roles {}", subject, roles);
        logger.info("[local] Authorization: Bearer {}", token);
    }
}
