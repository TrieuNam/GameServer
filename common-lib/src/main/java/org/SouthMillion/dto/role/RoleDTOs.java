package org.SouthMillion.dto.role;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class RoleDTOs {

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RoleResp {
        // ===== Core id / user binding
        private String  roleId;          // lưu dưới dạng String, server sẽ parse int32 khi emit
        private String  userId;

        // ===== Tên (Emitters ưu tiên name, sau đó nickname/roleName)
        private String  name;
        @JsonAlias({"nick","nickname"})
        private String  nickname;
        @JsonAlias({"role_name","rolename"})
        private String  roleName;

        // ===== Level / Exp (Emitters đọc curExp; alias từ exp nếu service cũ trả 'exp')
        private Integer level;
        @JsonAlias({"exp"})              // cho phép service cũ trả "exp"
        private Long    curExp;          // Emitters dùng field này để set PB_SCRoleInfoAck.cur_exp

        // ===== RoleInfo fields (khớp PB_RoleInfo/PB_SCRoleInfoAck)
        private Long    cap;             // battle power
        private Integer headPicId;       // avatar id
        private Integer titleId;         // danh hiệu

        @JsonAlias({"createTime","create_time","createTimeSec"})
        private Long    createTimeEpochSec; // epoch seconds

        private Integer knightLevel;     // cấp kỵ sĩ
        private String  headChar;        // avatar url/bytes (client để bytes, ở đây để String nguồn)
        private String  guildName;       // tên guild

        // ===== (Optional) stat cơ bản — không dùng trong Emitters.sendRoleInfoAck, giữ để mở rộng
        private Long    hp;
        private Long    attack;
        private Long    defense;
        private Integer speed;
    }

    // ====== Request dùng khi tạo mới role (handler có trySet nhiều field — thêm sẵn cho “khớp”)
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CreateRoleReq {
        @NotBlank
        private String userId;

        // tên hiển thị — handler sẽ ưu tiên set nickname/roleName/name
        @NotBlank
        private String name;

        // Các field handler có thể set nếu tồn tại
        private String uid;
        private String accountId;
        private String nickname;
        private String roleName;

        private Integer job;
        private Integer classId;
        private Integer gender;
        private String  serverId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class ListResp {
        private List<RoleResp> items;
    }

    // ===== Mutate APIs =====

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AddExpReq {
        @NotBlank private String roleId;
        private long exp;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RenameReq {
        @NotBlank private String name;
    }
}