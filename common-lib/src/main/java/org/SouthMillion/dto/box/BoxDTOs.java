package org.SouthMillion.dto.box;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/** DTOs cho BoxService (không dùng record). */
public class BoxDTOs {

    // ===== /info
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InfoResp {
        private int boxLevel;
        private int boxBuyTimes;
        private long levelUpEndEpoch;
        private int levelFetchFlag;
        private int openBoxTotal;
        private boolean lastOpenIsFive;
        private int shiZhuangNum;
        private int arenaItemNum;
        /** Open-time snapshot (equip vừa roll, bonus, v.v.) */
        private Map<String, Object> pending; // null nếu không có
        /** Typed compare session for compare UI during migration away from pendingJson. */
        private BoxCompareStateResp compareState;
    }

    // ===== /open (request)
    @Getter @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenReq {
        @NotBlank private String roleId;
        @Min(1) @Max(5) private int count;
        @Min(1) private int roleLevel;
    }

    // ===== /open (response)
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenResp {
        /** equip vừa roll (raw map), có thể chứa stats hoặc các flag khác */
        private Map<String,Object> pending;
        private int openBoxTotal;
        private boolean lastOpenIsFive;
        private List<Map<String,Object>> bonusItems;  // challenge/fashion/... nếu có

        /** Typed equip mới vừa roll — dùng cho SC_BOX_EQUIP_INFO popup. */
        private EquipRolled openEquip;   // optional
        private Integer isNew;           // 0/1 optional (thường = 1 sau open)
        /** Equip hiện tại của player ở cùng slot — để client so sánh cũ vs mới. */
        private EquipRolled currentEquip; // null nếu chưa có equip ở slot này
        /** Typed compare session stored separately from pendingJson. */
        private BoxCompareStateResp compareState;
    }

    // ===== các req/resp khác
    @Getter @Setter @NoArgsConstructor
    public static class WearReq { @NotBlank private String roleId; }

    @Getter @Setter @NoArgsConstructor
    public static class SellReq { @NotBlank private String roleId; }

    @Getter @Setter @NoArgsConstructor
    public static class SimpleReq { @NotBlank private String roleId; }

    @Getter @Setter @NoArgsConstructor
    public static class QuickenReq extends SimpleReq { @Min(1) private int num; }

    @Getter @Setter @NoArgsConstructor
    public static class LevelRewardReq extends SimpleReq { @Min(1) private int idx; }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OkResp {
        private boolean ok;
        private String message;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SellResp {
        private boolean ok;
        private String message;
        private long sellCoin;
        private long sellExp;
    }

    // ===== Luck Unpacking
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LuckInfoResp {
        private long endTimestamp;
        private long receiveFlag;
        private int  openBoxNumDelta;
        private int  boxLevel;
    }

    @Getter @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LuckReceiveReq {
        @NotBlank private String roleId;
        @Min(0) private int seq;
    }

    // ===== Setting
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BoxSettingResp {
        private int equipEqality, openFiveMark, equipCapMark, equipSellMark;
        private int conditionFirst1, conditionFirst2, conditionSecond1, conditionSecond2;
        private int conditionFirstMark, conditionSecondMark, retainMark, challengeMark;
    }

    @Getter @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BoxSettingReq {
        private String roleId;
        private BoxSettingResp boxSet;
    }

    /** Alias of BoxSettingReq — used by POST /setting to persist auto-sweep stop conditions */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SaveSettingReq {
        private String roleId;
        /** eqality: min quality to keep (PB_WaBaoSet.eqality) */
        private int equipEqality;
        /** eqalityMark: 1=stop when got ≥ eqality, 0=ignore (PB_WaBaoSet.eqality_mark) */
        private int openFiveMark;
        /** newRecord: 1=stop on new collection record (PB_WaBaoSet.new_record) */
        private int retainMark;
        /** newBook: 1=stop on new collection book (PB_WaBaoSet.new_book) */
        private int newBook;
    }

    // ===== Decompose
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DecomposeResp {
        private boolean ok;
        private long gotItemId;
        private long gotNum;
        private long gotExp;
        private String message;
    }

    /** Dòng parse kiểu hoá theo file thực tế. */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EquipRow {
        private int id, part, level, quality;
        private long hpMin, hpMax;
        private long attMin, attMax;
        private long defMin, defMax;
        private long speedMin, speedMax;
        private long exp, fristAtt, secondAtt;
        private int itemType;
        private long sellPrice;
        private int pileLimit;
        private int isDropRecord, invalidTime, isSpecial;
    }

    /** Stats RANGE (min/max) để nhét vào pending + kèm attr type/value. */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EquipStats {

        @Getter @Setter
        @NoArgsConstructor @AllArgsConstructor @Builder
        public static class Range {
            private long min;
            private long max;
        }

        private Range hp;
        private Range attack;
        private Range defense;
        private Range speed;
        private Integer attrType1;
        private Integer attrValue1;
        private Integer attrType2;
        private Integer attrValue2;

        /** “Setter” bất biến — trả về bản sao mới với giá trị đã cập nhật. */
        public EquipStats setAttrType1Copy(Integer v)  { return copyWith(v, this.attrValue1, this.attrType2, this.attrValue2); }
        public EquipStats setAttrValue1Copy(Integer v) { return copyWith(this.attrType1, v, this.attrType2, this.attrValue2); }
        public EquipStats setAttrType2Copy(Integer v)  { return copyWith(this.attrType1, this.attrValue1, v, this.attrValue2); }
        public EquipStats setAttrValue2Copy(Integer v) { return copyWith(this.attrType1, this.attrValue1, this.attrType2, v); }

        private EquipStats copyWith(Integer at1, Integer av1, Integer at2, Integer av2) {
            return EquipStats.builder()
                    .hp(hp).attack(attack).defense(defense).speed(speed)
                    .attrType1(at1).attrValue1(av1).attrType2(at2).attrValue2(av2)
                    .build();
        }

        /** Điền mặc định attr type từ EquipRow nếu hiện đang null. */
        public EquipStats withDefaultsFrom(EquipRow r) {
            Integer t1 = (attrType1 != null) ? attrType1 : (int) r.getFristAtt();
            Integer t2 = (attrType2 != null) ? attrType2 : (int) r.getSecondAtt();
            return EquipStats.builder()
                    .hp(hp).attack(attack).defense(defense).speed(speed)
                    .attrType1(t1).attrValue1(attrValue1).attrType2(t2).attrValue2(attrValue2)
                    .build();
        }

        /** Lấy EquipStats trực tiếp từ EquipRow (attrType1/2 = fristAtt/secondAtt, value mặc định = null). */
        public static EquipStats fromRow(EquipRow r) {
            return EquipStats.builder()
                    .hp(new Range(r.getHpMin(),    r.getHpMax()))
                    .attack(new Range(r.getAttMin(),   r.getAttMax()))
                    .defense(new Range(r.getDefMin(),  r.getDefMax()))
                    .speed(new Range(r.getSpeedMin(),  r.getSpeedMax()))
                    .attrType1((int) r.getFristAtt()).attrValue1(null)
                    .attrType2((int) r.getSecondAtt()).attrValue2(null)
                    .build();
        }

        /** Xuất JSON (map) để nhét vào pending.stats (range). */
        public Map<String, Object> toJson() {
            Map<String, Object> m = new LinkedHashMap<>();
            if (hp     != null) m.put("hp",     Map.of("min", hp.getMin(),     "max", hp.getMax()));
            if (attack != null) m.put("attack", Map.of("min", attack.getMin(), "max", attack.getMax()));
            if (defense!= null) m.put("defense",Map.of("min", defense.getMin(),"max", defense.getMax()));
            if (speed  != null) m.put("speed",  Map.of("min", speed.getMin(),  "max", speed.getMax()));
            if (attrType1  != null) m.put("attr_type1",  attrType1);
            if (attrValue1 != null) m.put("attr_value1", attrValue1);
            if (attrType2  != null) m.put("attr_type2",  attrType2);
            if (attrValue2 != null) m.put("attr_value2", attrValue2);
            return m;
        }
    }

    /** Dữ liệu ROLLED (đơn trị) để map đúng PB_EquipData trong SC_BOX_EQUIP_INFO. */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EquipRolled {
        private Integer equipType;
        private Integer itemId;
        private Integer hp;
        private Integer attack;
        private Integer defend;     // lưu ý: PB_EquipData dùng "defend"
        private Integer speed;
        private Integer attrType1;
        private Integer attrValue1;
        private Integer attrType2;
        private Integer attrValue2;
    }

    /** Payload của SC_BOX_EQUIP_INFO. */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EquipInfo {
        private Integer isNew;       // 0/1
        private EquipRolled equipInfo;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BoxCompareSnapshotDTO {
        private Integer itemId;
        private Integer equipType;
        private Integer quality;
        private Integer equipLevel;
        private Integer hp;
        private Integer attack;
        private Integer defend;
        private Integer speed;
        private Integer attrType1;
        private Integer attrValue1;
        private Integer attrType2;
        private Integer attrValue2;

        public EquipRolled toEquipRolled() {
            return EquipRolled.builder()
                    .equipType(equipType)
                    .itemId(itemId)
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

        public static BoxCompareSnapshotDTO fromEquipRolled(EquipRolled rolled) {
            if (rolled == null) return null;
            return BoxCompareSnapshotDTO.builder()
                    .equipType(rolled.getEquipType())
                    .itemId(rolled.getItemId())
                    .hp(rolled.getHp())
                    .attack(rolled.getAttack())
                    .defend(rolled.getDefend())
                    .speed(rolled.getSpeed())
                    .attrType1(rolled.getAttrType1())
                    .attrValue1(rolled.getAttrValue1())
                    .attrType2(rolled.getAttrType2())
                    .attrValue2(rolled.getAttrValue2())
                    .build();
        }
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BoxCompareStateResp {
        private Long roleId;
        private String stateVersion;
        private String source;
        private String status;
        private long openedAt;
        private Integer isNew;
        private BoxCompareSnapshotDTO candidateEquip;
        private BoxCompareSnapshotDTO equippedBefore;
    }

    public static EquipInfo equipInfoFromCompareState(BoxCompareStateResp compareState, Integer isNewDefault) {
        if (compareState == null || compareState.getCandidateEquip() == null) return null;
        Integer finalIsNew = compareState.getIsNew() != null ? compareState.getIsNew() : isNewDefault;
        return EquipInfo.builder()
                .isNew(finalIsNew)
                .equipInfo(compareState.getCandidateEquip().toEquipRolled())
                .build();
    }

    // ---------- Helpers để parse pending => EquipInfo (nếu pending đã chứa trị số đơn) ----------
    @SuppressWarnings("unchecked")
    public static EquipInfo equipInfoFromPending(Map<String, Object> pending, Integer isNewDefault) {
        if (pending == null || pending.isEmpty()) return null;

        Map<String, Object> src = pending;
        Object nested = pending.get("equip");
        if (nested instanceof Map<?, ?> m) src = (Map<String, Object>) m;

        Integer pendingIsNew = pickInt(src, "isNew", "is_new");
        if (pendingIsNew == null) {
            pendingIsNew = pickInt(pending, "isNew", "is_new");
        }
        Integer finalIsNew = pendingIsNew != null ? pendingIsNew : isNewDefault;

        Integer itemId   = pickInt(src, "itemId", "item_id");
        Integer equipTyp = pickInt(src, "equipType", "equip_type", "type", "pos", "part");

        // stats có thể nằm trong src hoặc src.stats
        Map<String, Object> stats = asMap(src.get("stats"));
        Integer hp     = pickInt(stats, src, "hp");
        Integer attack = pickInt(stats, src, "attack");
        // có thể là "defend" hoặc "defense" tuỳ nguồn
        Integer defend = orElse(pickInt(stats, src, "defend"), pickInt(stats, src, "defense"));
        Integer speed  = pickInt(stats, src, "speed");

        Integer at1 = pickInt(stats, src, "attr_type1");
        Integer av1 = pickInt(stats, src, "attr_value1");
        Integer at2 = pickInt(stats, src, "attr_type2");
        Integer av2 = pickInt(stats, src, "attr_value2");

        EquipRolled rolled = EquipRolled.builder()
                .equipType(equipTyp)
                .itemId(itemId)
                .hp(hp).attack(attack).defend(defend).speed(speed)
                .attrType1(at1).attrValue1(av1).attrType2(at2).attrValue2(av2)
                .build();

        return EquipInfo.builder()
            .isNew(finalIsNew)
                .equipInfo(rolled)
                .build();
    }

    // ------------ small utils ------------
    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return (o instanceof Map<?,?> m) ? (Map<String, Object>) m : null;
    }

    private static Integer orElse(Integer a, Integer b) { return (a != null) ? a : b; }

    private static Integer pickInt(Map<String, Object> m, String... keys) {
        if (m == null) return null;
        for (String k : keys) {
            Object v = m.get(k);
            Integer got = toIntIfNumber(v);
            if (got != null) return got;
        }
        return null;
    }

    /** ưu tiên lấy từ stats nếu có, fallback sang src phẳng */
    private static Integer pickInt(Map<String, Object> stats, Map<String, Object> src, String key) {
        Integer fromStats = pickInt(stats, key);
        if (fromStats != null) return fromStats;

        Object raw = (stats != null) ? stats.get(key) : null;
        if (raw instanceof Map<?,?>) {
            // range {min,max} -> bỏ qua (chưa có rolled)
        }
        return pickInt(src, key);
    }

    private static Integer toIntIfNumber(Object v) {
        if (v instanceof Number n) return n.intValue();
        try {
            if (v instanceof String s && !s.isBlank()) return Integer.parseInt(s.trim());
        } catch (Exception ignore) {}
        return null;
    }

    // ===== WaBao SC DTOs — SC 1643/1645/1646/1647/1648/1650/1651 ==========

    /** SC 1643 PB_SCWaBaoMapInfo */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WaBaoMapInfo {
        private int curMap;        // PB_SCWaBaoMapInfo.cur_map
        private int unlockedMap;   // PB_SCWaBaoMapInfo.unlocked_map
        private List<Integer> mapConditionNum; // PB_SCWaBaoMapInfo.map_condition_num
    }

    /** One node for SC 1645 PB_WaBaoIntegrityNode */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WaBaoIntegrityNode {
        private int itemId;
        private int maxIntegrity;
    }

    /** SC 1645 PB_SCWaBaoIntegrityInfo */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WaBaoIntegrityInfo {
        private int isLogin;                    // 1=login push list
        private List<WaBaoIntegrityNode> dataList;
    }

    /** One node for SC 1646 PB_WaBaoCollectionNode */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WaBaoCollectionNode {
        private int itemType;   // 宝物种类 (4 types)
        private int index;      // bag slot (6 per type)
        private int itemId;
        private int integrity;
        private int attrType1;  private int attrValue1;
        private int attrType2;  private int attrValue2;
    }

    /** SC 1646 PB_SCWaBaoCollectionListInfo */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WaBaoCollectionInfo {
        private int isLogin;
        private List<WaBaoCollectionNode> dataList;
    }

    /** One tool item for SC 1647 */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WaBaoToolNode {
        private int itemId;
        private int count;
    }

    /** SC 1647 PB_SCWaBaoToolInfo */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WaBaoToolInfo {
        private List<WaBaoToolNode> toolList;
    }

    /** One task for SC 1648 */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WaBaoTaskNode {
        private int taskId;
        private int progress;
        private int target;
        private int status;   // 0=ongoing 1=claimable 2=claimed
    }

    /** SC 1648 PB_SCWaBaoTaskInfo — task_flag + repeated int32 task_list + task_type_num */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WaBaoTaskInfo {
        private Integer taskFlag;
        private List<Integer> taskList;
        private List<Integer> taskTypeNumList;
    }

    /** SC 1651 PB_SCWaBaoBookListInfo — repeated int32 activate_flag */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WaBaoBookListInfo {
        private List<Integer> activateFlagList;
    }
}

