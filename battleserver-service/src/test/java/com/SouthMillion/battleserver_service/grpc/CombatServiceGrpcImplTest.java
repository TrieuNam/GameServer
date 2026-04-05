package com.SouthMillion.battleserver_service.grpc;

import com.SouthMillion.battleserver_service.dto.CombatRequest;
import com.SouthMillion.battleserver_service.dto.CombatResult;
import com.SouthMillion.battleserver_service.publisher.CombatEventPublisher;
import com.SouthMillion.battleserver_service.service.CombatService;
import com.SouthMillion.battleserver_service.service.MonsterStatsService;
import com.SouthMillion.battleserver_service.service.RoleStatsService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CombatServiceGrpcImplTest {

    @Mock
    private CombatService combatService;

    @Mock
    private CombatEventPublisher eventPublisher;

    @Mock
    private RoleStatsService roleStatsService;

    @Mock
    private MonsterStatsService monsterStatsService;

    @Mock
    private StreamObserver<org.SouthMillion.grpc.combat.CombatResponse> responseObserver;

    @Mock
    private StreamObserver<org.SouthMillion.grpc.combat.CombatSession> sessionObserver;

    @Mock
    private StreamObserver<org.SouthMillion.grpc.combat.CombatResult> endObserver;

    private CombatServiceGrpcImpl grpc;

    @BeforeEach
    void setUp() {
        when(roleStatsService.getPlayerStats(anyLong())).thenAnswer(invocation -> {
            long roleId = invocation.getArgument(0, Long.class);
            return com.SouthMillion.battleserver_service.dto.PlayerStats.builder()
                    .playerId(roleId)
                    .hp(1000)
                    .maxHp(1000)
                    .attack(150)
                    .defense(50)
                    .speed(100)
                    .critRate(0)
                    .critDamage(200)
                    .vampiric(0)
                    .vampiricImmunity(0)
                    .counter(0)
                    .counterImmunity(0)
                    .combo(0)
                    .comboImmunity(0)
                    .evasion(0)
                    .evasionImmunity(0)
                    .criticalImmunity(0)
                    .stun(0)
                    .stunImmunity(0)
                    .tyranny(0)
                    .benevolence(0)
                    .muddy(0)
                    .interdiction(0)
                    .rejuvenation(0)
                    .level(1)
                    .fightPower(1300)
                    .build();
        });
        lenient().when(monsterStatsService.getMonsterStats(any(), any(), any(), anyBoolean())).thenAnswer(invocation -> {
            Long roleId = invocation.getArgument(0, Long.class);
            return com.SouthMillion.battleserver_service.dto.PlayerStats.builder()
                    .playerId(roleId != null ? roleId : 0L)
                    .hp(1200)
                    .maxHp(1200)
                    .attack(180)
                    .defense(60)
                    .speed(90)
                    .critRate(0)
                    .critDamage(200)
                    .vampiric(0)
                    .vampiricImmunity(0)
                    .counter(0)
                    .counterImmunity(0)
                    .combo(0)
                    .comboImmunity(0)
                    .evasion(0)
                    .evasionImmunity(0)
                    .criticalImmunity(0)
                    .stun(0)
                    .stunImmunity(0)
                    .tyranny(0)
                    .benevolence(0)
                    .muddy(0)
                    .interdiction(0)
                    .rejuvenation(0)
                    .level(1)
                    .fightPower(1600)
                    .build();
        });
        grpc = new CombatServiceGrpcImpl(combatService, eventPublisher, roleStatsService, monsterStatsService);
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
    void calculateCombatUsesMonsterStatsWhenContextIncludesMonster() {
        when(combatService.validatePlayerStats(any())).thenReturn(true);
        when(combatService.calculateCombat(any(CombatRequest.class))).thenReturn(
                CombatResult.builder()
                        .attackerId(1001L)
                        .defenderId(2002L)
                        .winnerId(1001L)
                        .attackerFinalHp(700)
                        .defenderFinalHp(0)
                        .totalRounds(2)
                        .duration(80L)
                        .build()
        );

        org.SouthMillion.grpc.combat.CombatRequest request = org.SouthMillion.grpc.combat.CombatRequest.newBuilder()
                .setAttackerRoleId(1001L)
                .setDefenderRoleId(2002L)
                .setCombatType("PVE")
                .setContext(org.SouthMillion.grpc.combat.CombatContext.newBuilder()
                        .setStageId(12)
                        .setMonsterId(9001)
                        .setIsBoss(true)
                        .build())
                .build();

        grpc.calculateCombat(request, responseObserver);

        verify(monsterStatsService).getMonsterStats(eq(2002L), eq(9001), eq(12), eq(true));
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

