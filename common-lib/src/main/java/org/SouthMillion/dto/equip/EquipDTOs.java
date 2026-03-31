package org.SouthMillion.dto.equip;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EquipDTOs {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquipItem {
        private int equipType;
        private int itemId;
        private int hp;
        private int attack;
        private int defend;
        private int speed;
        private int attrType1;
        private int attrValue1;
        private int attrType2;
        private int attrValue2;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResp {
        private List<EquipItem> items;
    }

    /** Yêu cầu mặc trang bị */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquipReq {
        @NotBlank
        private String roleId;

        @Min(1)
        private int itemId;

        /** 0=bag thường; 1=túi trang bị; null=theo cấu hình equip.equip-bag-type */
        private Byte bagType;

        /** optional — ép mặc vào slot cụ thể nếu meta không chỉ rõ (null = auto) */
        private Byte forceEquipType;
    }

    /** Yêu cầu tháo trang bị */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnequipReq {
        @NotBlank
        private String roleId;

        @Min(0)
        private int equipType;

        private Byte bagType;
    }

    /** Phản hồi OK/NG đơn giản */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OkResp {
        private boolean ok;
        private String message;

        public static OkResp OK() { return new OkResp(true, null); }
        public static OkResp NG(String msg) { return new OkResp(false, msg); }

        // Giữ tương thích với chỗ gọi resp.ok() / resp.message()
        public boolean ok() { return ok; }
        public String message() { return message; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WearFromBoxItem {
        private String kind;
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
        private Boolean isNew;

        public static WearFromBoxItem fromPending(Map<String, Object> pending) {
            if (pending == null || pending.isEmpty()) return null;
            return WearFromBoxItem.builder()
                    .kind(asString(pending.get("kind")))
                    .itemId(asInt(pending.get("itemId"), pending.get("item_id")))
                    .equipType(asInt(pending.get("equipType"), pending.get("equip_type"), pending.get("position"), pending.get("pos")))
                    .quality(asInt(pending.get("quality"), pending.get("color"), pending.get("q")))
                    .equipLevel(asInt(pending.get("equipLevel"), pending.get("level"), pending.get("lv")))
                    .hp(asInt(pending.get("hp")))
                    .attack(asInt(pending.get("attack"), pending.get("att")))
                    .defend(asInt(pending.get("defend"), pending.get("defense"), pending.get("def")))
                    .speed(asInt(pending.get("speed"), pending.get("spd")))
                    .attrType1(asInt(pending.get("attrType1"), pending.get("attr_type1"), pending.get("fristAtt")))
                    .attrValue1(asInt(pending.get("attrValue1"), pending.get("attr_value1"), pending.get("fristAttValue")))
                    .attrType2(asInt(pending.get("attrType2"), pending.get("attr_type2"), pending.get("secondAtt")))
                    .attrValue2(asInt(pending.get("attrValue2"), pending.get("attr_value2"), pending.get("secondAttValue")))
                    .isNew(asBoolean(pending.get("isNew"), pending.get("is_new")))
                    .build();
        }

        public Map<String, Object> toPendingMap() {
            Map<String, Object> pending = new LinkedHashMap<>();
            pending.put("kind", kind != null ? kind : "equip");
            if (itemId != null) pending.put("itemId", itemId);
            if (equipType != null) pending.put("equipType", equipType);
            if (quality != null) pending.put("quality", quality);
            if (equipLevel != null) pending.put("equipLevel", equipLevel);
            if (hp != null) pending.put("hp", hp);
            if (attack != null) pending.put("attack", attack);
            if (defend != null) pending.put("defend", defend);
            if (speed != null) pending.put("speed", speed);
            if (attrType1 != null) pending.put("attr_type1", attrType1);
            if (attrValue1 != null) pending.put("attr_value1", attrValue1);
            if (attrType2 != null) pending.put("attr_type2", attrType2);
            if (attrValue2 != null) pending.put("attr_value2", attrValue2);
            if (isNew != null) pending.put("isNew", isNew);
            return pending;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WearFromBoxReq {
        @NotBlank
        private String roleId;

        @NotNull
        private WearFromBoxItem item;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReplacedEquip {
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
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WearFromBoxResp {
        private ReplacedEquip replaced;
    }

    private static Integer asInt(Object... values) {
        if (values == null) return null;
        for (Object value : values) {
            if (value instanceof Number n) return n.intValue();
            if (value instanceof String s) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        return Integer.parseInt(trimmed);
                    } catch (Exception ignore) {
                        // ignore non-numeric string
                    }
                }
            }
        }
        return null;
    }

    private static String asString(Object value) {
        if (value == null) return null;
        String str = String.valueOf(value).trim();
        return str.isEmpty() ? null : str;
    }

    private static Boolean asBoolean(Object... values) {
        if (values == null) return null;
        for (Object value : values) {
            if (value instanceof Boolean b) return b;
            if (value instanceof Number n) return n.intValue() != 0;
            if (value instanceof String s) {
                String trimmed = s.trim();
                if (trimmed.isEmpty()) continue;
                if ("true".equalsIgnoreCase(trimmed) || "1".equals(trimmed)) return true;
                if ("false".equalsIgnoreCase(trimmed) || "0".equals(trimmed)) return false;
            }
        }
        return null;
    }
}