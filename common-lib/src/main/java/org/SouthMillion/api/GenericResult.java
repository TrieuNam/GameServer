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
        return GenericResult.<T>builder().code(0).message("OK").data(data).build();
    }

    public static <T> GenericResult<T> fail(int code, String message) {
        return GenericResult.<T>builder().code(code).message(message).build();
    }

    public static <T> GenericResult<T> error(String msg) {
        return new GenericResult<>(-1, msg, null);
    }

    public static <T> GenericResult<T> fail(String msg) {
        return new GenericResult<>(-1, msg, null);
    }

    public boolean isOk() {
        return code == null || code == 0;
    }

}