package com.SouthMillion.webSocket_server.handler.pet;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import petguard_service.PetguardService.PB_CSPetFbReq;
import petguard_service.PetguardService.PB_SCPetFbInfo;
import com.SouthMillion.webSocket_server.service.pet.PetGuardService;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetGuardHandler implements MessageHandler {
    private final PetGuardService petGuardService;

    @Override
    public int[] interests() {
        // TODO: Đăng ký đúng msgId cho PetGuard
        return new int[]{2160}; // Ví dụ: 2160 là msgId cho PB_CSPetFbReq
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                PB_CSPetFbReq req = PB_CSPetFbReq.parseFrom(payload);
                PB_SCPetFbInfo resp = petGuardService.process(req, session.getRoleId());
                    Emitters.emit(session, 2161, resp.toByteArray()); // 2161 là msgId trả về
            } catch (Exception e) {
                log.error("[PetGuard] Lỗi xử lý yêu cầu", e);
            }
        });
    }
}
