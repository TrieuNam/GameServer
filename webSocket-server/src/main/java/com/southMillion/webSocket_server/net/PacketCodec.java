package com.southMillion.webSocket_server.net;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class PacketCodec {
    private PacketCodec(){}

    public static Decoded decode(byte[] frame) {
        if (frame == null || frame.length < 8) return null;
        var buf = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN);
        int bodyLen = buf.getInt();
        if (bodyLen != frame.length - 4) return null;
        int msgId = buf.getInt();
        byte[] payload = new byte[bodyLen - 4];
        buf.get(payload);
        return new Decoded(msgId, payload);
    }

    public static byte[] encode(int msgId, byte[] payload) {
        int bodyLen = 4 + (payload == null ? 0 : payload.length);
        ByteBuffer buf = ByteBuffer.allocate(4 + bodyLen).order(ByteOrder.BIG_ENDIAN);
        buf.putInt(bodyLen);
        buf.putInt(msgId);
        if (payload != null) buf.put(payload);
        return buf.array();
    }

    public record Decoded(int msgId, byte[] payload) {}
}