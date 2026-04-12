package com.SouthMillion.webSocket_server.handler.other;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.net.PacketCodec;
import com.SouthMillion.webSocket_server.service.client.ActivityFeign;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.PetFeign;
import com.SouthMillion.webSocket_server.service.client.RoleFeign;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.SouthMillion.proto.Msgother.Msgother;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtherHandlerTest {

    @Mock
    private ActivityFeign activityFeign;
    @Mock
    private BagFeign bagFeign;
    @Mock
    private PetFeign petFeign;
    @Mock
    private RoleFeign roleFeign;

    @InjectMocks
    private OtherHandler otherHandler;

    @Test
    void handleLimitCore_backendFails_stillEmitsSafeSixLevels() throws Exception {
        PlayerSession session = mock(PlayerSession.class);
        Sinks.Many<byte[]> outbound = mock(Sinks.Many.class);
        lenient().when(session.getRoleId()).thenReturn(2001L);
        lenient().when(session.getOutbound()).thenReturn(outbound);
        lenient().when(outbound.tryEmitNext(any(byte[].class))).thenReturn(Sinks.EmitResult.OK);

        when(roleFeign.limitCore(anyString(), anyMap())).thenThrow(new RuntimeException("downstream failed"));

        Msgother.PB_CSLimitCoreReq req = Msgother.PB_CSLimitCoreReq.newBuilder()
                .setType(0)
                .setP1(1)
                .build();

        otherHandler.handle(session, MsgIds.CS_LIMIT_CORE_REQ, req.toByteArray()).block();

        ArgumentCaptor<byte[]> frames = ArgumentCaptor.forClass(byte[].class);
        verify(outbound, atLeastOnce()).tryEmitNext(frames.capture());

        Msgother.PB_SCLimitCoreInfo info = null;
        for (byte[] frame : frames.getAllValues()) {
            PacketCodec.Decoded d = PacketCodec.decode(frame);
            if (d != null && d.msgId() == MsgIds.SC_LIMIT_CORE_INFO) {
                info = Msgother.PB_SCLimitCoreInfo.parseFrom(d.payload());
                break;
            }
        }

        assertThat(info).isNotNull();
        assertThat(info.getCoreLevelCount()).isEqualTo(6);
        assertThat(info.getCoreLevelList()).containsExactly(0, 0, 0, 0, 0, 0);
    }

    @Test
    void handleLimitCore_drawEmitsNoticeAndPaddedCoreInfo() throws Exception {
        PlayerSession session = mock(PlayerSession.class);
        Sinks.Many<byte[]> outbound = mock(Sinks.Many.class);
        lenient().when(session.getRoleId()).thenReturn(2001L);
        lenient().when(session.getOutbound()).thenReturn(outbound);
        lenient().when(outbound.tryEmitNext(any(byte[].class))).thenReturn(Sinks.EmitResult.OK);

        when(roleFeign.limitCore(anyString(), anyMap())).thenReturn(Map.of(
                "coreLevels", List.of(2, 3),
                "drawnItems", List.of(Map.of("itemId", 40500, "num", 2))
        ));

        Msgother.PB_CSLimitCoreReq req = Msgother.PB_CSLimitCoreReq.newBuilder()
                .setType(1)
                .setP1(2)
                .build();

        otherHandler.handle(session, MsgIds.CS_LIMIT_CORE_REQ, req.toByteArray()).block();

        ArgumentCaptor<byte[]> frames = ArgumentCaptor.forClass(byte[].class);
        verify(outbound, atLeastOnce()).tryEmitNext(frames.capture());

        Msgknapsack.PB_SCGetItemNotice notice = null;
        Msgother.PB_SCLimitCoreInfo info = null;

        for (byte[] frame : frames.getAllValues()) {
            PacketCodec.Decoded d = PacketCodec.decode(frame);
            if (d == null) continue;
            if (d.msgId() == MsgIds.SC_GET_ITEM_NOTICE) {
                notice = Msgknapsack.PB_SCGetItemNotice.parseFrom(d.payload());
            } else if (d.msgId() == MsgIds.SC_LIMIT_CORE_INFO) {
                info = Msgother.PB_SCLimitCoreInfo.parseFrom(d.payload());
            }
        }

        assertThat(notice).isNotNull();
        assertThat(notice.getGetType()).isEqualTo(93);
        assertThat(notice.getItemListCount()).isEqualTo(1);
        assertThat(notice.getItemList(0).getItemId()).isEqualTo(40500);
        assertThat(notice.getItemList(0).getNum()).isEqualTo(2);

        assertThat(info).isNotNull();
        assertThat(info.getCoreLevelCount()).isEqualTo(6);
        assertThat(new ArrayList<>(info.getCoreLevelList()).subList(0, 2)).containsExactly(2, 3);
        assertThat(new ArrayList<>(info.getCoreLevelList()).subList(2, 6)).containsExactly(0, 0, 0, 0);
    }
}
