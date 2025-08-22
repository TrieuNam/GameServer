package org.SouthMillion.dto.role;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

public final class RoleDTOs {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleResp {
        private String roleId;
        private String userId;
        private String name;
        private int level;
        private long exp;
        private long hp;
        private long attack;
        private long defense;
        private int speed;
    }

    @Getter
    @Setter
    public static class CreateRoleReq {
        @NotBlank
        private String userId;
        @NotBlank
        private String name;
        // optional: job/gender/serverId nếu sau này bạn cần
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResp {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<RoleResp> items;
    }

    @Getter
    @Setter
    public static class AddExpReq {
        @NotBlank
        private String roleId;
        @Min(1)
        private long addExp;
    }

    @Getter
    @Setter
    public static class RenameReq {
        @NotBlank
        private String roleId;
        @NotBlank
        private String name;
    }
}