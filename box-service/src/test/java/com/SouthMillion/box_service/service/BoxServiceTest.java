package com.SouthMillion.box_service.service;

import com.SouthMillion.box_service.config.EquipmentIndex;
import com.SouthMillion.box_service.config.LuckUnpackConfigCache;
import com.SouthMillion.box_service.config.UnpackConfigCache;
import com.SouthMillion.box_service.enity.BoxSetting;
import com.SouthMillion.box_service.enity.BoxState;
import com.SouthMillion.box_service.enity.LuckState;
import com.SouthMillion.box_service.repository.BoxSettingRepository;
import com.SouthMillion.box_service.repository.BoxStateRepository;
import com.SouthMillion.box_service.repository.BoxCompareStateRepository;
import com.SouthMillion.box_service.repository.LuckStateRepository;
import com.SouthMillion.box_service.service.client.BagFeign;
import com.SouthMillion.box_service.service.client.EquipFeign;
import com.SouthMillion.box_service.service.client.ItemMetaFeign;
import com.SouthMillion.box_service.service.client.RoleFeign;
import com.SouthMillion.box_service.service.client.WalletFeign;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.SouthMillion.dto.box.BoxDTOs;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BoxService Tests")
class BoxServiceTest {

    @Mock private BoxStateRepository   boxRepo;
    @Mock private LuckStateRepository  luckRepo;
    @Mock private BoxSettingRepository settingRepo;
    @Mock private BoxCompareStateRepository compareStateRepo;
    @Mock private UnpackConfigCache    unpackCfg;
    @Mock private LuckUnpackConfigCache luckCfg;
    @Mock private EquipmentIndex       equipIdx;
    @Mock private BagFeign             bag;
    @Mock private ItemMetaFeign        itemFeign;
    @Mock private EquipFeign           equipFeign;
    @Mock private RoleFeign            roleFeign;
    @Mock private WalletFeign          walletFeign;
    @Spy  private MeterRegistry        meterRegistry = new SimpleMeterRegistry();

    @InjectMocks private BoxService boxService;

    private static final Long ROLE_ID = 1L;

    @BeforeEach
    void setupDefaults() {
        Mockito.lenient().when(compareStateRepo.find(anyLong())).thenReturn(Optional.empty());
        Mockito.lenient().when(equipIdx.isEquipId(anyInt())).thenReturn(true);
    }

    /** Build a BoxState with typical values and no active level-up or pending. */
    private BoxState buildState() {
        BoxState s = new BoxState();
        s.setRoleId(ROLE_ID);
        s.setBoxLevel(2);
        s.setBoxBuyTimes(3);
        s.setLevelUpEndEpoch(0L);
        s.setLevelFetchFlag(0);
        s.setOpenBoxTotal(10);
        s.setLastOpenIsFive(false);
        s.setPendingJson(null);
        return s;
    }

    /** Build a LuckState with no active event (endEpoch=0). */
    private LuckState buildLuckState(long endEpoch, long bitmap, int snapshotCnt) {
        LuckState ls = new LuckState();
        ls.setRoleId(ROLE_ID);
        ls.setEndEpoch(endEpoch);
        ls.setStartEpoch(0L);
        ls.setReceiveBitmap(bitmap);
        ls.setSnapshotOpenCnt(snapshotCnt);
        return ls;
    }

    private BoxDTOs.BoxCompareStateResp buildCompareState(int itemId, int equipType, boolean isNew) {
        return BoxDTOs.BoxCompareStateResp.builder()
                .roleId(ROLE_ID)
                .status("PENDING_COMPARE")
                .isNew(isNew ? 1 : 0)
                .candidateEquip(BoxDTOs.BoxCompareSnapshotDTO.builder()
                        .itemId(itemId)
                        .equipType(equipType)
                        .quality(4)
                        .equipLevel(1)
                        .hp(100)
                        .attack(50)
                        .defend(20)
                        .speed(10)
                        .attrType1(11)
                        .attrValue1(100)
                        .attrType2(12)
                        .attrValue2(50)
                        .build())
                .build();
    }

                private BoxDTOs.BoxCompareStateResp buildCompareStateWithBefore(int candidateItemId, int candidateEquipType, int beforeItemId, int beforeEquipType) {
                return BoxDTOs.BoxCompareStateResp.builder()
                    .roleId(ROLE_ID)
                    .status("PENDING_COMPARE")
                    .isNew(1)
                    .candidateEquip(BoxDTOs.BoxCompareSnapshotDTO.builder()
                        .itemId(candidateItemId)
                        .equipType(candidateEquipType)
                        .quality(4)
                        .equipLevel(1)
                        .hp(100)
                        .attack(50)
                        .defend(20)
                        .speed(10)
                        .attrType1(11)
                        .attrValue1(100)
                        .attrType2(12)
                        .attrValue2(50)
                        .build())
                    .equippedBefore(BoxDTOs.BoxCompareSnapshotDTO.builder()
                        .itemId(beforeItemId)
                        .equipType(beforeEquipType)
                        .quality(4)
                        .equipLevel(1)
                        .hp(90)
                        .attack(45)
                        .defend(18)
                        .speed(9)
                        .attrType1(11)
                        .attrValue1(90)
                        .attrType2(12)
                        .attrValue2(45)
                        .build())
                    .build();
                }

    private EquipDTOs.EquipItem buildEquipItem(int itemId, int equipType) {
        return EquipDTOs.EquipItem.builder()
                .itemId(itemId)
                .equipType(equipType)
                .hp(100)
                .attack(50)
                .defend(20)
                .speed(10)
                .attrType1(11)
                .attrValue1(100)
                .attrType2(12)
                .attrValue2(50)
                .build();
    }

    private EquipDTOs.EquipItem buildEquipItem(int itemId,
                                               int equipType,
                                               int hp,
                                               int attack,
                                               int defend,
                                               int speed,
                                               int attrType1,
                                               int attrValue1,
                                               int attrType2,
                                               int attrValue2) {
        return EquipDTOs.EquipItem.builder()
                .itemId(itemId)
                .equipType(equipType)
                .hp(hp)
                .attack(attack)
                .defend(defend)
                .speed(speed)
                .attrType1(attrType1)
                .attrValue1(attrValue1)
                .attrType2(attrType2)
                .attrValue2(attrValue2)
                .build();
    }

    private BoxDTOs.EquipRow buildEquipRow(int itemId, int equipType, int quality, int level) {
        return BoxDTOs.EquipRow.builder()
                .id(itemId)
                .part(equipType)
                .quality(quality)
                .level(level)
                .hpMin(100)
                .hpMax(100)
                .attMin(50)
                .attMax(50)
                .defMin(20)
                .defMax(20)
                .speedMin(10)
                .speedMax(10)
                .fristAtt(11)
                .secondAtt(12)
                .build();
    }

    @Test
    @DisplayName("TC-BOX-000 [P] rollQuality() phai ton trong equipment_color_1/2 tu unpack config")
    void rollQuality_usesConfiguredEquipmentColorWeights() {
        Map<String, Object> colorRow = Map.of(
                "equipment_color_1", "0|0|10000|0|0|0|0|0",
                "equipment_color_2", "0|0|0|10000|0|0|0|0"
        );

        for (int i = 0; i < 20; i++) {
            Integer singleOpenQuality = ReflectionTestUtils.invokeMethod(boxService, "rollQuality", colorRow, false);
            Integer fiveOpenQuality = ReflectionTestUtils.invokeMethod(boxService, "rollQuality", colorRow, true);
            assertThat(singleOpenQuality).isEqualTo(3);
            assertThat(fiveOpenQuality).isEqualTo(4);
        }
    }

    // =========================================================
    // info()
    // =========================================================
    @Nested
    @DisplayName("info()")
    class Info {

        @Test
        @DisplayName("TC-BOX-001 [P] State ton tai, khong co pending – tra ve InfoResp chinh xac")
        void info_existingState_returnsInfoResp() {
            BoxState state = buildState();
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));

            BoxDTOs.InfoResp result = boxService.info(ROLE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getBoxLevel()).isEqualTo(2);
            assertThat(result.getBoxBuyTimes()).isEqualTo(3);
            assertThat(result.getOpenBoxTotal()).isEqualTo(10);
            assertThat(result.getPending()).isNull();
        }

        @Test
        @DisplayName("TC-BOX-002 [P] State chua ton tai – tao state mac dinh voi boxLevel=1")
        void info_noExistingState_createsDefaultAndReturns() {
            BoxState defaultState = new BoxState();
            defaultState.setRoleId(ROLE_ID);
            defaultState.setBoxLevel(1);
            defaultState.setBoxBuyTimes(0);
            defaultState.setOpenBoxTotal(0);
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(defaultState));

            BoxDTOs.InfoResp result = boxService.info(ROLE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getBoxLevel()).isGreaterThanOrEqualTo(1);
            assertThat(result.getBoxBuyTimes()).isEqualTo(0);
            then(boxRepo).should().insertDefaultIfAbsent(ROLE_ID);
        }
    }

    // =========================================================
    // wear()
    // =========================================================
    @Nested
    @DisplayName("wear()")
    class Wear {

        @Test
        @DisplayName("TC-BOX-003 [N] Khong co pending equip – tra ve OkResp(false, NO_PENDING)")
        void wear_noPendingEquip_returnsNoPending() {
            BoxState state = buildState(); // pendingJson = null
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));

            BoxDTOs.OkResp result = boxService.wear(ROLE_ID);

            assertThat(result.isOk()).isFalse();
            assertThat(result.getMessage()).isEqualTo("NO_PENDING");
        }

        @Test
        @DisplayName("TC-BOX-004 [P] Co pending equip hop le – them item vao bag va xoa pending")
        void wear_hasPendingEquip_addsItemToBagAndClearsPending() {
            BoxState state = buildState();
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.of(buildCompareState(100, 1, true)));
            given(equipFeign.wearFromBox(any())).willReturn(EquipDTOs.WearFromBoxResp.builder().build());

            BoxDTOs.OkResp result = boxService.wear(ROLE_ID);

            assertThat(result.isOk()).isTrue();
            assertThat(result.getMessage()).isEqualTo("OK");
            then(compareStateRepo).should().delete(ROLE_ID);
        }

        @Test
        @DisplayName("TC-BOX-004A [P] Replaced item trung candidate va cung snapshot – khong swap compare")
        void wear_replacedEqualsCandidate_skipsAddAndClearsPending() {
            BoxState state = buildState();
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.of(buildCompareState(100, 1, true)));
            given(equipFeign.wearFromBox(any())).willReturn(
                    EquipDTOs.WearFromBoxResp.builder()
                    .replaced(EquipDTOs.ReplacedEquip.builder()
                        .itemId(100)
                        .equipType(1)
                        .quality(4)
                        .equipLevel(1)
                        .hp(100)
                        .attack(50)
                        .defend(20)
                        .speed(10)
                        .attrType1(11)
                        .attrValue1(100)
                        .attrType2(12)
                        .attrValue2(50)
                        .build())
                            .build());

            BoxDTOs.OkResp result = boxService.wear(ROLE_ID);

            assertThat(result.isOk()).isTrue();
            assertThat(result.getMessage()).isEqualTo("OK");
            then(bag).should(never()).add(any());
            then(compareStateRepo).should().delete(ROLE_ID);
        }

        @Test
        @DisplayName("TC-BOX-004A1 [P] Replaced item trung itemId nhung khac stats – van mac mon moi va auto-sell mon cu")
        void wear_replacedSameItemIdButDifferentStats_createsSwappedPendingCompare() {
            BoxState state = buildState();
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.of(buildCompareStateWithBefore(100, 1, 100, 1)));
            given(equipFeign.wearFromBox(any())).willReturn(
                EquipDTOs.WearFromBoxResp.builder()
                    .replaced(EquipDTOs.ReplacedEquip.builder()
                        .itemId(100)
                        .equipType(1)
                        .quality(4)
                        .equipLevel(1)
                        .hp(90)
                        .attack(45)
                        .defend(18)
                        .speed(9)
                        .attrType1(11)
                        .attrValue1(90)
                        .attrType2(12)
                        .attrValue2(45)
                        .build())
                    .build());
            given(equipFeign.computeSell(any())).willReturn(Map.of("coin", 88L, "exp", 11L));

            BoxDTOs.OkResp result = boxService.wear(ROLE_ID);

            assertThat(result.isOk()).isTrue();
            assertThat(result.getMessage()).isEqualTo("OK");
            then(compareStateRepo).should().delete(ROLE_ID);
            then(walletFeign).should().batchAdd(any());
            then(roleFeign).should().addExp(any());
        }

        @Test
        @DisplayName("TC-BOX-004B [P] Replaced item duoc auto-sell sau khi mac mon moi")
        void wear_replacedCreatesSwappedPendingCompare() {
            BoxState state = buildState();
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.of(buildCompareState(100, 1, true)));
            given(equipFeign.wearFromBox(any())).willReturn(
                    EquipDTOs.WearFromBoxResp.builder()
                            .replaced(EquipDTOs.ReplacedEquip.builder().itemId(200).equipType(1).build())
                            .build());
            given(equipFeign.computeSell(any())).willReturn(Map.of("coin", 55L, "exp", 9L));

            BoxDTOs.OkResp result = boxService.wear(ROLE_ID);

            assertThat(result.isOk()).isTrue();
            assertThat(result.getMessage()).isEqualTo("OK");
            then(bag).should(never()).add(any());
            then(compareStateRepo).should().delete(ROLE_ID);
            then(walletFeign).should().batchAdd(any());
            then(roleFeign).should().addExp(any());
        }

        @Test
        @DisplayName("TC-BOX-004C [P] Mac mon moi thi dong flow compare va auto-sell mon cu")
        void wear_swapCompareUsesActualEquippedSnapshotAfterWear() {
            BoxState state = buildState();

            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.of(buildCompareState(100, 1, true)));
            given(equipFeign.wearFromBox(any())).willReturn(
                EquipDTOs.WearFromBoxResp.builder()
                    .replaced(EquipDTOs.ReplacedEquip.builder()
                        .itemId(200)
                        .equipType(1)
                        .quality(4)
                        .equipLevel(1)
                        .hp(90)
                        .attack(45)
                        .defend(18)
                        .speed(9)
                        .attrType1(11)
                        .attrValue1(90)
                        .attrType2(12)
                        .attrValue2(45)
                        .build())
                    .build());
            given(equipFeign.computeSell(any())).willReturn(Map.of("coin", 101L, "exp", 22L));

            BoxDTOs.OkResp result = boxService.wear(ROLE_ID);

            assertThat(result.isOk()).isTrue();
            then(compareStateRepo).should().delete(ROLE_ID);
            then(compareStateRepo).should(never()).save(any());
            then(walletFeign).should().batchAdd(any());
            then(roleFeign).should().addExp(any());
        }

        @Test
        @DisplayName("TC-BOX-004D [P] Redis compare-state miss nhung DB con pendingJson hop le thi van mac duoc")
        void wear_missingCompareState_usesPendingJsonFromDb() {
            BoxState state = buildState();
            state.setPendingJson("{\"kind\":\"equip\",\"itemId\":100,\"equipType\":1,\"quality\":4,\"equipLevel\":1,\"hp\":100,\"attack\":50,\"defend\":20,\"speed\":10,\"isNew\":true}");
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.empty());
            given(equipFeign.wearFromBox(any())).willReturn(EquipDTOs.WearFromBoxResp.builder().build());

            BoxDTOs.OkResp result = boxService.wear(ROLE_ID);

            assertThat(result.isOk()).isTrue();
            assertThat(result.getMessage()).isEqualTo("OK");
            assertThat(state.getPendingJson()).isNull();
            then(compareStateRepo).should().delete(ROLE_ID);
        }
    }

    @Nested
    @DisplayName("open()")
    class Open {

        @BeforeEach
        void setupOpenDefaults() {
            ReflectionTestUtils.setField(boxService, "unpackItemIdFallback", 5000);
            Mockito.lenient().when(bag.consume(any())).thenReturn(org.SouthMillion.dto.bag.BagOkResp.builder().succeeded(true).build());
            Mockito.lenient().when(unpackCfg.other()).thenReturn(List.of(Map.of("unpack_item_id", "5000")));
            Mockito.lenient().when(unpackCfg.fixedReward()).thenReturn(List.of(Map.of("box_oder", "11", "item_id", "100")));
            Mockito.lenient().when(unpackCfg.shizhuangRate()).thenReturn(List.of());
            Mockito.lenient().when(equipIdx.isEquipId(100)).thenReturn(true);
            Mockito.lenient().when(equipIdx.findPQLById(100)).thenReturn(Optional.of(new int[]{1, 4}));
            Mockito.lenient().when(equipIdx.resolve(1, 4, 1)).thenReturn(Optional.of(100));
            Mockito.lenient().when(equipIdx.rowOf(100)).thenReturn(Optional.of(buildEquipRow(100, 1, 4, 1)));
            Mockito.lenient().when(equipIdx.statsJsonOf(100)).thenReturn(Optional.empty());
            Mockito.lenient().when(equipIdx.allFieldsCanonicalOf(100)).thenReturn(Optional.empty());
            Mockito.lenient().when(equipIdx.allFieldsRawStringsOf(100)).thenReturn(Optional.empty());
            Mockito.lenient().when(equipIdx.getIdxPreferred()).thenReturn(Map.of());
        }

        @Test
        @DisplayName("TC-BOX-026 [P] Open theo flow cu – luu pendingJson va khong tao compare-state")
        void open_legacyFlow_storesPendingWithoutCompareState() {
            BoxState state = buildState();
            given(boxRepo.findByRoleIdForUpdate(ROLE_ID)).willReturn(Optional.of(state));
            given(boxRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(equipFeign.list(String.valueOf(ROLE_ID)))
                    .willReturn(new EquipDTOs.ListResp(List.of(
                            EquipDTOs.EquipItem.builder().equipType(1).itemId(200).build())));

            BoxDTOs.OpenReq req = new BoxDTOs.OpenReq();
            req.setRoleId(String.valueOf(ROLE_ID));
            req.setCount(1);
            req.setRoleLevel(1);

            BoxDTOs.OpenResp result = boxService.open(req);

            assertThat(result).isNotNull();
            assertThat(result.getCompareState()).isNotNull();
            assertThat(result.getCompareState().getCandidateEquip()).isNotNull();
            assertThat(result.getCompareState().getCandidateEquip().getItemId()).isEqualTo(100);
            assertThat(result.getCompareState().getEquippedBefore()).isNotNull();
            assertThat(result.getCompareState().getEquippedBefore().getItemId()).isEqualTo(200);
            assertThat(result.getCompareState().getStatus()).isEqualTo("PENDING_COMPARE");
            assertThat(result.getPending()).isNotNull();
            assertThat(result.getPending().get("itemId")).isEqualTo(100);

            ArgumentCaptor<BoxState> stateCaptor = ArgumentCaptor.forClass(BoxState.class);
            then(boxRepo).should(atLeastOnce()).save(stateCaptor.capture());
            assertThat(stateCaptor.getAllValues()).anyMatch(saved -> saved.getPendingJson() != null && saved.getPendingJson().contains("\"itemId\":100"));
        }

        @Test
        @DisplayName("TC-BOX-026A [P] Open phai uu tien snapshot nhanh va khong goi list full-equip neu snapshot da co du lieu")
        void open_prefersSnapshotAndSkipsListWhenAvailable() {
            BoxState state = buildState();
            given(boxRepo.findByRoleIdForUpdate(ROLE_ID)).willReturn(Optional.of(state));
            given(boxRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(equipFeign.snapshot(ROLE_ID, 1)).willReturn(buildEquipItem(200, 1));

            BoxDTOs.OpenReq req = new BoxDTOs.OpenReq();
            req.setRoleId(String.valueOf(ROLE_ID));
            req.setCount(1);
            req.setRoleLevel(1);

            BoxDTOs.OpenResp result = boxService.open(req);

            assertThat(result).isNotNull();
            assertThat(result.getCompareState()).isNotNull();
            assertThat(result.getCompareState().getEquippedBefore()).isNotNull();
            assertThat(result.getCompareState().getEquippedBefore().getItemId()).isEqualTo(200);
            then(equipFeign).should(never()).list(String.valueOf(ROLE_ID));
        }

        @Test
        @DisplayName("TC-BOX-026B [P] Open phai dung hinted roleLevel, khong goi role-service detail tren hot path")
        void open_usesHintedRoleLevelWithoutCallingRoleService() {
            BoxState state = buildState();
            given(boxRepo.findByRoleIdForUpdate(ROLE_ID)).willReturn(Optional.of(state));
            given(boxRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            Mockito.lenient().when(roleFeign.detail(String.valueOf(ROLE_ID)))
                    .thenThrow(new AssertionError("role.detail should not be called when roleLevel hint is present"));

            BoxDTOs.OpenReq req = new BoxDTOs.OpenReq();
            req.setRoleId(String.valueOf(ROLE_ID));
            req.setCount(1);
            req.setRoleLevel(7);

            BoxDTOs.OpenResp result = boxService.open(req);

            assertThat(result).isNotNull();
            then(roleFeign).should(never()).detail(String.valueOf(ROLE_ID));
        }

        @Test
        @DisplayName("TC-BOX-026C [P] Compare lookup cham thi open van tiep tuc voi trang thai incomplete")
        void open_slowCompareLookupFallsBackWithoutBlocking() {
            BoxState state = buildState();
            ReflectionTestUtils.setField(boxService, "compareLookupTimeoutMs", 10L);
            given(boxRepo.findByRoleIdForUpdate(ROLE_ID)).willReturn(Optional.of(state));
            given(boxRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(equipFeign.snapshot(ROLE_ID, 1)).willAnswer(inv -> {
                try {
                    Thread.sleep(80L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return buildEquipItem(200, 1);
            });

            BoxDTOs.OpenReq req = new BoxDTOs.OpenReq();
            req.setRoleId(String.valueOf(ROLE_ID));
            req.setCount(1);
            req.setRoleLevel(1);

            long startedAt = System.currentTimeMillis();
            BoxDTOs.OpenResp result = boxService.open(req);
            long elapsedMs = System.currentTimeMillis() - startedAt;

            assertThat(result).isNotNull();
            assertThat(result.getCompareState()).isNotNull();
            assertThat(result.getCompareState().getStatus()).isEqualTo("PENDING_COMPARE_INCOMPLETE");
            assertThat(elapsedMs).isLessThan(500L);
        }

        @Test
        @DisplayName("TC-BOX-027 [P] Open theo flow cu khong con dung snapshot/list current equip de compare")
        void open_legacyFlow_doesNotCreateCompareStateWhenCurrentEquipLookupFails() {
            BoxState state = buildState();
            given(boxRepo.findByRoleIdForUpdate(ROLE_ID)).willReturn(Optional.of(state));
            given(boxRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            BoxDTOs.OpenReq req = new BoxDTOs.OpenReq();
            req.setRoleId(String.valueOf(ROLE_ID));
            req.setCount(1);
            req.setRoleLevel(1);

            BoxDTOs.OpenResp result = boxService.open(req);

            assertThat(result).isNotNull();
            assertThat(result.getCompareState()).isNotNull();
            assertThat(result.getCompareState().getCandidateEquip()).isNotNull();
            assertThat(result.getCompareState().getCandidateEquip().getItemId()).isEqualTo(100);
            assertThat(result.getCompareState().getEquippedBefore()).isNull();
            assertThat(result.getCompareState().getStatus()).isEqualTo("PENDING_COMPARE_INCOMPLETE");
            assertThat(result.getPending()).isNotNull();
            assertThat(result.getPending().get("itemId")).isEqualTo(100);
        }

        @Test
        @DisplayName("TC-BOX-028 [P] Redis compare-state miss thi fallback sang DB pendingJson, khong nuot pending va khong mo hop moi")
        void open_missingCompareState_usesPendingJsonFromDb() {
            BoxState state = buildState();
            state.setPendingJson("{\"kind\":\"equip\",\"itemId\":100,\"equipType\":1,\"quality\":4,\"equipLevel\":1,\"hp\":100,\"attack\":50,\"defend\":20,\"speed\":10,\"isNew\":true}");
            given(boxRepo.findByRoleIdForUpdate(ROLE_ID)).willReturn(Optional.of(state));
            given(boxRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.empty());

            BoxDTOs.OpenReq req = new BoxDTOs.OpenReq();
            req.setRoleId(String.valueOf(ROLE_ID));
            req.setCount(1);
            req.setRoleLevel(1);

            BoxDTOs.OpenResp result = boxService.open(req);

            assertThat(result).isNotNull();
            assertThat(result.getOpenEquip()).isNull();
            assertThat(result.getPending()).isNotNull();
            assertThat(result.getPending().get("itemId")).isEqualTo(100);
            then(bag).should(never()).consume(any());
        }

        @Test
        @DisplayName("TC-BOX-029 [P] Compare-state item loi/stale thi phai tu clear de khong khoa mo box")
        void open_invalidCompareStateCandidate_clearsStateAndContinues() {
            BoxState state = buildState();
            state.setPendingJson("{\"kind\":\"equip\",\"itemId\":1,\"equipType\":1,\"quality\":4,\"equipLevel\":1}");
            given(boxRepo.findByRoleIdForUpdate(ROLE_ID)).willReturn(Optional.of(state));
            given(boxRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.of(
                    BoxDTOs.BoxCompareStateResp.builder()
                            .roleId(ROLE_ID)
                            .status("PENDING_COMPARE")
                            .candidateEquip(BoxDTOs.BoxCompareSnapshotDTO.builder()
                                    .itemId(1)
                                    .equipType(1)
                                    .quality(4)
                                    .equipLevel(1)
                                    .build())
                            .build()));
            BoxDTOs.OpenReq req = new BoxDTOs.OpenReq();
            req.setRoleId(String.valueOf(ROLE_ID));
            req.setCount(1);
            req.setRoleLevel(1);

            BoxDTOs.OpenResp result = boxService.open(req);

            assertThat(result).isNotNull();
            assertThat(result.getOpenEquip()).isNotNull();
            assertThat(result.getOpenEquip().getItemId()).isEqualTo(100);
            assertThat(result.getPending()).isNotNull();
            assertThat(result.getPending().get("itemId")).isEqualTo(100);
            then(compareStateRepo).should().delete(ROLE_ID);
            then(bag).should().consume(any());
        }

        @Test
        @DisplayName("TC-BOX-029A [P] getCompareState phai reject itemId=1 du index tra nham la equip")
        void getCompareState_itemIdOneAlwaysClearsAsInvalid() {
            BoxState state = buildState();
            state.setPendingJson("{\"kind\":\"equip\",\"itemId\":1,\"equipType\":1,\"quality\":4,\"equipLevel\":1}");
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(boxRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.of(
                    BoxDTOs.BoxCompareStateResp.builder()
                            .roleId(ROLE_ID)
                            .status("PENDING_COMPARE")
                            .candidateEquip(BoxDTOs.BoxCompareSnapshotDTO.builder()
                                    .itemId(1)
                                    .equipType(1)
                                    .quality(4)
                                    .equipLevel(1)
                                    .build())
                            .build()));
            BoxDTOs.BoxCompareStateResp result = boxService.getCompareState(ROLE_ID);

            assertThat(result).isNull();
            assertThat(state.getPendingJson()).isNull();
            then(compareStateRepo).should().delete(ROLE_ID);
        }
    }

    // =========================================================
    // sell()
    // =========================================================
    @Nested
    @DisplayName("sell()")
    class Sell {

        @Test
        @DisplayName("TC-BOX-005 [P] Ban equip – xoa pendingJson va tra ve OK")
        void sell_clearsPendingJsonAndReturnsOk() {
            BoxState state = buildState();
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.of(buildCompareState(100, 1, true)));
            given(equipFeign.computeSell(any())).willReturn(Map.of("coin", 123L, "exp", 45L));
            ArgumentCaptor<BoxState> captor = ArgumentCaptor.forClass(BoxState.class);
            given(boxRepo.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

            BoxDTOs.SellResp result = boxService.sell(ROLE_ID);

            assertThat(result.isOk()).isTrue();
            assertThat(result.getMessage()).isEqualTo("OK");
            assertThat(result.getSellCoin()).isEqualTo(123L);
            assertThat(result.getSellExp()).isEqualTo(45L);
            then(compareStateRepo).should().delete(ROLE_ID);
            assertThat(captor.getValue().isLastOpenIsFive()).isFalse();
        }
    }

    // =========================================================
    // buy()
    // =========================================================
    @Nested
    @DisplayName("buy()")
    class Buy {

        @Test
        @DisplayName("TC-BOX-006 [P] Mua luot mo – boxBuyTimes tang 1")
        void buy_incrementsBoxBuyTimes() {
            BoxState state = buildState(); // boxBuyTimes=3
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            ArgumentCaptor<BoxState> captor = ArgumentCaptor.forClass(BoxState.class);
            given(boxRepo.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

            BoxDTOs.OkResp result = boxService.buy(ROLE_ID);

            assertThat(result.isOk()).isTrue();
            assertThat(captor.getValue().getBoxBuyTimes()).isEqualTo(4);
        }
    }

    // =========================================================
    // levelUp()
    // =========================================================
    @Nested
    @DisplayName("levelUp()")
    class LevelUp {

        @Test
        @DisplayName("TC-BOX-007 [P] Nang cap hop – dat timer 60 giay va tra ve UPGRADING")
        void levelUp_setsTimerAndReturnsUpgrading() {
            BoxState state = buildState();
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            ArgumentCaptor<BoxState> captor = ArgumentCaptor.forClass(BoxState.class);
            given(boxRepo.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

            BoxDTOs.OkResp result = boxService.levelUp(ROLE_ID);

            assertThat(result.isOk()).isTrue();
            assertThat(result.getMessage()).isEqualTo("UPGRADING");
            long now = Instant.now().getEpochSecond();
            assertThat(captor.getValue().getLevelUpEndEpoch())
                    .isBetween(now + 55, now + 65); // ~60 seconds from now
        }
    }

    // =========================================================
    // levelReward()
    // =========================================================
    @Nested
    @DisplayName("levelReward()")
    class LevelReward {

        @Test
        @DisplayName("TC-BOX-008 [P] Nhan thuong cap idx=0 – bat bit 0 cua levelFetchFlag")
        void levelReward_idx0_setsBit0() {
            BoxState state = buildState(); // levelFetchFlag=0
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            ArgumentCaptor<BoxState> captor = ArgumentCaptor.forClass(BoxState.class);
            given(boxRepo.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

            BoxDTOs.OkResp result = boxService.levelReward(ROLE_ID, 0);

            assertThat(result.isOk()).isTrue();
            assertThat(captor.getValue().getLevelFetchFlag() & 1).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-BOX-009 [B] idx=31 (vuot gioi han 30) – bi cap lai thanh bit 30")
        void levelReward_idx31_cappedToIdx30() {
            BoxState state = buildState();
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            ArgumentCaptor<BoxState> captor = ArgumentCaptor.forClass(BoxState.class);
            given(boxRepo.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

            boxService.levelReward(ROLE_ID, 31); // 31 > 30 → capped to 30

            // Bit 30 should be set: 1 << 30 = 1073741824
            assertThat(captor.getValue().getLevelFetchFlag() & (1 << 30)).isEqualTo(1 << 30);
        }

        @Test
        @DisplayName("TC-BOX-010 [P] Nhan nhieu thuong – levelFetchFlag tich luy bit")
        void levelReward_multipleCalls_accumulatesBits() {
            BoxState state = buildState();
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(boxRepo.save(any())).willAnswer(inv -> {
                BoxState saved = inv.getArgument(0);
                state.setLevelFetchFlag(saved.getLevelFetchFlag()); // simulate persistence
                return saved;
            });

            boxService.levelReward(ROLE_ID, 0); // bit 0
            boxService.levelReward(ROLE_ID, 2); // bit 2

            assertThat(state.getLevelFetchFlag() & 0b101).isEqualTo(0b101);
        }
    }

    // =========================================================
    // luckInfo()
    // =========================================================
    @Nested
    @DisplayName("luckInfo()")
    class LuckInfo {

        @Test
        @DisplayName("TC-BOX-011 [P] LuckState ton tai – tinh openBoxNumDelta = openBoxTotal - snapshotCnt")
        void luckInfo_luckStateExists_returnsDeltaCorrectly() {
            LuckState ls = buildLuckState(0L, 0L, 5); // snapshotCnt=5
            BoxState state = buildState(); // openBoxTotal=10
            given(luckRepo.findById(ROLE_ID)).willReturn(Optional.of(ls));
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));

            BoxDTOs.LuckInfoResp result = boxService.luckInfo(ROLE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getOpenBoxNumDelta()).isEqualTo(5); // 10 - 5
            assertThat(result.getBoxLevel()).isEqualTo(2);
            assertThat(result.getEndTimestamp()).isEqualTo(0L);
        }

        @Test
        @DisplayName("TC-BOX-012 [P] LuckState chua ton tai – tao snapshot moi, delta = 0")
        void luckInfo_noLuckState_createsSnapshotAndReturnsDeltaZero() {
            BoxState state = buildState(); // openBoxTotal=10
            given(luckRepo.findById(ROLE_ID)).willReturn(Optional.empty());
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(luckRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            BoxDTOs.LuckInfoResp result = boxService.luckInfo(ROLE_ID);

            assertThat(result).isNotNull();
            // snapshotOpenCnt = openBoxTotal = 10, delta = 10 - 10 = 0
            assertThat(result.getOpenBoxNumDelta()).isEqualTo(0);
            then(luckRepo).should().save(any());
        }
    }

    // =========================================================
    // luckReceive()
    // =========================================================
    @Nested
    @DisplayName("luckReceive()")
    class LuckReceive {

        @Test
        @DisplayName("TC-BOX-013 [N] Su kien da ket thuc (endEpoch <= now) – tra ve LUCK_ENDED")
        void luckReceive_luckEnded_returnsLuckEnded() {
            long pastEpoch = Instant.now().getEpochSecond() - 100; // 100s ago
            LuckState ls = buildLuckState(pastEpoch, 0L, 0);
            given(luckRepo.findById(ROLE_ID)).willReturn(Optional.of(ls));

            BoxDTOs.OkResp result = boxService.luckReceive(ROLE_ID, 1);

            assertThat(result.isOk()).isFalse();
            assertThat(result.getMessage()).isEqualTo("LUCK_ENDED");
        }

        @Test
        @DisplayName("TC-BOX-014 [P] Su kien con hoat dong – dat bit nhan thuong va tra ve OK")
        void luckReceive_luckActive_setsBitmapAndReturnsOk() {
            // endEpoch=0 → condition (endEpoch>0 && endEpoch<=now) is false → NOT ended
            LuckState ls = buildLuckState(0L, 0L, 0);
            BoxState state = buildState();
            given(luckRepo.findById(ROLE_ID)).willReturn(Optional.of(ls));
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(luckCfg.reward()).willReturn(List.of(
                    Map.of(
                            "type", 3,
                            "type_box_num", 2,
                            "type_num", 2,
                            "reward_item", Map.of("item_id", 1000, "num", 1)
                    )
            ));
            given(equipIdx.isEquipId(1000)).willReturn(false); // item 1000 not equip
                given(itemFeign.batchMeta(any())).willReturn(
                    Map.of(1000, Map.of("isVirtual", 0))); // non-virtual
            given(luckRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            BoxDTOs.OkResp result = boxService.luckReceive(ROLE_ID, 3);

            assertThat(result.isOk()).isTrue();
            assertThat(result.getMessage()).isEqualTo("OK");
            then(bag).should().add(any());
            ArgumentCaptor<LuckState> captor = ArgumentCaptor.forClass(LuckState.class);
            then(luckRepo).should().save(captor.capture());
            long expectedBit = 1L << 3; // bit 3 set
            assertThat(captor.getValue().getReceiveBitmap() & expectedBit).isEqualTo(expectedBit);
        }
    }

    // =========================================================
    // quicken()
    // =========================================================
    @Nested
    @DisplayName("quicken()")
    class Quicken {

        @Test
        @DisplayName("TC-BOX-015 [N] Khong co levelUp dang hoat dong – tra ve 'No level up'")
        void quicken_noActiveLevelUp_returnsNoLevelUp() {
            BoxState state = buildState(); // levelUpEndEpoch=0
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));

            BoxDTOs.OkResp result = boxService.quicken(ROLE_ID, 2);

            assertThat(result.isOk()).isFalse();
            assertThat(result.getMessage()).isEqualTo("No level up");
        }

        @Test
        @DisplayName("TC-BOX-016 [P] Giam toc du de hoan thanh – tra ve 'Quickened to completion'")
        void quicken_reducesToCompletion_completesLevelUp() {
            BoxState state = buildState();
            long future = Instant.now().getEpochSecond() + 100; // 100s from now
            state.setLevelUpEndEpoch(future);
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(boxRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            // unpackCfg.other() returns empty list → quickId=0, secPerItem=60 (default)
            given(unpackCfg.other()).willReturn(List.of());

            // num=2, secPerItem=60, reduce=120, newEnd = future-120 = now+100-120 = now-20 <= now → done
            BoxDTOs.OkResp result = boxService.quicken(ROLE_ID, 2);

            assertThat(result.isOk()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Quickened to completion");
        }

        @Test
        @DisplayName("TC-BOX-017 [P] Giam toc mot phan – tra ve 'Quickened'")
        void quicken_reducesPartially_returnsQuickened() {
            BoxState state = buildState();
            long future = Instant.now().getEpochSecond() + 300; // 300s from now
            state.setLevelUpEndEpoch(future);
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(boxRepo.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(unpackCfg.other()).willReturn(List.of());

            // num=1, secPerItem=60, reduce=60, newEnd = future-60 = now+240 > now → partial
            BoxDTOs.OkResp result = boxService.quicken(ROLE_ID, 1);

            assertThat(result.isOk()).isTrue();
            assertThat(result.getMessage()).isEqualTo("Quickened");
        }
    }

    // =========================================================
    // getSetting()
    // =========================================================
    @Nested
    @DisplayName("getSetting()")
    class GetSetting {

        @Test
        @DisplayName("TC-BOX-018 [P] Setting chua ton tai – tao moi voi equipCapMark=1")
        void getSetting_notFound_createsDefaultWithEquipCapMark1() {
            BoxSetting saved = new BoxSetting();
            saved.setRoleId(ROLE_ID);
            saved.setEquipCapMark(1);
            given(settingRepo.findById(ROLE_ID)).willReturn(Optional.of(saved));

            BoxDTOs.BoxSettingResp result = boxService.getSetting(ROLE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getEquipCapMark()).isEqualTo(1);
            then(settingRepo).should().insertDefaultIfAbsent(ROLE_ID);
        }

        @Test
        @DisplayName("TC-BOX-019 [P] Setting da ton tai – tra ve setting hien tai")
        void getSetting_found_returnsExistingSetting() {
            BoxSetting setting = new BoxSetting();
            setting.setRoleId(ROLE_ID);
            setting.setEquipCapMark(1);
            setting.setOpenFiveMark(1);
            setting.setEquipSellMark(0);
            given(settingRepo.findById(ROLE_ID)).willReturn(Optional.of(setting));

            BoxDTOs.BoxSettingResp result = boxService.getSetting(ROLE_ID);

            assertThat(result.getEquipCapMark()).isEqualTo(1);
            assertThat(result.getOpenFiveMark()).isEqualTo(1);
        }
    }

    // =========================================================
    // saveSetting()
    // =========================================================
    @Nested
    @DisplayName("saveSetting()")
    class SaveSetting {

        @Test
        @DisplayName("TC-BOX-020 [P] Luu setting – cap nhat tat ca truong va tra ve SettingResp")
        void saveSetting_savesAllFields() {
            BoxSetting existing = new BoxSetting();
            existing.setRoleId(ROLE_ID);
            existing.setEquipCapMark(1);
            given(settingRepo.findById(ROLE_ID)).willReturn(Optional.of(existing));
            given(settingRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            BoxDTOs.BoxSettingResp req = BoxDTOs.BoxSettingResp.builder()
                    .equipEqality(2)
                    .openFiveMark(1)
                    .equipCapMark(0)
                    .equipSellMark(1)
                    .conditionFirst1(3)
                    .conditionFirst2(4)
                    .conditionSecond1(5)
                    .conditionSecond2(6)
                    .conditionFirstMark(1)
                    .conditionSecondMark(0)
                    .retainMark(1)
                    .challengeMark(0)
                    .build();

            BoxDTOs.BoxSettingResp result = boxService.saveSetting(ROLE_ID, req);

            assertThat(result.getEquipEqality()).isEqualTo(2);
            assertThat(result.getOpenFiveMark()).isEqualTo(1);
            assertThat(result.getEquipCapMark()).isEqualTo(0);
            assertThat(result.getConditionFirst1()).isEqualTo(3);
            assertThat(result.getRetainMark()).isEqualTo(1);
            then(settingRepo).should().insertDefaultIfAbsent(ROLE_ID);
        }
    }

    // =========================================================
    // decompose()
    // =========================================================
    @Nested
    @DisplayName("decompose()")
    class Decompose {

        @Test
        @DisplayName("TC-BOX-021 [N] Khong co pending equip – tra ve loi 'No pending equip'")
        void decompose_noPending_returnsError() {
            BoxState state = buildState(); // pendingJson=null
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.empty());

            BoxDTOs.DecomposeResp result = boxService.decompose(ROLE_ID);

            assertThat(result.isOk()).isFalse();
            assertThat(result.getMessage()).isEqualTo("No pending equip");
        }

        @Test
        @DisplayName("TC-BOX-022 [P] Co pending equip, lastOpenIsFive=false – tra ve ket qua phan giai")
        void decompose_hasPendingEquip_returnsDecomposeResult() {
            BoxState state = buildState();
            state.setLastOpenIsFive(false);
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.of(buildCompareState(100, 1, true)));
            given(equipFeign.decompose(any())).willReturn(Map.of("itemId", 50, "num", 2, "exp", 100));
            given(boxRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            BoxDTOs.DecomposeResp result = boxService.decompose(ROLE_ID);

            assertThat(result.isOk()).isTrue();
            assertThat(result.getGotItemId()).isEqualTo(50L);
            assertThat(result.getGotNum()).isEqualTo(2L);
            assertThat(result.getGotExp()).isEqualTo(100L);
        }

        @Test
        @DisplayName("TC-BOX-023 [P] lastOpenIsFive=true, isNew=true – nhan x5 vat lieu va exp")
        void decompose_lastOpenIsFiveAndIsNew_multipliesBy5() {
            BoxState state = buildState();
            state.setLastOpenIsFive(true);
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.of(buildCompareState(100, 1, true)));
            given(equipFeign.decompose(any())).willReturn(Map.of("itemId", 50, "num", 2, "exp", 100));
            given(boxRepo.save(any())).willAnswer(inv -> inv.getArgument(0));

            BoxDTOs.DecomposeResp result = boxService.decompose(ROLE_ID);

            assertThat(result.isOk()).isTrue();
            assertThat(result.getGotNum()).isEqualTo(10L);  // 2 * 5
            assertThat(result.getGotExp()).isEqualTo(500L); // 100 * 5
        }

        @Test
        @DisplayName("TC-BOX-024 [N] EquipFeign nem exception – tra ve 'Decompose compute failed'")
        void decompose_equipFeignThrows_returnsComputeFailed() {
            BoxState state = buildState();
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.of(buildCompareState(100, 1, true)));
            given(equipFeign.decompose(any())).willThrow(new RuntimeException("Service unavailable"));

            BoxDTOs.DecomposeResp result = boxService.decompose(ROLE_ID);

            assertThat(result.isOk()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Decompose compute failed");
        }

        @Test
        @DisplayName("TC-BOX-025 [P] Phan giai thanh cong – xoa pendingJson va reset lastOpenIsFive")
        void decompose_success_clearsPendingAndResetFlag() {
            BoxState state = buildState();
            state.setLastOpenIsFive(true);
            given(boxRepo.findById(ROLE_ID)).willReturn(Optional.of(state));
            given(compareStateRepo.find(ROLE_ID)).willReturn(Optional.of(buildCompareState(100, 1, true)));
            given(equipFeign.decompose(any())).willReturn(Map.of("itemId", 50, "num", 2, "exp", 100));
            ArgumentCaptor<BoxState> captor = ArgumentCaptor.forClass(BoxState.class);
            given(boxRepo.save(captor.capture())).willAnswer(inv -> inv.getArgument(0));

            boxService.decompose(ROLE_ID);

            BoxState saved = captor.getValue();
            assertThat(saved.isLastOpenIsFive()).isFalse();
            then(compareStateRepo).should().delete(ROLE_ID);
        }
    }
}
