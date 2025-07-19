package com.SouthMillion.user_service.utils;

public class AdVerifier {
    public static boolean verify(String adProvider, String adToken) {
        // Với AdMob/UnityAds thực: call REST API verify adToken (S2S), demo ở đây
        return adToken != null && adToken.length() > 10;
    }
}
