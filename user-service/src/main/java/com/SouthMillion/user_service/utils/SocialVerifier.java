package com.SouthMillion.user_service.utils;

import org.SouthMillion.dto.user.GoogleProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class SocialVerifier {

    @Value("${oauth2.google.client-id}")
    private String googleClientId;


    /**
     * Xác thực id_token của Google, trả về Google user id nếu hợp lệ
     */
    public GoogleProfile  verifyGoogle(String idToken) {
        String url = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + idToken;
        RestTemplate rest = new RestTemplate();
        Map<String, Object> resp = rest.getForObject(url, Map.class);
        if (resp == null || !resp.containsKey("sub")) {
            throw new RuntimeException("Invalid Google id_token");
        }
        // Check đúng client_id (aud)
        if (!googleClientId.equals(resp.get("aud"))) {
            throw new RuntimeException("Google token audience (aud) mismatch!");
        }

        // Map các field từ response
        GoogleProfile profile = new GoogleProfile();
        profile.setSub((String) resp.get("sub"));
        profile.setEmail((String) resp.get("email"));
        profile.setName((String) resp.get("name"));
        profile.setPicture((String) resp.get("picture"));
        profile.setLocale((String) resp.get("locale"));
        profile.setAud((String) resp.get("aud"));

        return profile;
    }

    /**
     * Xác thực access_token của Facebook, trả về Facebook user id nếu hợp lệ
     */
//    public String verifyFacebook(String accessToken) {
//        // Optional: verify access_token is valid for your app
//        String debugUrl = String.format(
//                "https://graph.facebook.com/debug_token?input_token=%s&access_token=%s|%s",
//                accessToken, facebookAppId, facebookAppSecret);
//        RestTemplate rest = new RestTemplate();
//        Map debugResp = rest.getForObject(debugUrl, Map.class);
//        if (debugResp == null || debugResp.get("data") == null || !(Boolean) ((Map)debugResp.get("data")).get("is_valid")) {
//            throw new RuntimeException("Invalid Facebook access_token");
//        }
//        // Lấy user id thật
//        String url = "https://graph.facebook.com/me?fields=id,email&access_token=" + accessToken;
//        Map resp = rest.getForObject(url, Map.class);
//        if (resp == null || !resp.containsKey("id")) {
//            throw new RuntimeException("Cannot get Facebook user id");
//        }
//        return (String) resp.get("id");
//    }

    /**
     * Xác thực token social bất kỳ, trả về user id social nếu hợp lệ
     */
    public Object verify(String provider, String token) {
        switch (provider) {
            case "google":
                return verifyGoogle(token);
//            case "facebook":
//                return verifyFacebook(token);
            default:
                throw new RuntimeException("Unknown social provider: " + provider);
        }
    }
}
