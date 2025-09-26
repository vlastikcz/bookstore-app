package com.example.bookstore.catalog.application;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class StrongEtagGenerator {

    private static final String QUOTE = "\"";

    public String generate(UUID resourceId, long version) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(resourceId.toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(Long.toString(version).getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(digest.digest());
            return QUOTE + hex + QUOTE;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
