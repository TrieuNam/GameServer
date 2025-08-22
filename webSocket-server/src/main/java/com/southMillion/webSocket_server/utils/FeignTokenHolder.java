package com.southMillion.webSocket_server.utils;

public final class FeignTokenHolder {
    private static final ThreadLocal<String> TL = new ThreadLocal<>();

    private FeignTokenHolder() {}

    public static void set(String token) { TL.set(token); }
    public static String get() { return TL.get(); }
    public static void clear() { TL.remove(); }
}