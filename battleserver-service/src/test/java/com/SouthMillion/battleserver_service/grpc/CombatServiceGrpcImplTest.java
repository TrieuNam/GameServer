package com.SouthMillion.battleserver_service.grpc;

import com.SouthMillion.battleserver_service.dto.CombatRequest;
import com.SouthMillion.battleserver_service.dto.CombatResult;
import com.SouthMillion.battleserver_service.publisher.CombatEventPublisher;
import com.SouthMillion.battleserver_service.service.CombatService;
import io.grpc.stub.StreamObserver;
import org.SouthMillion.grpc.combat.CombatSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CombatServiceGrpcImplTest {

    @Mock
    private CombatService combatService;

    @Mock
    private CombatEventPublisher eventPublisher;

    @Mock
    private StreamObserver<org.SouthMillion.grpc.combat.CombatResponse> responseObserver;

    @Mock
    private StreamObserver<org.SouthMillion.grpc.combat.CombatSession> sessionObserver;

    @Mock
    private StreamObserver<org.SouthMillion.grpc.combat.CombatResult> endObserver;

    private CombatServiceGrpcImpl grpc;

    @BeforeEach
    void setUp() {
        grpc = new CombatServiceGrpcImpl(combatService, eventPublisher);
    }

    @Test
    void calculateCombatPublishesVictoryWhenWinnerMatchesAttacker() {
        when(combatService.validatePlayerStats(any())).thenReturn(true);
        when(combatService.calculateCombat(any(CombatRequest.class))).thenReturn(
                CombatResult.builder()
                        .attackerId(1001L)
                        .defenderId(2002L)
                        .winnerId(1001L)
                        .attackerFinalHp(500)
                        .defenderFinalHp(0)
                        .totalRounds(3)
                        .duration(120L)
                        .build()
        );

        org.SouthMillion.grpc.combat.CombatRequest request = org.SouthMillion.grpc.combat.CombatRequest.newBuilder()
                .setAttackerRoleId(1001L)
                .setDefenderRoleId(2002L)
                .setCombatType("PVP")
                .build();

        grpc.calculateCombat(request, responseObserver);

        ArgumentCaptor<Boolean> victoryCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(eventPublisher).publishCombatResult(
                eq(1001L),
                eq("PVP"),
                victoryCaptor.capture(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );

        assertTrue(victoryCaptor.getValue());
    }

    @Test
    void endCombatPublishesEventForSessionModeCombat() {
        org.SouthMillion.grpc.combat.StartCombatRequest startRequest =
                org.SouthMillion.grpc.combat.StartCombatRequest.newBuilder()
                        .addAttackerRoleIds(3003L)
                        .addDefenderRoleIds(4004L)
                        .setCombatType("TRIAL")
                        .build();

        grpc.startCombat(startRequest, sessionObserver);

        ArgumentCaptor<CombatSession> sessionCaptor = ArgumentCaptor.forClass(CombatSession.class);
        verify(sessionObserver).onNext(sessionCaptor.capture());
        String sessionId = sessionCaptor.getValue().getSessionId();

        org.SouthMillion.grpc.combat.EndCombatRequest endRequest =
                org.SouthMillion.grpc.combat.EndCombatRequest.newBuilder()
                        .setSessionId(sessionId)
                        .setEndReason("TEST_END")
                        .build();

        grpc.endCombat(endRequest, endObserver);

        verify(eventPublisher).publishCombatResult(
                eq(3003L),
                eq("TRIAL"),
                anyBoolean(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }
}

