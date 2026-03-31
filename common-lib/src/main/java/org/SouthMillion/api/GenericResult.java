package org.SouthMillion.api;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenericResult<T> {
    private Integer  code;     // 0 = ok
    private String message;
    private T data;

    public static <T> GenericResult<T> ok(T data) {
        GenericResult<T> result = new GenericResult<>();
        result.code = 0;
        result.message = "OK";
        result.data = data;
        return result;
    }

    public static <T> GenericResult<T> fail(int code, String message) {
        GenericResult<T> result = new GenericResult<>();
        result.code = code;
        result.message = message;
        return result;
    }

    public static <T> GenericResult<T> error(String msg) {
        GenericResult<T> result = new GenericResult<>();
        result.code = -1;
        result.message = msg;
        return result;
    }

    public static <T> GenericResult<T> fail(String msg) {
        GenericResult<T> result = new GenericResult<>();
        result.code = -1;
        result.message = msg;
        return result;
    }

    public boolean isOk() {
        return code == null || code == 0;
    }

}