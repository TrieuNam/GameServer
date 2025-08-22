package org.SouthMillion.dto.shop;

public record ResultDTO<T>(boolean ok, String error, T data) {
    public static <T> ResultDTO<T> ok(T data) { return new ResultDTO<>(true, null, data); }
    public static <T> ResultDTO<T> fail(String msg) { return new ResultDTO<>(false, msg, null); }
}