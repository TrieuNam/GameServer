package org.SouthMillion.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultDTO<T> {
    private int code;
    private String message;
    private T data;
    public static <T> ResultDTO<T> ok(T data){ return new ResultDTO<>(0,"OK",data); }
    public static <T> ResultDTO<T> err(int code, String msg){ return new ResultDTO<>(code,msg,null); }
}
