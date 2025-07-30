package org.SouthMillion.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.SouthMillion.proto.Msgrole.Msgrole;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSettingDTO {
    private Integer roleId;
    private List<Msgrole.PB_system_set> systemSetList;
}