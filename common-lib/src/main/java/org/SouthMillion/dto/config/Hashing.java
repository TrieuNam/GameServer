package org.SouthMillion.dto.config;

import java.security.MessageDigest; import java.util.HexFormat;
public final class Hashing {
    private Hashing(){}
    public static String sha1(byte[]... arr) {
        try {
            var md = MessageDigest.getInstance("SHA-1");
            for (var a: arr) md.update(a);
            return "\""+HexFormat.of().formatHex(md.digest())+"\"";
        } catch(Exception e){ return "\"na\""; }
    }
}