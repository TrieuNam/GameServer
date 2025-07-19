package org.SouthMillion.dto.pet;

import lombok.Data;

import java.util.List;

@Data
public class KaigeziRateDTO {
    private int seq;
    private int rate;
    private List<Object> open; // hoặc định nghĩa DTO nếu open có cấu trúc cụ thể
}
