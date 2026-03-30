package com.SouthMillion.webSocket_server.handler.battle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.webSocket_server.constant.MessageIds;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.service.grpc.BattleServerGrpcClient;
import org.SouthMillion.grpc.combat.CombatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BattleHandlerTest {

    @Mock
    private BattleServerGrpcClient battleServerGrpcClient;

    @Mock
    private PlayerSession session;

    private BattleHandler battleHandler;

    @BeforeEach
    void setUp() {
        battleHandler = new BattleHandler(battleServerGrpcClient, new ObjectMapper());
    }

    @Test
    void interestsShouldContainBattleRequestId() {
        assertArrayEquals(new int[]{MessageIds.CS_BATTLE_REQ}, battleHandler.interests());
    }

    @Test
    void handleShouldNotThrowWhenRoleIdMissing() {
        when(session.getRoleId()).thenReturn(null);

        assertDoesNotThrow(() -> battleHandler.handle(session, MessageIds.CS_BATTLE_REQ, new byte[0]).block());
    }

    @Test
    void handleCalculateCombatShouldNotThrow() {
        when(session.getRoleId()).thenReturn(1001L);
        when(session.getOutbound()).thenReturn(null);
        when(battleServerGrpcClient.calculateCombat(anyString(), anyString(), anyString(), any()))
                .thenReturn(CombatResponse.newBuilder().setAttackerWins(true).setRounds(3).setCombatDurationMs(120).build());

        String jsonPayload = "{\"op\":1,\"targetRoleId\":2002,\"combatType\":2}";
        assertDoesNotThrow(() -> battleHandler.handle(session, MessageIds.CS_BATTLE_REQ, jsonPayload.getBytes()).block());
    }
}

