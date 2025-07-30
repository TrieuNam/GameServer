package org.SouthMillion.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.util.List;
import java.util.Map;

public class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static <T> List<T> fromJsonList(String json, Class<T> cls) {
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, cls));
        } catch (Exception e) {
            return null;
        }
    }
    public static <T> String toJson(List<T> list) {
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    // Parse từ json String sang Map<Integer, Object> (hoặc Map<Integer, T> nếu dùng generic)
    public static <K, V> Map<K, V> fromJsonToMap(String json, Class<K> keyClass, Class<V> valueClass) {
        try {
            // TypeReference giúp parse map có key kiểu Integer và value kiểu Object
            return MAPPER.readValue(json, new TypeReference<Map<K, V>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse map from JSON", e);
        }
    }


    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON encode error", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON decode error", e);
        }
    }
}
