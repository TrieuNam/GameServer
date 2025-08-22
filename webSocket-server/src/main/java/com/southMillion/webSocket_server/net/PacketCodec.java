package com.southMillion.webSocket_server.net;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class PacketCodec {

    private PacketCodec() {}

    /** Client gửi: [4 bytes length = 4 + payload][4 bytes msgId][payload] Big-Endian */
    public static Decoded decode(byte[] frame) {
        if (frame.length < 8) throw new IllegalArgumentException("frame too short");
        ByteBuffer buf = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN);
        int bodyLen = buf.getInt();              // = 4 + payload
        if (bodyLen != frame.length - 4) {
            // có thể chấp nhận len lệch một chút tùy client, nhưng ở đây strict
        }
        int msgId = buf.getInt();
        byte[] payload = new byte[frame.length - 8];
        buf.get(payload);
        return new Decoded(msgId, payload);
    }

    /** Server trả: [4 bytes length = 4 + payload][4 bytes msgId][payload] Big-Endian */
    public static byte[] encode(int msgId, byte[] payload) {
        int bodyLen = 4 + (payload == null ? 0 : payload.length);
        ByteBuffer out = ByteBuffer.allocate(4 + bodyLen).order(ByteOrder.BIG_ENDIAN);
        out.putInt(bodyLen);
        out.putInt(msgId);
        if (payload != null && payload.length > 0) out.put(payload);
        return out.array();
    }

    public record Decoded(int msgId, byte[] payload) { }
}