package org.SouthMillion.dto.pet;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;

public class PetDTOs {
    // ===== View =====
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AllInfoResp {
        private List<Integer> fightPetIndex;
        private List<PetData> petList;
        private List<TsGemData> tsGemList;   // optional: tổng hợp theo petIndex
        private List<ClothData> clothList;   // optional
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PetData {
        private int petIndex;
        private int petId;
        private int petLevel;
        private long petExp;
        private int petOrder;
        private List<Integer> skillIds;
        private List<Integer> skillLevels;
        private List<Integer> gemItemId;   // theo slot
        private List<Integer> tsGemIndex;  // theo slot
        private List<Integer> attrList;    // [hp, atk, def, spd,...] theo client
        private long capability;
        private int skillLockFlag;
    }

    // ===== Commands =====
    public record SetFightReq(@NotBlank String roleId, @NotNull List<@Min(0) Integer> fightPetIndex) {
    }

    public record LevelUpReq(@NotBlank String roleId, @Min(0) int petIndex, @Min(1) int times,
                             Map<Long, Long> costItems) {
    }

    public record GradeUpReq(@NotBlank String roleId, @Min(0) int petIndex,
                             @NotNull List<@Min(1) Long> costItemIds) {
    }

    public record GemInlayReq(@NotBlank String roleId, int petIndex, int slot, long gemItemId) {
    }

    public record GemLevelUpReq(@NotBlank String roleId, int petIndex, int slot,
                                Map<Long, Long> materials) {
    }

    public record TsGemUpReq(@NotBlank String roleId, int petIndex, int slot, int targetLevel) {
    }

    public record SkillLearnReq(@NotBlank String roleId, int petIndex, int slot, long skillBookItemId,
                                @Min(0) int lockFlag) {
    }

    public record OkResp(boolean ok, String error) {
        public static OkResp OK() {
            return new OkResp(true, null);
        }

        public static OkResp NG(String e) {
            return new OkResp(false, e);
        }
    }

    public record TsGemData(int petIndex, List<Integer> tsGemIndex) {
    }

    public record ClothData(long clothId, boolean activated) {
    }
}