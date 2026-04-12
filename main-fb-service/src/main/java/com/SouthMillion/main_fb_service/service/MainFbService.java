package com.SouthMillion.main_fb_service.service;

import com.SouthMillion.main_fb_service.entity.MainFbChapterRewardEntity;
import com.SouthMillion.main_fb_service.entity.MainFbStageProgressEntity;
import com.SouthMillion.main_fb_service.entity.MainFbTaskProgressEntity;
import com.SouthMillion.main_fb_service.repository.MainFbChapterRewardRepository;
import com.SouthMillion.main_fb_service.repository.MainFbStageProgressRepository;
import com.SouthMillion.main_fb_service.repository.MainFbTaskProgressRepository;
import com.SouthMillion.main_fb_service.service.client.GameworldFeignClient;
import com.SouthMillion.main_fb_service.service.client.ItemFeignClient;
import com.SouthMillion.main_fb_service.service.remote.MainFbTaskConfigService;
import com.SouthMillion.main_fb_service.service.remote.MaoxianConfig;
import com.SouthMillion.main_fb_service.service.remote.RemoteMaoxianConfigService;
import com.SouthMillion.main_fb_service.utils.ReqId;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.main_fb.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainFbService {
    private final RemoteMaoxianConfigService cfg;
    private final MainFbTaskConfigService taskCfg;                 // NEW
    private final ItemFeignClient itemFeign;
    private final GameworldFeignClient gameworldFeign;
    private final MainFbStageProgressRepository progressRepo;
    private final MainFbChapterRewardRepository chapterRepo;
    private final MainFbTaskProgressRepository taskRepo;           // NEW
    private final StringRedisTemplate redis;
    private final TaskProgressPublisher taskProgressPublisher;

    @Value("${mainfb.stamina-enabled:false}")
    private boolean STAMINA_ENABLED;

    @Value("${mainfb.stamina-item-id:0}")
    private Long STAMINA_ITEM_ID;

    private static final long LEGACY_INVALID_STAMINA_ITEM_ID = 50001L;

    private static String settleKey(String battleId) { return "mainfb:settle:" + battleId; }

    // ====== NHIỆM VỤ: public API ======
    @Transactional()
    public TaskResp getCurrentTask(String playerId) {
        var state = taskRepo.findByPlayerId(playerId).orElse(null);
        int nextIndex = (state == null || state.getDoneIndex() == null)
                ? taskCfg.firstIndex()
                : state.getDoneIndex() + 1;

        var opt = taskCfg.byIndex(nextIndex);
        if (opt.isEmpty()) {
            return TaskResp.builder()
                    .allDone(true)
                    .title("Đã hoàn thành toàn bộ ải")
                    .desc("Không còn nhiệm vụ trong danh sách.")
                    .build();
        }
        var t = opt.get();
        return TaskResp.builder()
                .index(t.getIndex())
                .title("Vượt ải %d-%d".formatted(t.getStage(), t.getLevel()))
                .desc("Hoàn thành ải %d-%d (chỉ cần clear một lần).".formatted(t.getStage(), t.getLevel()))
                .stage(t.getStage())
                .level(t.getLevel())
                .requireScore(t.getThreeStarScore()) // để hiển thị tham khảo
                .rewards(mapRewards(t.getRewardPreview())) // chỉ preview
                .allDone(false)
                .build();
    }

    // ====== GIỮ NGUYÊN (progress hiện tại) ======
    public MainFbDTOs.ProgressResp getProgress(String playerId) {
        var list = progressRepo.findByPlayerId(playerId);
        MainFbDTOs.ProgressResp resp = new MainFbDTOs.ProgressResp();
        resp.setProgresses(list.stream().map(e -> {
            MainFbDTOs.StageProgress sp = new MainFbDTOs.StageProgress();
            sp.setStage(e.getStage()); sp.setLevel(e.getLevel());
            sp.setBestScore(Optional.ofNullable(e.getBestScore()).orElse(0));
            sp.setClearCount(Optional.ofNullable(e.getClearCount()).orElse(0));
            return sp; }).collect(Collectors.toList()));
        return resp;
    }

    @Transactional
    public MainFbDTOs.EnterStageResp enter(MainFbDTOs.EnterStageReq req) {
        var cOpt = cfg.findStage(req.getStage(), req.getLevel());
        if (cOpt.isEmpty()) throw new IllegalArgumentException("Stage/Level không tồn tại trong maoxian.json");
        MaoxianConfig.Clearance c = cOpt.get();

        // Validate unlock
        validateUnlock(req.getPlayerId(), req.getStage(), req.getLevel());

        int cost = safeInt(c.getNow_box());
        consumeConfiguredStamina(req.getPlayerId(), cost, "mainfb.enterStage");

        var inst = gameworldFeign.createInstance(
                new GameworldFeignClient.CreateInstanceReq(req.getPlayerId(), req.getStage(), req.getLevel()));

        MainFbDTOs.EnterStageResp resp = new MainFbDTOs.EnterStageResp();
        resp.setBattleId(inst.battleId());
        resp.setSeed(inst.seed());
        resp.setExpireAt(inst.expireAt());
        resp.setCost(cost);
        return resp;
    }

    @Transactional
    public List<ItemStackDTO> finish(MainFbDTOs.FinishStageReq req) {
        // idempotency
        String key = settleKey(req.getBattleId());
        Boolean existed = redis.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(30));
        if (Boolean.FALSE.equals(existed)) {
            return Collections.emptyList();
        }

        var c = cfg.findStage(req.getStage(), req.getLevel())
                .orElseThrow(() -> new IllegalArgumentException("Stage/Level không tồn tại"));

        // cập nhật progress
        var progress = progressRepo.findByPlayerIdAndStageAndLevel(req.getPlayerId(), req.getStage(), req.getLevel())
                .orElse(MainFbStageProgressEntity.builder()
                        .playerId(req.getPlayerId()).stage(req.getStage()).level(req.getLevel()).bestScore(0).clearCount(0).build());
        progress.onClear(req.getScore());
        progressRepo.save(progress);

        int clearedDungeonProgress = (int) progressRepo.findByPlayerId(req.getPlayerId()).stream()
                .filter(p -> Optional.ofNullable(p.getClearCount()).orElse(0) > 0)
                .count();

        // build phần thưởng (win)
        List<ItemStackDTO> rewards = mapDrops(c.getWin());
        if (progress.getClearCount() == 1) {
            rewards = aggregate(addSame(rewards, rewards));
        }

        // ====== TIẾN NHIỆM VỤ NỘI BỘ + TASK condition_4 (vượt ải) ======
        maybeAdvanceMission(req.getPlayerId(), req.getStage(), req.getLevel());
        publishDungeonProgressSnapshot(req.getPlayerId(), clearedDungeonProgress);

        // phát thưởng ải
        grantRewards(req.getPlayerId(), rewards, "mainfb.finishStage");
        return rewards;
    }

    @Transactional
    public MainFbDTOs.SweepResp sweep(MainFbDTOs.SweepReq req) {
        var c = cfg.findStage(req.getStage(), req.getLevel())
                .orElseThrow(() -> new IllegalArgumentException("Stage/Level không tồn tại"));

        var progress = progressRepo.findByPlayerIdAndStageAndLevel(req.getPlayerId(), req.getStage(), req.getLevel())
                .orElseThrow(() -> new IllegalArgumentException("Chưa từng vượt ải này"));
        int requireScore = safeInt(c.getScore());
        if (progress.getBestScore() == null || progress.getBestScore() < requireScore) {
            throw new IllegalStateException("Chưa đạt điều kiện quét (3 sao)");
        }

        int times = req.getTimes();
        int maxQuick = safeInt(c.getQuick_num());
        if (maxQuick > 0 && times > maxQuick) times = maxQuick;
        int quickCost = calcQuickCost(c.getQuick_expend(), times);
        consumeConfiguredStamina(req.getPlayerId(), quickCost, "mainfb.sweep");

        List<ItemStackDTO> base = mapDrops(c.getWin());
        List<ItemStackDTO> total = new ArrayList<>();
        for (int i = 0; i < times; i++) total.addAll(base);
        total = aggregate(total);

        grantRewards(req.getPlayerId(), total, "mainfb.sweep");

        MainFbDTOs.SweepResp resp = new MainFbDTOs.SweepResp();
        resp.setRewards(total.stream().map(s -> {
            MainFbDTOs.RewardItem r = new MainFbDTOs.RewardItem(); r.setItemId(s.itemId()); r.setCount(s.count()); return r;}).toList());
        return resp;
    }

    @Transactional
    public List<ItemStackDTO> claimChapterReward(String playerId, int stage, String chapterLabel) {
        var cfgObj = cfg.getConfig();
        var jdr = cfgObj.getJieduan_reward().stream()
                .filter(j -> Integer.parseInt(j.getStage()) == stage && j.getChapter().equals(chapterLabel))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Không có mốc chương này"));

        var rec = chapterRepo.findByPlayerIdAndStageAndChapterLabel(playerId, stage, chapterLabel)
                .orElse(MainFbChapterRewardEntity.builder().playerId(playerId).stage(stage).chapterLabel(chapterLabel).claimed(false).build());
        if (Boolean.TRUE.equals(rec.getClaimed())) throw new IllegalStateException("Đã nhận trước đó");

        int need = safeInt(jdr.getClearance_condition());
        long cleared = progressRepo.findByPlayerId(playerId).stream()
                .filter(p -> p.getStage() == stage && Optional.ofNullable(p.getClearCount()).orElse(0) > 0).count();
        if (cleared < need) throw new IllegalStateException("Chưa đủ số ải vượt để nhận thưởng chương");

        List<ItemStackDTO> rewards = mapDrops(jdr.getWin());
        grantRewards(playerId, rewards, "mainfb.chapterReward");

        rec.markClaimed();
        chapterRepo.save(rec);
        return rewards;
    }

    // ====== Helpers ======

    /** Nếu lần finish này đúng ải đang chờ trong tuyến nhiệm vụ -> tăng doneIndex. */
    private void maybeAdvanceMission(String playerId, int stage, int level) {
        int idxOf = taskCfg.indexOf(stage, level);
        if (idxOf < 0) return;

        var state = taskRepo.findByPlayerId(playerId).orElse(null);
        int nextIndex = (state == null || state.getDoneIndex() == null)
                ? taskCfg.firstIndex()
                : state.getDoneIndex() + 1;

        if (idxOf != nextIndex) return; // chỉ cho đi theo thứ tự

        if (state == null) {
            state = MainFbTaskProgressEntity.builder()
                    .playerId(playerId)
                    .doneIndex(idxOf)
                    .updatedAt(Instant.now().getEpochSecond())
                    .build();
        } else {
            state.markDone(idxOf);
        }
        taskRepo.save(state);
    }

    private void publishDungeonProgressSnapshot(String playerId, int clearedDungeonProgress) {
        if (clearedDungeonProgress <= 0) {
            return;
        }
        // task-service condition 4 uses SNAPSHOT_MAX, so send the player's current cleared-dungeon count
        // instead of delta=1 and instead of tying the publish to the internal main-fb mission order.
        if (!taskProgressPublisher.publish(playerId, "complete_dungeon", clearedDungeonProgress, "main-fb-service")) {
            log.warn("[MainFb] Failed to publish dungeon progress snapshot for player {} progress={}",
                    playerId, clearedDungeonProgress);
        }
    }

    private void consumeConfiguredStamina(String playerId, int cost, String source) {
        if (cost <= 0) {
            return;
        }
        if (!STAMINA_ENABLED) {
            log.debug("[MainFb] Stamina consume skipped for playerId={} source={} because mainfb.stamina-enabled=false",
                    playerId, source);
            return;
        }
        if (Objects.equals(STAMINA_ITEM_ID, LEGACY_INVALID_STAMINA_ITEM_ID)) {
            log.warn("[MainFb] Stamina consume skipped for playerId={} source={} because itemId={} is a legacy invalid config value",
                    playerId, source, STAMINA_ITEM_ID);
            return;
        }
        if (STAMINA_ITEM_ID == null || STAMINA_ITEM_ID <= 0) {
            log.debug("[MainFb] Stamina consume skipped for playerId={} source={} because mainfb.stamina-item-id is disabled ({})",
                    playerId, source, STAMINA_ITEM_ID);
            return;
        }

        long roleId = resolveInternalRoleId(playerId);
        var consumeReq = BagConsumeReq.builder()
                .userId(roleId)
                .roleId(roleId)
                .itemId(STAMINA_ITEM_ID.intValue())
                .amount(cost)
                .source(source)
                .idemKey(ReqId.gen())
                .build();
        itemFeign.consumeItem(consumeReq);
    }

    private void grantRewards(String playerId, List<ItemStackDTO> rewards, String source) {
        if (rewards == null || rewards.isEmpty()) {
            return;
        }

        long roleId = resolveInternalRoleId(playerId);
        var bagItems = rewards.stream()
                .map(s -> BagAddItemReq.Item.builder()
                        .itemId(s.itemId().intValue())
                        .amount(s.count())
                        .build())
                .toList();
        var addReq = BagAddItemReq.builder()
                .userId(roleId)
                .roleId(roleId)
                .items(bagItems)
                .source(source)
                .idemKey(ReqId.gen())
                .build();
        itemFeign.addItemsBulk(addReq);
    }

    private long resolveInternalRoleId(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("playerId is required");
        }
        try {
            return Long.parseLong(playerId);
        } catch (NumberFormatException ignored) {
            long fallbackRoleId = Integer.toUnsignedLong(playerId.hashCode());
            log.debug("[MainFb] Non-numeric playerId='{}'; using synthetic roleId={} for internal bag calls",
                    playerId, fallbackRoleId);
            return fallbackRoleId;
        }
    }

    private void validateUnlock(String playerId, int stage, int level) {
        if (level == 1 && stage == 1) return;
        if (level > 1) {
            var prev = progressRepo.findByPlayerIdAndStageAndLevel(playerId, stage, level - 1);
            if (prev.isEmpty() || Optional.ofNullable(prev.get().getClearCount()).orElse(0) <= 0)
                throw new IllegalStateException("Chưa mở khoá (cần clear level trước)");
        } else {
            int maxPrev = cfg.maxLevelInStage(stage - 1);
            var prev = progressRepo.findByPlayerIdAndStageAndLevel(playerId, stage - 1, maxPrev);
            if (prev.isEmpty() || Optional.ofNullable(prev.get().getClearCount()).orElse(0) <= 0)
                throw new IllegalStateException("Chưa mở khoá (cần clear chương trước)");
        }
    }

    private static int safeInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }

    private static List<ItemStackDTO> mapDrops(List<MaoxianConfig.Drop> drops) {
        if (drops == null) return Collections.emptyList();
        return drops.stream().map(d -> new ItemStackDTO(Long.parseLong(d.getItem_id()), Integer.parseInt(d.getNum()))).toList();
    }

    private static List<RewardItem> mapRewards(List<MaoxianConfig.Drop> drops) {
        if (drops == null) return Collections.emptyList();
        return drops.stream().map(d -> new RewardItem(Long.parseLong(d.getItem_id()), Integer.parseInt(d.getNum()))).toList();
    }

    private static List<ItemStackDTO> addSame(List<ItemStackDTO> a, List<ItemStackDTO> b) {
        List<ItemStackDTO> out = new ArrayList<>();
        out.addAll(a); out.addAll(b); return out; }

    private static List<ItemStackDTO> aggregate(List<ItemStackDTO> in) {
        Map<Long, Integer> map = new HashMap<>();
        for (var it : in) map.merge(it.itemId(), it.count(), Integer::sum);
        return map.entrySet().stream().map(e -> new ItemStackDTO(e.getKey(), e.getValue())).toList();
    }

    private static int calcQuickCost(String quickExpend, int times) {
        if (quickExpend == null || quickExpend.isEmpty() || times <= 0) return 0;
        String[] parts = quickExpend.split("\\|");
        int cost = 0;
        for (int i = 0; i < times && i < parts.length; i++) {
            try { cost += Integer.parseInt(parts[i]); } catch (Exception ignored) {}
        }
        return cost;
    }
}