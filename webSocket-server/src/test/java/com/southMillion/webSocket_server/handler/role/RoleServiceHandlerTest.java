package com.SouthMillion.webSocket_server.handler.role;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.net.PacketCodec;
import com.SouthMillion.webSocket_server.service.InMemoryPlayerSessionRegistry;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.MountFeign;
import com.SouthMillion.webSocket_server.service.client.RoleFeign;
import com.SouthMillion.webSocket_server.service.client.ShiZhuangFeign;
import com.SouthMillion.webSocket_server.service.grpc.AngelGrpcClient;
import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.SouthMillion.dto.role.settings.SettingsDTOs;
import org.SouthMillion.dto.role.settings.SystemSettings;
import org.SouthMillion.proto.Msgother.Msgother;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceHandlerTest {

    @Mock
    private RoleFeign roleFeign;
    @Mock
    private ShiZhuangFeign shiZhuangFeign;
    @Mock
    private MountFeign mountFeign;
    @Mock
    private AngelGrpcClient angelGrpcClient;
    @Mock
    private InMemoryPlayerSessionRegistry registry;
    @Mock
    private TaskProgressPublisher taskProgressPublisher;

    @InjectMocks
    private RoleServiceHandler roleServiceHandler;

    @Test
    void handle_noticeTimeReq_queriesRoleServiceAndEmitsNoticeTimeRet() throws Exception {
        PlayerSession session = PlayerSession.builder()
                .ws(null)
                .outbound(Sinks.many().unicast().onBackpressureBuffer())
                .build();
        session.setRoleId(2001L);
        session.setSessionId("notice-session");

        when(roleFeign.noticeTime(eq("2001"), any(Map.class)))
                .thenReturn(Map.of("noticeTime", 123456L));

        Msgother.PB_CSNoticeTimeReq req = Msgother.PB_CSNoticeTimeReq.newBuilder()
                .setType(1)
                .setParam(123456L)
                .build();

        List<byte[]> frames = new ArrayList<>();
        session.getOutbound().asFlux().subscribe(frames::add);
        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);

        roleServiceHandler.handle(session, MsgIds.CS_NOTICE_TIME_REQ, req.toByteArray()).block();

        verify(roleFeign).noticeTime(eq("2001"), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue()).containsEntry("type", 1).containsEntry("param", 123456L);
        assertThat(frames).isNotEmpty();

        PacketCodec.Decoded decoded = PacketCodec.decode(frames.get(0));
        assertThat(decoded).isNotNull();
        assertThat(decoded.msgId()).isEqualTo(MsgIds.SC_NOTICE_TIME_RET);

        Msgother.PB_SCNoticeTimeRet ret = Msgother.PB_SCNoticeTimeRet.parseFrom(decoded.payload());
        assertThat(ret.getNoticeTime()).isEqualTo(123456L);
    }

    @Test
    void handle_systemSetReq_mapsLegacyAudioFlagsToNamedRoleSettings() throws Exception {
        PlayerSession session = PlayerSession.builder()
                .ws(null)
                .outbound(Sinks.many().unicast().onBackpressureBuffer())
                .build();
        session.setUserId("user-2001");
        session.setRoleId(2001L);
        session.setSessionId("role-setting-session");

        when(roleFeign.applySettings(any())).thenReturn(new SettingsDTOs.SystemSetResp(
                "user-2001",
                SystemSettings.defaults(),
                Instant.now()));
        when(roleFeign.getOtherRole("user-2001", "2001")).thenReturn(mock(OtherRoleDTOs.OtherRoleInfo.class));

        Msgrole.PB_CSRoleSystemSetReq req = Msgrole.PB_CSRoleSystemSetReq.newBuilder()
                .addSystemSetList(Msgrole.PB_system_set.newBuilder().setSystemSetType(0).setSystemSetParam(1).build())
                .addSystemSetList(Msgrole.PB_system_set.newBuilder().setSystemSetType(1).setSystemSetParam(0).build())
                .addSystemSetList(Msgrole.PB_system_set.newBuilder().setSystemSetType(2).setSystemSetParam(1).build())
                .build();

        ArgumentCaptor<SettingsDTOs.SystemSetReq> reqCaptor = ArgumentCaptor.forClass(SettingsDTOs.SystemSetReq.class);

        roleServiceHandler.handle(session, MsgIds.CS_ROLE_SYSTEM_SET_REQ, req.toByteArray()).block();

        verify(roleFeign).applySettings(reqCaptor.capture());
        assertThat(reqCaptor.getValue().userId()).isEqualTo("user-2001");
        assertThat(reqCaptor.getValue().systemSetList())
                .extracting(SettingsDTOs.SystemSettingItem::key, SettingsDTOs.SystemSettingItem::value)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("music", false),
                        org.assertj.core.groups.Tuple.tuple("sfx", true),
                        org.assertj.core.groups.Tuple.tuple("vibrate", false)
                );
    }

    @Test
    void pushAll_emitsStoredRoleSettingsAsLegacy1461Flags() throws Exception {
        PlayerSession session = PlayerSession.builder()
                .ws(null)
                .outbound(Sinks.many().unicast().onBackpressureBuffer())
                .build();
        session.setUserId("user-2001");
        session.setRoleId(2001L);
        session.setSessionId("role-setting-push-session");

        when(roleFeign.getOtherRole("user-2001", "2001")).thenReturn(mock(OtherRoleDTOs.OtherRoleInfo.class));
        lenient().when(roleFeign.applySettings(any())).thenReturn(new SettingsDTOs.SystemSetResp(
                "user-2001",
                new SystemSettings(false, 100, true, 100, true, false, "vi", Map.of()),
                Instant.now()));

        List<byte[]> frames = new ArrayList<>();
        session.getOutbound().asFlux().subscribe(frames::add);

        roleServiceHandler.pushAll(session).block();

        PacketCodec.Decoded decoded = frames.stream()
                .map(PacketCodec::decode)
                .filter(packet -> packet != null && packet.msgId() == MsgIds.SC_ROLE_SYSTEM_SET_INFO)
                .findFirst()
                .orElse(null);

        assertThat(decoded).as("system settings push frame").isNotNull();
        Msgrole.PB_SCRoleSystemSetInfo info = Msgrole.PB_SCRoleSystemSetInfo.parseFrom(decoded.payload());
        assertThat(info.getSystemSetListList())
                .extracting(Msgrole.PB_system_set::getSystemSetType, Msgrole.PB_system_set::getSystemSetParam)
                .contains(
                        org.assertj.core.groups.Tuple.tuple(0, 1),
                        org.assertj.core.groups.Tuple.tuple(1, 0),
                        org.assertj.core.groups.Tuple.tuple(2, 1)
                );
    }
}
