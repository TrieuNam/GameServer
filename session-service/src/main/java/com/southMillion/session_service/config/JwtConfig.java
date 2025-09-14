package com.southMillion.session_service.config;


import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.secret}")
    private String secret;

    // ===== Beans dùng chung =====

    /** Bytes của secret (hỗ trợ HEX / Base64URL / Base64 / UTF-8) */
    @Bean("jwtSecretBytes")
    public byte[] jwtSecretBytes() {
        byte[] key = resolveSecret(secret);
        if (key.length < 32) {
            log.warn("⚠️ JWT secret length < 32 bytes. Nên dùng secret ≥ 32 bytes cho HS256.");
        } else {
            log.info("JWT HS256 key length: {} bytes", key.length);
        }
        return key;
    }

    /** Signer cho HS256 */
    @Bean
    public MACSigner macSigner(@Qualifier("jwtSecretBytes") byte[] key) throws KeyLengthException {
        return new MACSigner(key); // ném KeyLengthException nếu < 256-bit
    }

    /** Verifier cho HS256 */
    @Bean
    public MACVerifier macVerifier(@Qualifier("jwtSecretBytes") byte[] key) throws JOSEException {
        return new MACVerifier(key);
    }

    /** Issuer dùng để kiểm tra/điền claim iss */
    @Bean("jwtIssuer")
    public String jwtIssuer() {
        return issuer;
    }

    // ===== Helper: parse secret linh hoạt =====

    private static final Pattern HEX = Pattern.compile("^[0-9a-fA-F]{32,}$"); // >=16 bytes (hex)
    private static final Pattern B64URL = Pattern.compile("^[A-Za-z0-9_\\-]+$");

    static byte[] resolveSecret(String raw) {
        if (raw == null) return new byte[0];
        String s = raw.trim();

        // HEX?
        if (HEX.matcher(s).matches() && (s.length() % 2 == 0)) {
            return hexToBytes(s);
        }
        // Base64URL?
        if (B64URL.matcher(s).matches()) {
            try { return Base64.getUrlDecoder().decode(s); } catch (IllegalArgumentException ignore) {}
        }
        // Base64 chuẩn?
        try { return Base64.getDecoder().decode(s); } catch (IllegalArgumentException ignore) {}

        // Fallback: UTF-8
        return s.getBytes(StandardCharsets.UTF_8);
    }

    static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i/2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    +  Character.digit(hex.charAt(i+1), 16));
        }
        return out;
    }
}