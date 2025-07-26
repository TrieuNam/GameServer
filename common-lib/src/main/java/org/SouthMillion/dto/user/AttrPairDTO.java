package org.SouthMillion.dto.user;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Data
@Getter
@Setter
public class AttrPairDTO implements Serializable {
    private Integer attrType;    // 属性类型号
    private Long attrValue;      // 属性的值
}