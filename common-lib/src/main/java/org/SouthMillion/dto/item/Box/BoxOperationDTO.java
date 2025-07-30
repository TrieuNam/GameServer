package org.SouthMillion.dto.item.Box;

import lombok.Data;

@Data
public class BoxOperationDTO {
    private Integer reqType; // 1: mở hộp, 2: trang bị, 3: bán
    private Integer param;   // tham số (id box hoặc id item)
}