package com.southMillion.session_service.config;


import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jwt.JwtDecoder; // sync decoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder; // reactive decoder
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;


@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.issuer}")
    private String issuer;

    @Value("${security.jwt.access-ttl-sec}")
    private long accessTtlSec;

    @Value("${security.jwt.refresh-ttl-sec}")
    private long refreshTtlSec;

    @PostConstruct
    void showConfig() {
        log.info("JWT issuer = {}", issuer);
        log.info("JWT accessTtlSec = {}s, refreshTtlSec = {}s", accessTtlSec, refreshTtlSec);
    }

    /** SecretKey dùng cho HS256. Hỗ trợ cả chuỗi Base64 và plain text. */
    @Bean
    public SecretKey jwtSecretKey() {
        byte[] keyBytes = tryBase64(secret);
        if (keyBytes == null) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            log.warn("⚠️ JWT secret length < 32 bytes. Nên dùng secret ≥ 32 bytes để HS256 an toàn hơn.");
        }
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /** ENCODER – ký token với HS256 bằng secret key */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    /** DECODER (reactive) – cho WebFlux Resource Server */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(SecretKey key) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    /** DECODER (sync) – dùng trong Service */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey key) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    private static byte[] tryBase64(String s) {
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}