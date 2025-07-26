package org.SouthMillion.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.item.GemInlayDTO;

import java.util.List;

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

    public static List<GemInlayDTO> parseGemList(String gemListJson) {
        if (gemListJson == null || gemListJson.isEmpty()) return List.of();
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(gemListJson, new TypeReference<List<GemInlayDTO>>() {});
        } catch (Exception e) {
            // Log lỗi nếu cần
            return List.of();
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
