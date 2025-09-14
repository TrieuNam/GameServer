package com.SouthMillion.role_service.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtils {
    private static final ObjectMapper OM = new ObjectMapper();
    private JsonUtils(){}

    public static String toJson(Object o) {
        try {
            return OM.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON serialize failed: " + e.getMessage(), e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return OM.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON parse failed: " + e.getMessage(), e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return OM.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON parse failed: " + e.getMessage(), e);
        }
    }
}