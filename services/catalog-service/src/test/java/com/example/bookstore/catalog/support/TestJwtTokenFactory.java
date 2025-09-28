package com.example.bookstore.catalog.support;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TestJwtTokenFactory {

    private static final String ISSUER = "https://bookstore.example.test";

    private final JwtEncoder jwtEncoder;

    public TestJwtTokenFactory(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String createAdminToken() {
        return createToken("admin", List.of("ADMIN"));
    }

    public String createStaffToken() {
        return createToken("staff", List.of("STAFF"));
    }

    public String createToken(String subject, Collection<String> roles) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(subject)
                .claim("preferred_username", subject)
                .claim("realm_access", Map.of("roles", roles))
                .build();

        JwtEncoderParameters parameters = JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("test-key").build(),
                claims);
        return jwtEncoder.encode(parameters).getTokenValue();
    }
}
