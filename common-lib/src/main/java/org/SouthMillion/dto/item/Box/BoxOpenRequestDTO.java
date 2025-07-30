package org.SouthMillion.dto.item.Box;

import lombok.Data;

import java.util.Map;

@Data
public class BoxOpenRequestDTO {
    private Long userId;
    private String boxType;       // "unpack", "gift", "baoxiangjijin", ...
    private Integer boxId;        // id box hoặc level
    private Map<String, Object> extraParams; // Tham số mở rộng nếu cần
    // Getter/Setter
}
