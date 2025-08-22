package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.entity.PlayerHarnessEntity;
import com.SouthMillion.pet_service.entity.PlayerMountEntity;
import com.SouthMillion.pet_service.repository.PlayerHarnessRepository;
import com.SouthMillion.pet_service.repository.PlayerMountRepository;
import com.SouthMillion.pet_service.service.client.BagFeignClient;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.proto.Msgmount.Msgmount;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MountService {
    private final PlayerMountRepository playerMountRepository;
    private final PlayerHarnessRepository playerHarnessRepository;
    private final MountConfigService mountConfigService;
    private final BagFeignClient bagService;

    /**
     * Build & trả về thông tin tất cả mount player có (PB_SCMountInfo, MsgId 2141)
     */
    public byte[] getMountInfo(String playerId) {
        List<PlayerMountEntity> mountList = playerMountRepository.findByPlayerId(playerId);

        Msgmount.PB_SCMountInfo.Builder builder = Msgmount.PB_SCMountInfo.newBuilder();

        // mapping từng mount sang PB_MountData
        for (PlayerMountEntity mount : mountList) {
            Msgmount.PB_MountData.Builder mountBuilder = Msgmount.PB_MountData.newBuilder()
                    .setLevel(mount.getLevel() != null ? mount.getLevel() : 0)
                    .setGrade(mount.getGrade() != null ? mount.getGrade() : 0)
                    .setLastExploreTime(mount.getCreatedTime() != null ? mount.getCreatedTime() : 0);
            // Nếu có các trường khác trong PB_MountData thì mapping bổ sung ở đây
            builder.addMountList(mountBuilder);
        }
        // Bạn có thể thêm các trường như appearanceId, pifuList, freeTime, refreshNum,... nếu có logic tương ứng
        return builder.build().toByteArray();
    }

    /**
     * Build & trả về danh sách harness (马具) player có (PB_SCMountHarnessListInfo, MsgId 2143)
     */
    public byte[] getHarnessListInfo(String playerId) {
        List<PlayerHarnessEntity> harnessList = playerHarnessRepository.findByPlayerId(playerId);

        Msgmount.PB_SCMountHarnessListInfo.Builder builder = Msgmount.PB_SCMountHarnessListInfo.newBuilder();

        for (PlayerHarnessEntity harness : harnessList) {
            Msgmount.PB_HarnessData.Builder harnessBuilder = Msgmount.PB_HarnessData.newBuilder()
                    .setIndex(harness.getId().intValue())
                    .setItemId(harness.getHarnessId() != null ? harness.getHarnessId() : 0)
                    .setWearingMark(harness.getWearingMark() != null ? harness.getWearingMark() : 0)
                    .setLockFlag(harness.getLockFlag() != null ? harness.getLockFlag() : 0);
            // Thuộc tính bổ sung (nếu có)
            // Nếu attrType/attrValue dạng List<Integer> thì parse từ JSON/string thành list:
            if (harness.getAttrType() != null) {
                harnessBuilder.addAllAttrType(parseIntList(harness.getAttrType()));
            }
            if (harness.getAttrValue() != null) {
                harnessBuilder.addAllAttrVaule(parseIntList(harness.getAttrValue()));
            }
            builder.addHarnessList(harnessBuilder);
        }
        return builder.build().toByteArray();
    }

    // --- Helper: parse attrType, attrValue từ JSON/chuỗi thành List<Integer> ---
    private List<Integer> parseIntList(String src) {
        if (src == null || src.isEmpty()) return List.of();
        // Nếu lưu dạng CSV: "10,11,12,13"...
        return List.of(src.split(",")).stream().map(Integer::parseInt).collect(Collectors.toList());
        // Nếu bạn lưu JSON, thì parse bằng ObjectMapper
    }
}
