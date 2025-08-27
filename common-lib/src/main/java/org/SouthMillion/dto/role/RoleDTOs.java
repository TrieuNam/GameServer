package org.SouthMillion.dto.role;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

public final class RoleDTOs {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RoleResp {
        // ===== core id/name/level/exp
        private String  roleId;
        private String  userId;
        private String  name;

        private Integer level;               // Integer để cho phép null
        private Long    exp;                 // Long để cho phép null

        // ===== các field khớp với proto (RoleInfoAck)
        private Long    cap;                 // battle power
        private Integer headPicId;           // avatar id
        private Integer titleId;             // danh hiệu
        private Long    createTimeEpochSec;  // epoch seconds

        private Integer knightLevel;         // cấp kỵ sĩ
        private String  headChar;            // ký tự đầu
        private String  guildName;           // tên bang

        // ===== stat cơ bản
        private Long    hp;
        private Long    attack;
        private Long    defense;
        private Integer speed;
    }

    @Getter @Setter
    public static class CreateRoleReq {
        @NotBlank
        private String userId;
        @NotBlank
        private String name;
        // nếu sau này cần job/gender/serverId thì thêm
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ListResp {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<RoleResp> items;
    }

    // ===== BỔ SUNG: DTO cho mutate APIs =====

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AddExpReq {
        private String roleId;
        private long exp;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RenameReq {
        @NotBlank
        private String name;
    }
}