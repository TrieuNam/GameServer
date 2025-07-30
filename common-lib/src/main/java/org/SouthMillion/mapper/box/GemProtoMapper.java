package org.SouthMillion.mapper.box;

import org.SouthMillion.dto.item.gem.GemDrawingDTO;
import org.SouthMillion.dto.item.gem.GemInlayDTO;
import org.SouthMillion.dto.item.gem.GemstoneDTO;
import org.SouthMillion.dto.item.gem.GemstoneDrawingDTO;
import org.SouthMillion.proto.Msgother.Msgother;

import java.util.List;

public class GemProtoMapper {

    public static Msgother.PB_GemInlayData toProto(GemInlayDTO inlay) {
        Msgother.PB_GemInlayData.Builder builder = Msgother.PB_GemInlayData.newBuilder();
        if (inlay.getItemId() != null) builder.setItemId(inlay.getItemId());
        if (inlay.getPos() != null) builder.setPos(inlay.getPos());
        return builder.build();
    }

    public static Msgother.PB_GemDrawingData toProto(GemDrawingDTO dto) {
        Msgother.PB_GemDrawingData.Builder builder = Msgother.PB_GemDrawingData.newBuilder();
        if (dto.getLevel() != null) builder.setLevel(dto.getLevel());
        if (dto.getGemList() != null) {
            for (GemInlayDTO inlay : dto.getGemList()) {
                builder.addGemList(toProto(inlay));
            }
        }
        return builder.build();
    }

    public static Msgother.PB_SCGemInfo toProtoGemInfo(List<GemDrawingDTO> drawingList, int drawingId) {
        Msgother.PB_SCGemInfo.Builder builder = Msgother.PB_SCGemInfo.newBuilder();
        builder.setDrawingId(drawingId);
        if (drawingList != null) {
            for (GemDrawingDTO dto : drawingList) {
                builder.addDrawingList(toProto(dto));
            }
        }
        return builder.build();
    }


    public static Msgother.PB_GemDrawingData toProto(GemstoneDrawingDTO dto) {
        // Không có dữ liệu meaningful để mapping sang proto, chỉ trả bản vẽ rỗng
        Msgother.PB_GemDrawingData.Builder builder = Msgother.PB_GemDrawingData.newBuilder();
        builder.setLevel(0); // Hoặc không set gì cả
        return builder.build();
    }

    public static Msgother.PB_GemDrawingData toProto(GemstoneDTO dto) {
        Msgother.PB_GemDrawingData.Builder builder = Msgother.PB_GemDrawingData.newBuilder();
        // Gán gem_level làm level, hoặc mặc định 1 nếu null
        builder.setLevel(dto.getGemLevel() == null ? 1 : dto.getGemLevel());

        // Giả lập gắn một viên đá duy nhất với itemId là id của đá, vị trí 0
        Msgother.PB_GemInlayData inlay = Msgother.PB_GemInlayData.newBuilder()
                .setItemId(dto.getId())
                .setPos(0)
                .build();

        builder.addGemList(inlay);
        return builder.build();
    }
}