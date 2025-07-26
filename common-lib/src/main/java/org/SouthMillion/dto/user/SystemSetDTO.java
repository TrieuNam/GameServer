package org.SouthMillion.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@Setter
@Getter
public class SystemSetDTO implements Serializable {
    private Integer systemSetType;   // 设置类型
    private Integer systemSetParam;  // 设置值
}