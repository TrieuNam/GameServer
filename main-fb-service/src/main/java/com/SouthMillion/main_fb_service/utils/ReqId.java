package com.SouthMillion.main_fb_service.utils;

import java.util.UUID;

public final class ReqId {
    private ReqId() {}
    public static String gen() { return UUID.randomUUID().toString(); }
}