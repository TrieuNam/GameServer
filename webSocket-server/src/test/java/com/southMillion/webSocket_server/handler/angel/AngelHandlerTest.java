package com.SouthMillion.webSocket_server.handler.angel;

import org.SouthMillion.grpc.common.ResponseStatus;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.role.RoleServiceHandler;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.AngelGrpcClient;
import org.SouthMillion.proto.Msgangel.Msgangel;
import org.SouthMillion.proto.angel.AppearanceLevelUpResponse;
import org.SouthMillion.proto.angel.AngelData;
import org.SouthMillion.proto.angel.GetUserAngelsResponse;
import org.SouthMillion.proto.angel.UseAppearanceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AngelHandlerTest {

    @Mock
    private AngelGrpcClient angelGrpcClient;
    @Mock
    private RoleServiceHandler roleServiceHandler;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;
    @Mock
    private TaskActionConditionMapping taskActionConditionMapping;

    @InjectMocks
    private AngelHandler angelHandler;

    @Test
    void publishTaskProgress_shouldPublishWithValidKey() {
        ReflectionTestUtils.invokeMethod(angelHandler, "publishTaskProgress", 2001L, "condition_84", "websocket-angel-level-up");

        verify(taskProgressPublisher).publish(2001L, "condition_84", 1, "websocket-angel-level-up");
    }

    @Test
    void publishTaskProgress_shouldSkipWithBlankKey() {
        ReflectionTestUtils.invokeMethod(angelHandler, "publishTaskProgress", 2001L, "", "websocket-angel-level-up");

        verify(taskProgressPublisher, never()).publish(anyLong(), anyString(), anyInt(), anyString());
    }

    @Test
    void handle_appearanceLevelUp_usesLegacySeqAndEmitsLegacyRetParams() throws Exception {
        PlayerSession session = PlayerSession.builder()
                .ws(null)
                .outbound(Sinks.many().unicast().onBackpressureBuffer())
                .build();
        session.setRoleId(2001L);

        Msgangel.PB_CSAngelReq req = Msgangel.PB_CSAngelReq.newBuilder()
                .setReqType(3)
                .setParam(5)
                .setParam2(1)
                .build();

        GetUserAngelsResponse before = GetUserAngelsResponse.newBuilder()
                .setStatus(ok())
                .addAngels(withAppearanceLevel(AngelData.newBuilder()
                        .setUserId(2001L)
                        .setAngelIndex(0)
                        .setLevel(12)
                        .setGrade(3)
                        .setIsActive(true)
                        .setIsEquipped(true)
                        .setAppearanceId(0), 0)
                        .build())
                .build();
        GetUserAngelsResponse after = GetUserAngelsResponse.newBuilder()
                .setStatus(ok())
                .addAngels(withAppearanceLevel(AngelData.newBuilder()
                        .setUserId(2001L)
                        .setAngelIndex(0)
                        .setLevel(12)
                        .setGrade(3)
                        .setIsActive(true)
                        .setIsEquipped(true)
                        .setAppearanceId(5), 1)
                        .build())
                .build();

        when(angelGrpcClient.getUserAngels("2001")).thenReturn(before, after);
        when(angelGrpcClient.useAppearance("2001", 0, 5)).thenReturn(UseAppearanceResponse.newBuilder().setStatus(ok()).build());
        when(angelGrpcClient.appearanceLevelUp("2001", 0, 1))
                .thenReturn(AppearanceLevelUpResponse.newBuilder().setStatus(ok()).setNewAppearanceLevel(1).build());
        when(taskActionConditionMapping.angelAppearanceLevelUpTaskKey()).thenReturn("condition_90");

        AtomicReference<byte[]> retBytes = new AtomicReference<>();
        try (MockedStatic<Emitters> emitters = mockStatic(Emitters.class)) {
            emitters.when(() -> Emitters.emit(eq(session), anyInt(), any(byte[].class)))
                    .thenAnswer(invocation -> {
                        int msgId = invocation.getArgument(1);
                        if (msgId == 2132) {
                            retBytes.set(invocation.getArgument(2));
                        }
                        return null;
                    });

            angelHandler.handle(session, 2130, req.toByteArray()).block();
        }

        verify(angelGrpcClient).useAppearance("2001", 0, 5);
        verify(angelGrpcClient).appearanceLevelUp("2001", 0, 1);

        Msgangel.PB_SCAngelOpRet ret = Msgangel.PB_SCAngelOpRet.parseFrom(retBytes.get());
        assertEquals(3, ret.getRetType());
        assertEquals(5, ret.getParam1());
        assertEquals(1, ret.getParam2());
    }

    @Test
    void sendAngelInfo_usesAppearanceSnapshotAndKeepsEquipSlotsSafe() throws Exception {
        PlayerSession session = PlayerSession.builder()
                .ws(null)
                .outbound(Sinks.many().unicast().onBackpressureBuffer())
                .build();
        session.setRoleId(2001L);

        GetUserAngelsResponse resp = GetUserAngelsResponse.newBuilder()
                .setStatus(ok())
                .addAngels(withAppearanceLevel(AngelData.newBuilder()
                        .setUserId(2001L)
                        .setAngelIndex(0)
                        .setLevel(15)
                        .setGrade(4)
                        .setIsActive(true)
                        .setIsEquipped(true)
                        .setAngelId(9999)
                        .setAppearanceId(7), 3)
                        .build())
                .build();

        AtomicReference<byte[]> infoBytes = new AtomicReference<>();
        try (MockedStatic<Emitters> emitters = mockStatic(Emitters.class)) {
            emitters.when(() -> Emitters.emit(eq(session), eq(2131), any(byte[].class)))
                    .thenAnswer(invocation -> {
                        infoBytes.set(invocation.getArgument(2));
                        return null;
                    });

            ReflectionTestUtils.invokeMethod(angelHandler, "sendAngelInfo", session, resp);
        }

        Msgangel.PB_SCAngelInfo info = Msgangel.PB_SCAngelInfo.parseFrom(infoBytes.get());
        assertNotNull(info);
        assertEquals(15, info.getAngelLevel());
        assertEquals(4, info.getAngelGrade());
        assertEquals(7, info.getUseAppearance());
        assertEquals(4, info.getAngelEquipIdCount());
        assertEquals(0, info.getAngelEquipId(0));
        assertEquals(0, info.getAngelEquipId(1));
        assertEquals(0, info.getAngelEquipId(2));
        assertEquals(0, info.getAngelEquipId(3));
        assertEquals(1, info.getAppearanceDataCount());
        assertEquals(7, info.getAppearanceData(0).getId());
        assertEquals(supportsAppearanceLevel() ? 3 : 0, info.getAppearanceData(0).getLevel());
    }

    private static AngelData.Builder withAppearanceLevel(AngelData.Builder builder, int level) {
        try {
            builder.getClass().getMethod("setAppearanceLevel", int.class).invoke(builder, level);
        } catch (ReflectiveOperationException ignored) {
            // Older generated proto builds do not expose this field yet.
        }
        return builder;
    }

    private static boolean supportsAppearanceLevel() {
        try {
            AngelData.Builder.class.getMethod("setAppearanceLevel", int.class);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    private static ResponseStatus ok() {
        return ResponseStatus.newBuilder().setCode(200).setMessage("Success").setSuccess(true).build();
    }
}
