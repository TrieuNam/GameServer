package com.SouthMillion.webSocket_server.handler;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.client.LocalizationFeign;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * WebSocket handler for localization/i18n
 * Message ID: 9200
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalizationHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LocalizationHandler.class);
    
    private final LocalizationFeign localizationFeign;
    private final ObjectMapper objectMapper;
    
    private static final int OP_TRANSLATE = 1;
    private static final int OP_GET_ALL = 2;
    
    @Override
    public int[] interests() {
        return new int[]{9200};
    }
    
    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
        log.debug("Handling localization message, msgId={}, roleId={}", msgId, ps.getRoleId());
        
        try {
            int operation = payload.length > 0 ? payload[0] : OP_GET_ALL;
            
            switch (operation) {
                case OP_TRANSLATE:
                    handleTranslate(ps, payload);
                    break;
                case OP_GET_ALL:
                    handleGetAll(ps, payload);
                    break;
                default:
                    log.warn("Unknown operation: {}", operation);
            }
        } catch (Exception e) {
            log.error("Error handling localization message", e);
        }
        });
    }
    
    private void handleTranslate(PlayerSession ps, byte[] payload) {
        // payload format: [1 byte op][UTF-8 string "key|lang"]
        // e.g. bytes: [1, 'h','e','l','l','o','|','z','h']
        if (payload.length < 2) {
            log.warn("[localization] Translate: payload too short ({})", payload.length);
            return;
        }
        String data = new String(payload, 1, payload.length - 1, StandardCharsets.UTF_8).trim();
        String[] parts = data.split("\\|", 2);
        String key = parts[0];
        String lang = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : "en";

        Map<String, Object> result = localizationFeign.translate(key, lang);
        log.debug("Translated key '{}' to lang '{}'", key, lang);
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(result);
            Emitters.emit(ps, 9200, jsonBytes);
        } catch (Exception ex) {
            log.error("[localization] Failed to send translate response", ex);
        }
    }

    private void handleGetAll(PlayerSession ps, byte[] payload) {
        // payload format: [1 byte op][UTF-8 language code, e.g. "zh" or "en"]
        String language = payload.length > 1
                ? new String(payload, 1, payload.length - 1, StandardCharsets.UTF_8).trim()
                : "en";
        if (language.isEmpty()) {
            language = "en";
        }
        Map<String, String> translations = localizationFeign.getAll(language);
        log.debug("Retrieved {} translations for lang '{}'", translations.size(), language);
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(translations);
            Emitters.emit(ps, 9200, jsonBytes);
        } catch (Exception ex) {
            log.error("[localization] Failed to send getAll response", ex);
        }
    }
}
