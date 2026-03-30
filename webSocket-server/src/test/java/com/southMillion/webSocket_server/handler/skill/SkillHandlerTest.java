package com.SouthMillion.webSocket_server.handler.skill;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.net.PacketCodec;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.SkillFeign;
import org.SouthMillion.dto.skill.SkillDTOs;
import org.SouthMillion.proto.Msgskill.Msgskill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Sinks;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillHandler Tests")
class SkillHandlerTest {

    @Mock
    private SkillFeign skillFeign;

        @Mock
        private TaskProgressPublisher taskProgressPublisher;

        @Mock
        private TaskActionConditionMapping taskActionConditionMapping;

    @Mock
    private PlayerSession playerSession;

    @Mock
    private Sinks.Many<byte[]> outbound;

    @InjectMocks
    private SkillHandler skillHandler;

    @BeforeEach
    void setUp() {
        when(playerSession.getRoleId()).thenReturn(1001L);
        when(playerSession.getSessionId()).thenReturn("session-token");
        when(playerSession.getOutbound()).thenReturn(outbound);
        when(outbound.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);
    }

    @Test
    @DisplayName("TC-SKHD-001 [P] 1470 reqType=LEARN được parse, gọi feign và emit 1471")
    void handleSkillLearn_dispatchAndEmit() {
        SkillDTOs.SkillAllInfoResp feignResp = SkillDTOs.SkillAllInfoResp.builder()
                .skillCount(1)
                .skillList(List.of(SkillDTOs.RoleSkillInfo.builder()
                        .skillId(2001)
                        .skillLevel(3)
                        .skillIndex(0)
                        .build()))
                .errorCode(SkillDTOs.SkillErrorCodes.OK)
                .build();
        when(skillFeign.learnSkill(eq(1001L), any())).thenReturn(feignResp);

        byte[] payload = Msgskill.PB_CSRoleSkillOperaReq.newBuilder()
                .setReqType(1)
                .setParam1(2001)
                .build()
                .toByteArray();

        skillHandler.handle(playerSession, MsgIds.CS_ROLE_SKILL_OPERA_REQ, payload).block();

        verify(skillFeign).learnSkill(eq(1001L), any(SkillDTOs.LearnSkillReq.class));
        ArgumentCaptor<byte[]> frameCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(outbound).tryEmitNext(frameCaptor.capture());

        PacketCodec.Decoded decoded = PacketCodec.decode(frameCaptor.getValue());
        assertThat(decoded).isNotNull();
        assertThat(decoded.msgId()).isEqualTo(MsgIds.SC_ROLE_SKILL_ALL_INFO);

        Msgskill.PB_SCRoleSkillAllInfo sc = assertDoesNotThrow(() -> Msgskill.PB_SCRoleSkillAllInfo.parseFrom(decoded.payload()));
        assertThat(sc.getSkillCount()).isEqualTo(1);
        assertThat(sc.getSkillListCount()).isEqualTo(1);
        assertThat(sc.getSkillList(0).getSkillId()).isEqualTo(2001);
        assertThat(sc.getSkillList(0).getSkillLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("TC-SKHD-002 [P] 1480 reqType=INFO dispatch đúng và emit 1481")
    void handleTalentInfo_dispatchAndEmit() {
        SkillDTOs.TalentAllInfoResp feignResp = SkillDTOs.TalentAllInfoResp.builder()
                .talentSkillCount(1)
                .talentSkillList(List.of(SkillDTOs.RoleTalentInfo.builder()
                        .skillId(3101)
                        .skillLevel(2)
                        .build()))
                .errorCode(SkillDTOs.SkillErrorCodes.OK)
                .build();
        when(skillFeign.getTalentAll(1001L)).thenReturn(feignResp);

        byte[] payload = Msgskill.PB_CSRoleTalentOperaReq.newBuilder()
                .setReqType(0)
                .build()
                .toByteArray();

        skillHandler.handle(playerSession, MsgIds.CS_ROLE_TALENT_OPERA_REQ, payload).block();

        verify(skillFeign).getTalentAll(1001L);
        ArgumentCaptor<byte[]> frameCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(outbound).tryEmitNext(frameCaptor.capture());

        PacketCodec.Decoded decoded = PacketCodec.decode(frameCaptor.getValue());
        assertThat(decoded).isNotNull();
        assertThat(decoded.msgId()).isEqualTo(MsgIds.SC_ROLE_TALENT_ALL_INFO);

        Msgskill.PB_SCRoleTalentAllInfo sc = assertDoesNotThrow(() -> Msgskill.PB_SCRoleTalentAllInfo.parseFrom(decoded.payload()));
        assertThat(sc.getTalentSkillCount()).isEqualTo(1);
        assertThat(sc.getTalentSkillListCount()).isEqualTo(1);
        assertThat(sc.getTalentSkillList(0).getSkillId()).isEqualTo(3101);
    }

    @Test
    @DisplayName("TC-SKHD-003 [N] payload hỏng không làm crash session")
    void handleInvalidPayload_graceful() {
        byte[] brokenPayload = new byte[]{0x01, 0x02};

        assertDoesNotThrow(() ->
                skillHandler.handle(playerSession, MsgIds.CS_ROLE_SKILL_OPERA_REQ, brokenPayload).block()
        );

        verify(skillFeign, never()).getSkillAll(any());
        verify(skillFeign, never()).learnSkill(any(), any());
    }

    @Test
    @DisplayName("TC-SKHD-004 [N] lỗi downstream feign được nuốt và không crash")
    void handleFeignError_graceful() {
        when(skillFeign.getSkillAll(1001L)).thenThrow(new RuntimeException("role-service down"));

        byte[] payload = Msgskill.PB_CSRoleSkillOperaReq.newBuilder()
                .setReqType(0)
                .build()
                .toByteArray();

        assertDoesNotThrow(() ->
                skillHandler.handle(playerSession, MsgIds.CS_ROLE_SKILL_OPERA_REQ, payload).block()
        );
    }

        @Test
        @DisplayName("publishTaskProgress helper publishes when key is present")
        void publishTaskProgress_helperPublishes() {
                ReflectionTestUtils.invokeMethod(
                                skillHandler,
                                "publishTaskProgress",
                                1001L,
                                "condition_71",
                                "websocket-skill-learn"
                );

                verify(taskProgressPublisher).publish(1001L, "condition_71", 1, "websocket-skill-learn");
        }

        @Test
        @DisplayName("publishTaskProgress helper skips blank key")
        void publishTaskProgress_helperSkipsBlank() {
                ReflectionTestUtils.invokeMethod(
                                skillHandler,
                                "publishTaskProgress",
                                1001L,
                                "",
                                "websocket-skill-learn"
                );

                verify(taskProgressPublisher, never()).publish(any(), any(), any(), any());
        }
}
