package org.SouthMillion.mapper.user;

import com.google.protobuf.ByteString;
import org.SouthMillion.dto.user.RoleInfoDTO;
import org.SouthMillion.proto.Msgrole.Msgrole;

public class UserProtoMapper {
    public static Msgrole.PB_RoleInfo toProto(RoleInfoDTO dto) {
        Msgrole.PB_RoleInfo.Builder builder = Msgrole.PB_RoleInfo.newBuilder();
        if (dto.getRoleId() != null) builder.setRoleId(dto.getRoleId());
        if (dto.getRoleName() != null) builder.setName(ByteString.copyFromUtf8(dto.getRoleName()));
        if (dto.getLevel() != null) builder.setLevel(dto.getLevel());
        if (dto.getCap() != null) builder.setCap(dto.getCap());
        if (dto.getHeadPicId() != null) builder.setHeadPicId(dto.getHeadPicId());
        if (dto.getTitleId() != null) builder.setTitleId(dto.getTitleId());
        if (dto.getGuildName() != null) builder.setGuildName(ByteString.copyFromUtf8(dto.getGuildName()));
        if (dto.getKnightLevel() != null) builder.setKnightLevel(dto.getKnightLevel());
        if (dto.getHeadChar() != null) builder.setHeadChar(ByteString.copyFromUtf8(dto.getHeadChar()));
        return builder.build();
    }
}