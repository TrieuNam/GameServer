package org.SouthMillion.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GMCommandDTO {
    private Integer roleId;
    private String commandType;
    private String command;
}