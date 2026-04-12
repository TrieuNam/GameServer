package com.SouthMillion.webSocket_server.handler.lingzhu;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.LingZhuGrpcClient;
import org.SouthMillion.proto.Msglingzhu.Msglingzhu;
import org.SouthMillion.proto.lingzhu.GenericResponse;
import org.SouthMillion.proto.lingzhu.GetAllResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LingZhuHandlerTest {

    @Mock
    private LingZhuGrpcClient lingZhuGrpcClient;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private LingZhuHandler lingZhuHandler;

    private static final long ROLE_ID = 2001L;
    private PlayerSession session;

    @BeforeEach
    void setUp() {
        session = mock(PlayerSession.class);
        lenient().when(session.getRoleId()).thenReturn(ROLE_ID);
    }

    // ── publishTaskProgress helper tests ──────────────────────────────────

    @Test
    void publishTaskProgress_shouldPublishWithValidKey() {
        ReflectionTestUtils.invokeMethod(lingZhuHandler, "publishTaskProgress", 2001L, "condition_89", "websocket-lingzhu-challenge");

        verify(taskProgressPublisher).publish(2001L, "condition_89", 1, "websocket-lingzhu-challenge");
    }

    @Test
    void publishTaskProgress_shouldSkipWithBlankKey() {
        ReflectionTestUtils.invokeMethod(lingZhuHandler, "publishTaskProgress", 2001L, "", "websocket-lingzhu-challenge");

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }

    // ── handle() dispatch tests ───────────────────────────────────────────

    @Test
    void handle_opFight_validatesWithGrpcAndSendsInfo() {
        byte[] payload = Msglingzhu.PB_CSLingZhuReq.newBuilder()
                .setType(0).setP1(1).setP2(2).build().toByteArray();
        when(lingZhuGrpcClient.challenge(ROLE_ID, 1, 2))
                .thenReturn(GenericResponse.newBuilder().setSuccess(true).build());
        when(lingZhuGrpcClient.getAll(ROLE_ID)).thenReturn(GetAllResponse.newBuilder().build());

        try (MockedStatic<Emitters> ignored = mockStatic(Emitters.class)) {
            lingZhuHandler.handle(session, 2008, payload).block();
        }

        verify(lingZhuGrpcClient).challenge(ROLE_ID, 1, 2);
    }

    @Test
    void handle_opFight_missingP2_serverReceivesZero() {
        // p2 not set (defaults to 0) — LingZhuService.challenge() treats 0 as auto-derive
        byte[] payload = Msglingzhu.PB_CSLingZhuReq.newBuilder()
                .setType(0).setP1(1).build().toByteArray();
        when(lingZhuGrpcClient.challenge(ROLE_ID, 1, 0))
                .thenReturn(GenericResponse.newBuilder().setSuccess(true).build());
        when(lingZhuGrpcClient.getAll(ROLE_ID)).thenReturn(GetAllResponse.newBuilder().build());

        try (MockedStatic<Emitters> ignored = mockStatic(Emitters.class)) {
            lingZhuHandler.handle(session, 2008, payload).block();
        }

        verify(lingZhuGrpcClient).challenge(ROLE_ID, 1, 0);
    }

    @Test
    void handle_opFinish_winPublishesTaskProgress() {
        byte[] payload = Msglingzhu.PB_CSLingZhuReq.newBuilder()
                .setType(4).setP1(1).setP2(2).build().toByteArray();
        when(lingZhuGrpcClient.finishChallenge(ROLE_ID, 1, 2))
                .thenReturn(GenericResponse.newBuilder().setSuccess(true).build());
        when(taskActionConditionMapping.lingzhuChallengeTaskKey()).thenReturn("condition_89");
        when(lingZhuGrpcClient.getAll(ROLE_ID)).thenReturn(GetAllResponse.newBuilder().build());

        try (MockedStatic<Emitters> ignored = mockStatic(Emitters.class)) {
            lingZhuHandler.handle(session, 2008, payload).block();
        }

        verify(lingZhuGrpcClient).finishChallenge(ROLE_ID, 1, 2);
        verify(taskProgressPublisher).publish(ROLE_ID, "condition_89", 1, "websocket-lingzhu-finish");
    }

    @Test
    void handle_opFinish_lossFails_noTaskProgress() {
        byte[] payload = Msglingzhu.PB_CSLingZhuReq.newBuilder()
                .setType(4).setP1(1).setP2(2).build().toByteArray();
        when(lingZhuGrpcClient.finishChallenge(ROLE_ID, 1, 2))
                .thenReturn(GenericResponse.newBuilder().setSuccess(false).build());
        when(lingZhuGrpcClient.getAll(ROLE_ID)).thenReturn(GetAllResponse.newBuilder().build());

        try (MockedStatic<Emitters> ignored = mockStatic(Emitters.class)) {
            lingZhuHandler.handle(session, 2008, payload).block();
        }

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }

    @Test
    void handle_opMop_sweepSucceeds_publishesTaskProgress() {
        byte[] payload = Msglingzhu.PB_CSLingZhuReq.newBuilder()
                .setType(1).setP1(2).setP2(3).build().toByteArray();
        when(lingZhuGrpcClient.sweep(ROLE_ID, 2, 3))
                .thenReturn(GenericResponse.newBuilder().setSuccess(true).build());
        when(taskActionConditionMapping.lingzhuSweepTaskKey()).thenReturn("condition_90");
        when(lingZhuGrpcClient.getAll(ROLE_ID)).thenReturn(GetAllResponse.newBuilder().build());

        try (MockedStatic<Emitters> ignored = mockStatic(Emitters.class)) {
            lingZhuHandler.handle(session, 2008, payload).block();
        }

        verify(lingZhuGrpcClient).sweep(ROLE_ID, 2, 3);
        verify(taskProgressPublisher).publish(ROLE_ID, "condition_90", 1, "websocket-lingzhu-sweep");
    }

    @Test
    void handle_opQuickMop_missingP2_defaultsToOne() {
        // p2=0 → handler falls back to 1
        byte[] payload = Msglingzhu.PB_CSLingZhuReq.newBuilder()
                .setType(2).setP1(1).build().toByteArray();
        when(lingZhuGrpcClient.sweep(ROLE_ID, 1, 1))
                .thenReturn(GenericResponse.newBuilder().setSuccess(true).build());
        when(taskActionConditionMapping.lingzhuSweepTaskKey()).thenReturn("condition_90");
        when(lingZhuGrpcClient.getAll(ROLE_ID)).thenReturn(GetAllResponse.newBuilder().build());

        try (MockedStatic<Emitters> ignored = mockStatic(Emitters.class)) {
            lingZhuHandler.handle(session, 2008, payload).block();
        }

        verify(lingZhuGrpcClient).sweep(ROLE_ID, 1, 1);
        verify(taskProgressPublisher).publish(ROLE_ID, "condition_90", 1, "websocket-lingzhu-sweep");
    }

    @Test
    void handle_opInfo_noMutationCalls() {
        byte[] payload = Msglingzhu.PB_CSLingZhuReq.newBuilder()
                .setType(3).build().toByteArray();
        when(lingZhuGrpcClient.getAll(ROLE_ID)).thenReturn(GetAllResponse.newBuilder().build());

        try (MockedStatic<Emitters> ignored = mockStatic(Emitters.class)) {
            lingZhuHandler.handle(session, 2008, payload).block();
        }

        verify(lingZhuGrpcClient, never()).challenge(anyLong(), anyInt(), anyInt());
        verify(lingZhuGrpcClient, never()).finishChallenge(anyLong(), anyInt(), anyInt());
        verify(lingZhuGrpcClient, never()).sweep(anyLong(), anyInt(), anyInt());
    }
}
