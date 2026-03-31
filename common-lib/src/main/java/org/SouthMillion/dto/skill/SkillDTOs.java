package org.SouthMillion.dto.skill;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

/**
 * DTOs cho hệ thống kỹ năng (skill) của nhân vật.
 *
 * <p>Proto tương ứng: msgskill.proto
 * <ul>
 *   <li>CS 1470 - PB_CSRoleSkillOperaReq  (client yêu cầu thao tác skill)</li>
 *   <li>SC 1471 - PB_SCRoleSkillAllInfo   (server trả toàn bộ skill info)</li>
 *   <li>CS 1480 - PB_CSRoleTalentOperaReq (client yêu cầu thao tác thiên phú)</li>
 *   <li>SC 1481 - PB_SCRoleTalentAllInfo  (server trả toàn bộ thiên phú info)</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SkillDTOs {

    // ─────────────────────────── Skill (active) ───────────────────────────

    /** Một entry kỹ năng của nhân vật (khớp PB_RoleSkillInfoPro). */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RoleSkillInfo {
        private Integer skillId;
        private Integer skillLevel;
        private Integer skillIndex;   // vị trí slot kỹ năng
    }

    /** Response: toàn bộ danh sách kỹ năng của nhân vật (khớp PB_SCRoleSkillAllInfo). */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SkillAllInfoResp {
        private Integer skillCount;
        private List<RoleSkillInfo> skillList;
        /** 0 = OK; xem SkillErrorCodes cho các giá trị khác. */
        private Integer errorCode;
        /** Mô tả lỗi (debug/log), không hiển thị cho client. */
        private String  errorMsg;
    }

    /** Request: học kỹ năng / lấy thông tin. */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LearnSkillReq {
        private Long   roleId;
        private Integer skillId;   // skill muốn học (hoặc 0 nếu là ONE_KEY)
        private Integer reqType;   // 0=INFO, 1=LEARN_SKILL, 2=ONE_KEY_LEVEL_UP
    }

    // ─────────────────────────── Talent (passive) ─────────────────────────

    /** Một entry thiên phú kỹ năng (khớp PB_RoleTalentInfoPro). */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RoleTalentInfo {
        private Integer skillId;
        private Integer skillLevel;
    }

    /** Response: toàn bộ danh sách thiên phú (khớp PB_SCRoleTalentAllInfo). */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TalentAllInfoResp {
        private Integer talentSkillCount;
        private List<RoleTalentInfo> talentSkillList;
        /** 0 = OK; xem SkillErrorCodes cho các giá trị khác. */
        private Integer errorCode;
        /** Mô tả lỗi (debug/log), không hiển thị cho client. */
        private String  errorMsg;
    }

    /** Request: học thiên phú / lấy thông tin. */
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LearnTalentReq {
        private Long   roleId;
        private Integer skillId;   // thiên phú muốn học (hoặc 0 nếu là INFO)
        private Integer reqType;   // 0=INFO, 1=LEARN_TALENT_SKILL
    }

    // ─────────────────────────── Error Codes ──────────────────────────────

    /**
     * Mã lỗi cho các operation skill / talent.
     *
     * <p>Quy ước: 0 = thành công. Client dùng mã này để hiển thị popup thông báo.
     */
    public static final class SkillErrorCodes {
        private SkillErrorCodes() {}

        /** Thao tác thành công. */
        public static final int OK = 0;

        /** Nhân vật không tồn tại. */
        public static final int ROLE_NOT_FOUND = 1;

        /** skillId không hợp lệ (≤ 0 hoặc null). */
        public static final int INVALID_SKILL_ID = 2;

        /** Kỹ năng / thiên phú đã đạt cấp tối đa. */
        public static final int ALREADY_MAX_LEVEL = 3;

        /** Nhân vật chưa đủ cấp để học kỹ năng này. */
        public static final int ROLE_LEVEL_TOO_LOW = 4;

        /** Không đủ tài nguyên (kim tiền / item) để học / nâng cấp. */
        public static final int INSUFFICIENT_RESOURCE = 5;

        /** Kỹ năng chưa được mở khóa (chưa đủ điều kiện tiên quyết). */
        public static final int SKILL_NOT_UNLOCKED = 6;
    }
}

