package com.SouthMillion.box_service.service;

import com.SouthMillion.box_service.config.LuckUnpackConfigCache;
import com.SouthMillion.box_service.config.UnpackConfigCache;
import com.SouthMillion.box_service.enity.BoxState;
import com.SouthMillion.box_service.enity.LuckState;
import com.SouthMillion.box_service.repository.BoxStateRepository;
import com.SouthMillion.box_service.repository.LuckStateRepository;
import com.SouthMillion.box_service.service.client.BagFeign;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.bag.BagDTOs.ItemDelta;
import org.SouthMillion.dto.bag.BagDTOs.AddItemReq;
import org.SouthMillion.dto.bag.BagDTOs.ConsumeReq;
import org.SouthMillion.dto.box.BoxDTOs;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class BoxService {

    private final BoxStateRepository boxRepo;
    private final LuckStateRepository luckRepo;
    private final UnpackConfigCache unpackCfg;
    private final LuckUnpackConfigCache luckCfg;
    private final BagFeign bag;

    private BoxState getOrCreate(String roleId) {
        return boxRepo.findById(roleId).orElseGet(() -> {
            BoxState s = new BoxState();
            s.setRoleId(roleId);
            s.setBoxLevel(1);
            return boxRepo.save(s);
        });
    }

    public BoxDTOs.InfoResp info(String roleId) {
        BoxState s = getOrCreate(roleId);
        Map<String, Object> pending = null;
        String x = s.getPendingJson();
        if (x != null && !x.isBlank()) {
            Map<String, Object> json = Map.of("json", x);
            pending = json;
        }
        return BoxDTOs.InfoResp.builder()
                .boxLevel(s.getBoxLevel())
                .boxBuyTimes(s.getBoxBuyTimes())
                .levelUpEndEpoch(s.getLevelUpEndEpoch())
                .levelFetchFlag(s.getLevelFetchFlag())
                .openBoxTotal(s.getOpenBoxTotal())
                .lastOpenIsFive(s.isLastOpenIsFive())
                .pending(pending)
                .build();
    }

    @Transactional
    public BoxDTOs.OpenResp open(BoxDTOs.OpenReq req) {
        String roleId = req.getRoleId();
        int count = req.getCount();
        int roleLevel = req.getRoleLevel();

        BoxState s = getOrCreate(roleId);
        if (s.getPendingJson()!=null && !s.getPendingJson().isBlank()) {
            throw new IllegalStateException("There is a pending opened equip.");
        }

        // chọn quality theo box_level & count
        Map<String,Object> colorRow = unpackCfg.randomColor().stream()
                .filter(m -> Integer.parseInt(String.valueOf(m.get("box_level"))) == s.getBoxLevel())
                .findFirst().orElseThrow(() -> new IllegalStateException("box_level config not found"));

        String ratioStr = String.valueOf(colorRow.get(count==5 ? "equipment_color_2" : "equipment_color_1"));
        int[] weights = Arrays.stream(ratioStr.split("\\|")).mapToInt(Integer::parseInt).toArray();
        int quality = weightedPick(weights); // 1..8

        // roll equip_level theo roleLevel
        int equipLevel = rollEquipLevel(roleLevel);

        // TODO build attr (color_att), fashion/get_challenge/fixed reward… tối giản
        Map<String,Object> pending = new LinkedHashMap<>();
        pending.put("quality", quality);
        pending.put("equipLevel", equipLevel);
        pending.put("rolledAt", Instant.now().getEpochSecond());
        pending.put("count", count);

        s.setPendingJson(pending.toString()); // ở v1 lưu toString, có thể dùng Jackson.
        s.setOpenBoxTotal(s.getOpenBoxTotal() + count);
        s.setLastOpenIsFive(count==5);
        boxRepo.save(s);

        // Luck snapshot: không cần cập nhật start/snapshot tại đây

        return BoxDTOs.OpenResp.builder()
                .pending(pending)
                .openBoxTotal(s.getOpenBoxTotal())
                .lastOpenIsFive(s.isLastOpenIsFive())
                .bonusItems(List.of())
                .build();
    }

    // Weighted random index (1-based)
    private int weightedPick(int[] w) {
        int sum = 0; for (int x : w) sum += x;
        int r = ThreadLocalRandom.current().nextInt(sum);
        int acc=0; for (int i=0;i<w.length;i++){ acc+=w[i]; if (r<acc) return i+1; }
        return 1;
    }

    private int rollEquipLevel(int roleLevel) {
        var rows = unpackCfg.randomLevel();
        int sum=0;
        List<int[]> candidates = new ArrayList<>();
        for (var r : rows) {
            int lv = Integer.parseInt(r.get("level"));
            if (lv!=roleLevel) continue;
            int randLv = Integer.parseInt(r.get("random_level"));
            int rate = Integer.parseInt(r.get("rate"));
            sum += rate;
            candidates.add(new int[]{randLv, rate});
        }
        if (sum==0) return roleLevel;
        int rnd = ThreadLocalRandom.current().nextInt(sum);
        int acc=0;
        for (var c : candidates) { acc += c[1]; if (rnd < acc) return c[0]; }
        return roleLevel;
    }

    @Transactional
    public BoxDTOs.OkResp wear(String roleId) {
        BoxState s = getOrCreate(roleId);
        if (s.getPendingJson()==null || s.getPendingJson().isBlank())
            return BoxDTOs.OkResp.builder().ok(false).message("No pending equip").build();

        // TODO: nối sang equip-service để tạo item instance và mặc vào slot.
        s.setPendingJson(null);
        boxRepo.save(s);
        return BoxDTOs.OkResp.builder().ok(true).message("Worn (pending cleared)").build();
    }

    @Transactional
    public BoxDTOs.OkResp sell(String roleId) {
        BoxState s = getOrCreate(roleId);
        if (s.getPendingJson()==null || s.getPendingJson().isBlank())
            return BoxDTOs.OkResp.builder().ok(false).message("No pending equip").build();

        // TODO: tính coin/exp bán, cộng vào bag. v1: bỏ qua, chỉ clear pending
        s.setPendingJson(null);
        boxRepo.save(s);
        return BoxDTOs.OkResp.builder().ok(true).message("Sold (pending cleared)").build();
    }

    @Transactional
    public BoxDTOs.OkResp buy(String roleId) {
        BoxState s = getOrCreate(roleId);
        var row = unpackCfg.randomColor().stream()
                .filter(m -> Integer.parseInt(String.valueOf(m.get("box_level"))) == s.getBoxLevel())
                .findFirst().orElseThrow();

        int price = Integer.parseInt(String.valueOf(row.get("price")));
        int currency = Integer.parseInt(unpackCfg.other().get(0).get("currency_type"));

        bag.consume(new ConsumeReq(roleId, (byte)0, List.of(new ItemDelta(currency, price, false, null)), 3001, 0));
        s.setBoxBuyTimes(s.getBoxBuyTimes()+1);
        boxRepo.save(s);

        // gửi quà theo row.reward[] (nếu có)
        @SuppressWarnings("unchecked")
        List<Map<String,String>> reward = (List<Map<String,String>>) row.getOrDefault("reward", List.of());
        if (!reward.isEmpty()) {
            List<ItemDelta> items = new ArrayList<>();
            for (var r: reward) {
                int itemId = Integer.parseInt(r.get("item_id"));
                long num = Long.parseLong(r.get("num"));
                items.add(new ItemDelta(itemId, num, false, null));
            }
            bag.add(new AddItemReq(roleId, (byte)0, items, 3101, 0));
        }
        return BoxDTOs.OkResp.builder().ok(true).message("Bought 1 time").build();
    }

    @Transactional
    public BoxDTOs.OkResp levelUp(String roleId) {
        BoxState s = getOrCreate(roleId);
        if (s.getLevelUpEndEpoch() > Instant.now().getEpochSecond())
            return BoxDTOs.OkResp.builder().ok(false).message("Level up in progress").build();

        var row = unpackCfg.randomColor().stream()
                .filter(m -> Integer.parseInt(String.valueOf(m.get("box_level"))) == s.getBoxLevel())
                .findFirst().orElseThrow();

        int need = Integer.parseInt(String.valueOf(row.get("up_buy_num")));
        int minutes = Integer.parseInt(String.valueOf(row.get("up_time_minute")));
        if (s.getBoxBuyTimes() < need)
            return BoxDTOs.OkResp.builder().ok(false).message("Not enough buy_times").build();

        long end = Instant.now().plusSeconds(minutes*60L).getEpochSecond();
        s.setLevelUpEndEpoch(end);
        boxRepo.save(s);
        return BoxDTOs.OkResp.builder().ok(true).message("Level up started").build();
    }

    @Transactional
    public BoxDTOs.OkResp quicken(String roleId, int num) {
        BoxState s = getOrCreate(roleId);
        long left = s.getLevelUpEndEpoch() - Instant.now().getEpochSecond();
        if (left <= 0) return BoxDTOs.OkResp.builder().ok(false).message("No level up").build();

        int quickId = Integer.parseInt(unpackCfg.other().get(0).get("accelerate_id"));
        bag.consume(new ConsumeReq(roleId, (byte)0, List.of(new ItemDelta(quickId, num, false, null)), 3201, 0));

        long reduce = num * 60L; // 1 item = 60s? (Bạn chỉnh theo rule của game)
        long newEnd = Math.max(0, s.getLevelUpEndEpoch() - reduce);
        s.setLevelUpEndEpoch(newEnd);
        boxRepo.save(s);
        return BoxDTOs.OkResp.builder().ok(true).message("Quickened").build();
    }

    @Transactional
    public BoxDTOs.OkResp levelReward(String roleId, int idx) {
        BoxState s = getOrCreate(roleId);
        var row = unpackCfg.randomColor().stream()
                .filter(m -> Integer.parseInt(String.valueOf(m.get("box_level"))) == s.getBoxLevel())
                .findFirst().orElseThrow();

        int need = Integer.parseInt(String.valueOf(row.get("up_buy_num")));
        if (idx < 1 || idx > need) return BoxDTOs.OkResp.builder().ok(false).message("idx out of range").build();
        if (((s.getLevelFetchFlag() >> (idx-1)) & 1) == 1) return BoxDTOs.OkResp.builder().ok(false).message("Already fetched").build();

        @SuppressWarnings("unchecked")
        List<Map<String,String>> reward = (List<Map<String,String>>) row.getOrDefault("reward", List.of());
        if (reward.size() < idx) return BoxDTOs.OkResp.builder().ok(false).message("No reward at idx").build();

        var r = reward.get(idx-1);
        int itemId = Integer.parseInt(r.get("item_id"));
        long num   = Long.parseLong(r.get("num"));
        bag.add(new AddItemReq(roleId, (byte)0, List.of(new ItemDelta(itemId, num, false, null)), 3301, 0));

        s.setLevelFetchFlag(s.getLevelFetchFlag() | (1<<(idx-1)));
        boxRepo.save(s);
        return BoxDTOs.OkResp.builder().ok(true).message("Reward fetched").build();
    }

    // ===== Luck Unpacking =====

    public BoxDTOs.LuckInfoResp luckInfo(String roleId) {
        LuckState ls = luckRepo.findById(roleId).orElseGet(() -> initLuck(roleId));
        BoxState bs = getOrCreate(roleId);
        int delta = Math.max(0, bs.getOpenBoxTotal() - ls.getSnapshotOpenCnt());
        return BoxDTOs.LuckInfoResp.builder()
                .endTimestamp(ls.getEndEpoch())
                .receiveFlag(ls.getReceiveBitmap())
                .openBoxNumDelta(delta)
                .boxLevel(bs.getBoxLevel())
                .build();
    }

    @Transactional
    public BoxDTOs.OkResp luckReceive(String roleId, int seq) {
        LuckState ls = luckRepo.findById(roleId).orElseGet(() -> initLuck(roleId));
        long now = Instant.now().getEpochSecond();
        if (now > ls.getEndEpoch()) return BoxDTOs.OkResp.builder().ok(false).message("Event ended").build();
        if (((ls.getReceiveBitmap() >> seq) & 1L) == 1L) return BoxDTOs.OkResp.builder().ok(false).message("Already received").build();

        var opt = luckCfg.reward().stream().filter(m -> Integer.parseInt(String.valueOf(m.get("type"))) == seq).findFirst();
        if (opt.isEmpty()) return BoxDTOs.OkResp.builder().ok(false).message("Seq not found").build();

        var r = opt.get();
        int typeBoxNum = Integer.parseInt(String.valueOf(r.get("type_box_num")));
        int typeNum    = Integer.parseInt(String.valueOf(r.get("type_num")));
        Map<String,String> rewardItem = (Map<String,String>) r.get("reward_item");
        int itemId = Integer.parseInt(rewardItem.get("item_id"));
        long num   = Long.parseLong(rewardItem.get("num"));

        BoxState bs = getOrCreate(roleId);
        boolean ok;
        if (typeBoxNum==1) {
            int delta = Math.max(0, bs.getOpenBoxTotal() - ls.getSnapshotOpenCnt());
            ok = delta >= typeNum;
        } else if (typeBoxNum==2) {
            ok = bs.getBoxLevel() >= typeNum;
        } else ok = false;

        if (!ok) return BoxDTOs.OkResp.builder().ok(false).message("Condition not met").build();

        bag.add(new AddItemReq(roleId, (byte)0, List.of(new ItemDelta(itemId, num, false, null)), 3401, 0));
        ls.setReceiveBitmap(ls.getReceiveBitmap() | (1L<<seq));
        luckRepo.save(ls);
        return BoxDTOs.OkResp.builder().ok(true).message("Received").build();
    }

    private LuckState initLuck(String roleId) {
        var other = luckCfg.other().isEmpty()? Map.<String,String>of() : luckCfg.other().get(0);
        boolean isOpen = "1".equals(other.getOrDefault("is_open","0"));
        int days = Integer.parseInt(other.getOrDefault("time","0"));
        long now = Instant.now().getEpochSecond();

        LuckState ls = new LuckState();
        ls.setRoleId(roleId);
        if (isOpen && days>0) {
            ls.setStartEpoch(now);
            ls.setEndEpoch(now + days*86400L);
        } else {
            ls.setStartEpoch(0L); ls.setEndEpoch(0L);
        }
        BoxState bs = getOrCreate(roleId);
        ls.setSnapshotOpenCnt(bs.getOpenBoxTotal());
        return luckRepo.save(ls);
    }
}