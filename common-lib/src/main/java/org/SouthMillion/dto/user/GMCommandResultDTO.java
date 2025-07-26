package org.SouthMillion.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class GMCommandResultDTO implements Serializable {
    private byte[] type;
    private byte[] result;
    // getter & setter
}