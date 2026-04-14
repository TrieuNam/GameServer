package com.SouthMillion.webSocket_server.handler.wabao;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.PacketCodec;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BoxFeign;
import org.SouthMillion.dto.box.BoxDTOs;
import org.SouthMillion.proto.Msgwabao.Msgwabao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WaBaoHandlerTest {

    @Mock private BoxFeign boxFeign;
    @Mock private TaskProgressPublisher taskProgressPublisher;

    @InjectMocks private WaBaoHandler waBaoHandler;

    private PlayerSession playerSession;
    private Sinks.Many<byte[]> outbound;

    @BeforeEach
    void setUp() {
        playerSession = Mockito.mock(PlayerSession.class);
        @SuppressWarnings("unchecked")
        Sinks.Many<byte[]> sink = Mockito.mock(Sinks.Many.class);
        outbound = sink;

        when(playerSession.getRoleId()).thenReturn(2001L);
        when(playerSession.getOutbound()).thenReturn(outbound);
        Mockito.lenient().when(outbound.tryEmitNext(any())).thenReturn(Sinks.EmitResult.OK);
        Mockito.lenient().when(taskProgressPublisher.publish(any(), any(), any(Integer.class), any())).thenReturn(true);

        Mockito.lenient().when(boxFeign.info(2001L)).thenReturn(BoxDTOs.InfoResp.builder()
                .boxLevel(7)
                .boxBuyTimes(3)
                .levelUpEndEpoch(0)
                .levelFetchFlag(1)
                .build());
        Mockito.lenient().when(boxFeign.getSetting(2001L)).thenReturn(BoxDTOs.BoxSettingResp.builder().build());
        Mockito.lenient().when(boxFeign.getWaBaoMapInfo(2001L)).thenReturn(BoxDTOs.WaBaoMapInfo.builder()
                .curMap(2)
                .unlockedMap(4)
                .mapConditionNum(List.of(1, 2, 3))
                .build());
        Mockito.lenient().when(boxFeign.getWaBaoIntegrity(2001L)).thenReturn(BoxDTOs.WaBaoIntegrityInfo.builder()
                .isLogin(1)
                .dataList(List.of())
                .build());
        Mockito.lenient().when(boxFeign.getWaBaoCollection(2001L)).thenReturn(BoxDTOs.WaBaoCollectionInfo.builder()
                .isLogin(1)
                .dataList(List.of())
                .build());
        Mockito.lenient().when(boxFeign.getWaBaoToolInfo(2001L)).thenReturn(BoxDTOs.WaBaoToolInfo.builder()
                .toolGrade(List.of())
                .toolLevel(List.of())
                .conditionType(List.of())
                .conditionNum(List.of())
                .toolList(List.of())
                .build());
        Mockito.lenient().when(boxFeign.getWaBaoTaskInfo(2001L)).thenReturn(BoxDTOs.WaBaoTaskInfo.builder()
                .taskFlag(0)
                .taskList(List.of())
                .taskTypeNumList(List.of())
                .build());
        Mockito.lenient().when(boxFeign.getWaBaoCollectionBookInfo(2001L)).thenReturn(BoxDTOs.WaBaoCollectionBookInfo.builder()
                .level(List.of())
                .build());
        Mockito.lenient().when(boxFeign.getWaBaoBookListInfo(2001L)).thenReturn(BoxDTOs.WaBaoBookListInfo.builder()
                .activateFlagList(List.of())
                .build());
    }

    @Test
    @DisplayName("op 5 forwards putCollection and emits SC 1646/1642")
    void handlePutCollection_emitsCollectionAndInfo() throws Exception {
        when(boxFeign.putCollection(any())).thenReturn(BoxDTOs.OkResp.builder().ok(true).message("ok").build());
        when(boxFeign.getWaBaoCollection(2001L)).thenReturn(BoxDTOs.WaBaoCollectionInfo.builder()
                .isLogin(1)
                .dataList(List.of(BoxDTOs.WaBaoCollectionNode.builder()
                        .itemType(2)
                        .index(5)
                        .itemId(7001)
                        .integrity(0)
                        .attrType1(11)
                        .attrValue1(22)
                        .attrType2(33)
                        .attrValue2(44)
                        .build()))
                .build());

        Msgwabao.PB_CSWaBaoReq req = Msgwabao.PB_CSWaBaoReq.newBuilder()
                .setOpType(5)
                .setParam1(2)
                .setParam2(9)
                .build();

        assertDoesNotThrow(() -> waBaoHandler.handle(playerSession, 1640, req.toByteArray()).block());

        ArgumentCaptor<BoxDTOs.PutCollectionReq> reqCaptor = ArgumentCaptor.forClass(BoxDTOs.PutCollectionReq.class);
        verify(boxFeign).putCollection(reqCaptor.capture());
        assertEquals("2001", reqCaptor.getValue().getRoleId());
        assertEquals(2, reqCaptor.getValue().getItemType());
        assertEquals(5, reqCaptor.getValue().getIndex());

        Msgwabao.PB_SCWaBaoCollectionListInfo collectionInfo = firstMessage(1646, Msgwabao.PB_SCWaBaoCollectionListInfo.class);
        assertEquals(1, collectionInfo.getIsLogin());
        assertEquals(1, collectionInfo.getDataListCount());
        assertEquals(2, collectionInfo.getDataList(0).getItemType());
        assertEquals(5, collectionInfo.getDataList(0).getIndex());
        assertEquals(7001, collectionInfo.getDataList(0).getItemData().getItemId());

        Msgwabao.PB_SCWaBaoInfo info = firstMessage(1642, Msgwabao.PB_SCWaBaoInfo.class);
        assertEquals(7, info.getCollectionLevel());
        assertEquals(3, info.getCollectionBuyTimes());
        assertEquals(1, info.getCollectionLevelFetchFlag());
    }

    @Test
    @DisplayName("op 6 forwards collectionSell and emits cleared SC 1646/1642")
    void handleCollectionSell_emitsCollectionAndInfo() throws Exception {
        when(boxFeign.collectionSell(any())).thenReturn(BoxDTOs.OkResp.builder().ok(true).message("ok").build());
        when(boxFeign.getWaBaoCollection(2001L)).thenReturn(BoxDTOs.WaBaoCollectionInfo.builder()
                .isLogin(1)
                .dataList(List.of(BoxDTOs.WaBaoCollectionNode.builder()
                        .itemType(1)
                        .index(0)
                        .itemId(0)
                        .integrity(0)
                        .attrType1(0)
                        .attrValue1(0)
                        .attrType2(0)
                        .attrValue2(0)
                        .build()))
                .build());

        Msgwabao.PB_CSWaBaoReq req = Msgwabao.PB_CSWaBaoReq.newBuilder()
                .setOpType(6)
                .setParam1(1)
                .setParam2(8)
                .build();

        assertDoesNotThrow(() -> waBaoHandler.handle(playerSession, 1640, req.toByteArray()).block());

        ArgumentCaptor<BoxDTOs.CollectionSellReq> reqCaptor = ArgumentCaptor.forClass(BoxDTOs.CollectionSellReq.class);
        verify(boxFeign).collectionSell(reqCaptor.capture());
        assertEquals("2001", reqCaptor.getValue().getRoleId());
        assertEquals(1, reqCaptor.getValue().getItemType());
        assertEquals(5, reqCaptor.getValue().getIndex());

        Msgwabao.PB_SCWaBaoCollectionListInfo collectionInfo = firstMessage(1646, Msgwabao.PB_SCWaBaoCollectionListInfo.class);
        assertEquals(1, collectionInfo.getDataListCount());
        assertEquals(0, collectionInfo.getDataList(0).getItemData().getItemId());

        Msgwabao.PB_SCWaBaoInfo info = firstMessage(1642, Msgwabao.PB_SCWaBaoInfo.class);
        assertEquals(7, info.getCollectionLevel());
    }

    @Test
    @DisplayName("op 12 forwards toolUpLevel and emits SC 1647/1642")
    void handleToolUpLevel_emitsToolInfoAndInfo() throws Exception {
        when(boxFeign.toolUpLevel(any())).thenReturn(BoxDTOs.OkResp.builder().ok(true).message("ok").build());
        when(boxFeign.getWaBaoToolInfo(2001L)).thenReturn(BoxDTOs.WaBaoToolInfo.builder()
                .toolGrade(List.of(1, 2))
                .toolLevel(List.of(3, 4))
                .conditionType(List.of(0, 1))
                .conditionNum(List.of(5, 6))
                .toolList(List.of())
                .build());

        Msgwabao.PB_CSWaBaoReq req = Msgwabao.PB_CSWaBaoReq.newBuilder()
                .setOpType(12)
                .setParam1(3)
                .build();

        assertDoesNotThrow(() -> waBaoHandler.handle(playerSession, 1640, req.toByteArray()).block());

        ArgumentCaptor<BoxDTOs.ToolUpLevelReq> reqCaptor = ArgumentCaptor.forClass(BoxDTOs.ToolUpLevelReq.class);
        verify(boxFeign).toolUpLevel(reqCaptor.capture());
        assertEquals("2001", reqCaptor.getValue().getRoleId());
        assertEquals(3, reqCaptor.getValue().getToolType());

        Msgwabao.PB_SCWaBaoToolInfo toolInfo = firstMessage(1647, Msgwabao.PB_SCWaBaoToolInfo.class);
        assertEquals(List.of(1, 2), toolInfo.getToolGradeList());
        assertEquals(List.of(3, 4), toolInfo.getToolLevelList());
        assertEquals(List.of(0, 1), toolInfo.getConditionTypeList());
        assertEquals(List.of(5, 6), toolInfo.getConditionNumList());

        Msgwabao.PB_SCWaBaoInfo info = firstMessage(1642, Msgwabao.PB_SCWaBaoInfo.class);
        assertEquals(7, info.getCollectionLevel());
    }

    @Test
    @DisplayName("op 13 forwards toolUpGrade and emits SC 1647/1642")
    void handleToolUpGrade_emitsToolInfoAndInfo() throws Exception {
        when(boxFeign.toolUpGrade(any())).thenReturn(BoxDTOs.OkResp.builder().ok(true).message("ok").build());
        when(boxFeign.getWaBaoToolInfo(2001L)).thenReturn(BoxDTOs.WaBaoToolInfo.builder()
                .toolGrade(List.of(4, 5, 6))
                .toolLevel(List.of(1, 1, 1))
                .conditionType(List.of(2, 2, 2))
                .conditionNum(List.of(0, 0, 0))
                .toolList(List.of())
                .build());

        Msgwabao.PB_CSWaBaoReq req = Msgwabao.PB_CSWaBaoReq.newBuilder()
                .setOpType(13)
                .setParam1(4)
                .build();

        assertDoesNotThrow(() -> waBaoHandler.handle(playerSession, 1640, req.toByteArray()).block());

        ArgumentCaptor<BoxDTOs.ToolUpGradeReq> reqCaptor = ArgumentCaptor.forClass(BoxDTOs.ToolUpGradeReq.class);
        verify(boxFeign).toolUpGrade(reqCaptor.capture());
        assertEquals("2001", reqCaptor.getValue().getRoleId());
        assertEquals(4, reqCaptor.getValue().getToolType());

        Msgwabao.PB_SCWaBaoToolInfo toolInfo = firstMessage(1647, Msgwabao.PB_SCWaBaoToolInfo.class);
        assertEquals(List.of(4, 5, 6), toolInfo.getToolGradeList());
        assertEquals(List.of(0, 0, 0), toolInfo.getConditionNumList());
    }

    @Test
    @DisplayName("op 14 forwards putCollectionBook and emits SC 1650/1642")
    void handlePutCollectionBook_emitsCollectionBookAndInfo() throws Exception {
        when(boxFeign.putCollectionBook(any())).thenReturn(BoxDTOs.OkResp.builder().ok(true).message("ok").build());
        when(boxFeign.getWaBaoCollectionBookInfo(2001L)).thenReturn(BoxDTOs.WaBaoCollectionBookInfo.builder()
                .level(List.of(0, 1, 2, 3))
                .build());

        Msgwabao.PB_CSWaBaoReq req = Msgwabao.PB_CSWaBaoReq.newBuilder()
                .setOpType(14)
                .setParam1(6)
                .build();

        assertDoesNotThrow(() -> waBaoHandler.handle(playerSession, 1640, req.toByteArray()).block());

        ArgumentCaptor<BoxDTOs.PutCollectionBookReq> reqCaptor = ArgumentCaptor.forClass(BoxDTOs.PutCollectionBookReq.class);
        verify(boxFeign).putCollectionBook(reqCaptor.capture());
        assertEquals("2001", reqCaptor.getValue().getRoleId());
        assertEquals(6, reqCaptor.getValue().getHandbookType());

        Msgwabao.PB_SCWaBaoCollectionBookInfo bookInfo = firstMessage(1650, Msgwabao.PB_SCWaBaoCollectionBookInfo.class);
        assertEquals(List.of(0, 1, 2, 3), bookInfo.getLevelList());

        Msgwabao.PB_SCWaBaoInfo info = firstMessage(1642, Msgwabao.PB_SCWaBaoInfo.class);
        assertEquals(7, info.getCollectionLevel());
    }

    @Test
    @DisplayName("op 15 forwards collectionBookLevelUp and emits SC 1651/1642")
    void handleCollectionBookLevelUp_emitsBookListAndInfo() throws Exception {
        when(boxFeign.collectionBookLevelUp(any())).thenReturn(BoxDTOs.OkResp.builder().ok(true).message("ok").build());
        when(boxFeign.getWaBaoBookListInfo(2001L)).thenReturn(BoxDTOs.WaBaoBookListInfo.builder()
                .activateFlagList(List.of(3, 7, 15))
                .build());

        Msgwabao.PB_CSWaBaoReq req = Msgwabao.PB_CSWaBaoReq.newBuilder()
                .setOpType(15)
                .setParam1(9)
                .build();

        assertDoesNotThrow(() -> waBaoHandler.handle(playerSession, 1640, req.toByteArray()).block());

        ArgumentCaptor<BoxDTOs.CollectionBookLevelUpReq> reqCaptor = ArgumentCaptor.forClass(BoxDTOs.CollectionBookLevelUpReq.class);
        verify(boxFeign).collectionBookLevelUp(reqCaptor.capture());
        assertEquals("2001", reqCaptor.getValue().getRoleId());
        assertEquals(9, reqCaptor.getValue().getHandbookType());

        Msgwabao.PB_SCWaBaoBookListInfo bookListInfo = firstMessage(1651, Msgwabao.PB_SCWaBaoBookListInfo.class);
        assertEquals(List.of(3, 7, 15), bookListInfo.getActivateFlagList());

        Msgwabao.PB_SCWaBaoInfo info = firstMessage(1642, Msgwabao.PB_SCWaBaoInfo.class);
        assertEquals(7, info.getCollectionLevel());
    }

    @Test
    @DisplayName("op 16 forwards activateBook and emits SC 1651/1642")
    void handleActivateBook_emitsBookListAndInfo() throws Exception {
        when(boxFeign.activateBook(any())).thenReturn(BoxDTOs.OkResp.builder().ok(true).message("ok").build());
        when(boxFeign.getWaBaoBookListInfo(2001L)).thenReturn(BoxDTOs.WaBaoBookListInfo.builder()
                .activateFlagList(List.of(0, 32, 64))
                .build());

        Msgwabao.PB_CSWaBaoReq req = Msgwabao.PB_CSWaBaoReq.newBuilder()
                .setOpType(16)
                .setParam1(2)
                .setParam2(5)
                .build();

        assertDoesNotThrow(() -> waBaoHandler.handle(playerSession, 1640, req.toByteArray()).block());

        ArgumentCaptor<BoxDTOs.ActivateBookReq> reqCaptor = ArgumentCaptor.forClass(BoxDTOs.ActivateBookReq.class);
        verify(boxFeign).activateBook(reqCaptor.capture());
        assertEquals("2001", reqCaptor.getValue().getRoleId());
        assertEquals(2, reqCaptor.getValue().getOrbMap());
        assertEquals(5, reqCaptor.getValue().getHandbookType());

        Msgwabao.PB_SCWaBaoBookListInfo bookListInfo = firstMessage(1651, Msgwabao.PB_SCWaBaoBookListInfo.class);
        assertEquals(List.of(0, 32, 64), bookListInfo.getActivateFlagList());

        Msgwabao.PB_SCWaBaoInfo info = firstMessage(1642, Msgwabao.PB_SCWaBaoInfo.class);
        assertEquals(7, info.getCollectionLevel());
    }

    @Test
    @DisplayName("pushAll emits the WaBao startup snapshot frames")
    void pushAll_emitsStartupSnapshotFrames() {
        when(boxFeign.getWaBaoCollection(2001L)).thenReturn(BoxDTOs.WaBaoCollectionInfo.builder()
                .isLogin(1)
                .dataList(List.of(BoxDTOs.WaBaoCollectionNode.builder().itemType(0).index(0).itemId(9001).build()))
                .build());
        when(boxFeign.getWaBaoToolInfo(2001L)).thenReturn(BoxDTOs.WaBaoToolInfo.builder()
                .toolGrade(List.of(1))
                .toolLevel(List.of(2))
                .conditionType(List.of(3))
                .conditionNum(List.of(4))
                .toolList(List.of())
                .build());
        when(boxFeign.getWaBaoTaskInfo(2001L)).thenReturn(BoxDTOs.WaBaoTaskInfo.builder()
                .taskFlag(1)
                .taskList(List.of(101))
                .taskTypeNumList(List.of(9))
                .build());
        when(boxFeign.getWaBaoCollectionBookInfo(2001L)).thenReturn(BoxDTOs.WaBaoCollectionBookInfo.builder()
                .level(List.of(5, 6))
                .build());
        when(boxFeign.getWaBaoBookListInfo(2001L)).thenReturn(BoxDTOs.WaBaoBookListInfo.builder()
                .activateFlagList(List.of(7, 8))
                .build());

        assertDoesNotThrow(() -> waBaoHandler.pushAll(playerSession).block());

        List<PacketCodec.Decoded> frames = capturedFrames();
        assertTrue(frames.stream().anyMatch(decoded -> decoded.msgId() == 1643));
        assertTrue(frames.stream().anyMatch(decoded -> decoded.msgId() == 1645));
        assertTrue(frames.stream().anyMatch(decoded -> decoded.msgId() == 1646));
        assertTrue(frames.stream().anyMatch(decoded -> decoded.msgId() == 1647));
        assertTrue(frames.stream().anyMatch(decoded -> decoded.msgId() == 1648));
        assertTrue(frames.stream().anyMatch(decoded -> decoded.msgId() == 1649));
        assertTrue(frames.stream().anyMatch(decoded -> decoded.msgId() == 1650));
        assertTrue(frames.stream().anyMatch(decoded -> decoded.msgId() == 1651));
    }

    private List<PacketCodec.Decoded> capturedFrames() {
        ArgumentCaptor<byte[]> frameCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(outbound, atLeastOnce()).tryEmitNext(frameCaptor.capture());
        return frameCaptor.getAllValues().stream()
                .map(PacketCodec::decode)
                .filter(Objects::nonNull)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private <T> T firstMessage(int msgId, Class<T> type) throws Exception {
        PacketCodec.Decoded decoded = capturedFrames().stream()
                .filter(frame -> frame.msgId() == msgId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected packet msgId=" + msgId));

        if (type == Msgwabao.PB_SCWaBaoCollectionListInfo.class) {
            return (T) Msgwabao.PB_SCWaBaoCollectionListInfo.parseFrom(decoded.payload());
        }
        if (type == Msgwabao.PB_SCWaBaoToolInfo.class) {
            return (T) Msgwabao.PB_SCWaBaoToolInfo.parseFrom(decoded.payload());
        }
        if (type == Msgwabao.PB_SCWaBaoCollectionBookInfo.class) {
            return (T) Msgwabao.PB_SCWaBaoCollectionBookInfo.parseFrom(decoded.payload());
        }
        if (type == Msgwabao.PB_SCWaBaoBookListInfo.class) {
            return (T) Msgwabao.PB_SCWaBaoBookListInfo.parseFrom(decoded.payload());
        }
        if (type == Msgwabao.PB_SCWaBaoInfo.class) {
            return (T) Msgwabao.PB_SCWaBaoInfo.parseFrom(decoded.payload());
        }
        throw new IllegalArgumentException("Unsupported protobuf type: " + type.getName());
    }
}