package com.SouthMillion.role_service.utils;

import java.security.SecureRandom;

/** ULID 26 chars (Crockford Base32). Đủ dùng cho server đơn. */
public final class IdGen {
    private static final SecureRandom RND = new SecureRandom();
    private static final char[] ENC = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private IdGen() {}

    public static String ulid() {
        long time = System.currentTimeMillis();
        byte[] randomness = new byte[10];
        RND.nextBytes(randomness);
        char[] out = new char[26];

        // 48-bit time -> 10 chars
        long t = time;
        for (int i = 9; i >= 0; i--) {
            out[i] = ENC[(int)(t & 31)];
            t >>>= 5;
        }
        // 80-bit randomness -> 16 chars
        int idx = 10;
        int v = 0, bits = 0;
        for (int i = 0; i < randomness.length; i++) {
            v = (v << 8) | (randomness[i] & 0xFF);
            bits += 8;
            while (bits >= 5) {
                out[idx++] = ENC[(v >>> (bits - 5)) & 31];
                bits -= 5;
            }
        }
        if (bits > 0) out[idx++] = ENC[(v << (5 - bits)) & 31];

        // pad nếu thiếu (cực hiếm)
        while (idx < 26) out[idx++] = '0';
        return new String(out, 0, 26);
    }
}