package com.SouthMillion.box_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


// BoxProperties.java
@ConfigurationProperties(prefix = "box")
@Data
public class BoxProperties {
    /** Nếu >0 sẽ dùng trực tiếp, bỏ qua lookup JSON. */
    private long initItemId = 0;

    /** Tên box muốn cấp (khớp theo equals/contains). Ví dụ: "暗夜骑士礼盒". */
    private String starterGiftName;

    /** Bag chứa box (0=túi thường...). */
    private byte starterBagType = 0;

    /** Số lượng box khởi đầu. */
    private int initCount = 3;
}