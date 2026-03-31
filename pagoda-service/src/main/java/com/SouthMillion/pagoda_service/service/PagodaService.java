package com.SouthMillion.pagoda_service.service;

import com.SouthMillion.pagoda_service.entity.*;
import com.SouthMillion.pagoda_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagodaService {

    private final ShiLianProgressRepository shiLianRepo;
    private final GuMoLayerRepository guMoLayerRepo;
    private final GuMoMetaRepository guMoMetaRepo;

    // === ShiLian (Trial Tower) ===

    public ShiLianProgress getShiLian(Long roleId) {
        return shiLianRepo.findByRoleId(roleId)
                .orElseGet(() -> shiLianRepo.save(
                        ShiLianProgress.builder()
                                .roleId(roleId).passLevel(0).bestLevel(0)
                                .useItem(0).randomId(0).seasonEndTime(0L).build()));
    }

    @Transactional
    public ShiLianProgress challengeShiLian(Long roleId, int p1) {
        ShiLianProgress progress = getShiLian(roleId);
        // p1 = the level being challenged
        if (p1 > progress.getPassLevel()) {
            progress.setPassLevel(p1);
            if (p1 > progress.getBestLevel()) progress.setBestLevel(p1);
            shiLianRepo.save(progress);
        }
        return progress;
    }

    @Transactional
    public boolean claimShiLian(Long roleId, int p1) {
        // p1 = item index to claim
        ShiLianProgress progress = getShiLian(roleId);
        if (p1 <= progress.getPassLevel()) {
            progress.setUseItem(progress.getUseItem() | (1 << p1));
            shiLianRepo.save(progress);
            return true;
        }
        return false;
    }

    // === GuMo (Demon-Lock Tower) ===

    public Map<String, Object> getGuMo(Long roleId) {
        GuMoMeta meta = guMoMetaRepo.findByRoleId(roleId)
                .orElseGet(() -> guMoMetaRepo.save(
                        GuMoMeta.builder().roleId(roleId).dayReward(0).lastDayLevel(0).build()));
        List<GuMoLayer> layers = guMoLayerRepo.findByRoleId(roleId);
        return Map.of("dayReward", meta.getDayReward(), "lastdayLevel", meta.getLastDayLevel(), "layers", layers);
    }

    @Transactional
    public Map<String, Object> challengeGuMo(Long roleId, int layerId) {
        GuMoLayer layer = guMoLayerRepo.findByRoleIdAndLayerId(roleId, layerId)
                .orElseGet(() -> GuMoLayer.builder()
                        .roleId(roleId).layerId(layerId).starFlag(0).boxFlag(false).build());
        if (layer.getStarFlag() == 0) {
            layer.setStarFlag(3); // default 3 stars on first clear
            guMoLayerRepo.save(layer);
        }
        return getGuMo(roleId);
    }

    @Transactional
    public boolean claimGuMo(Long roleId, int layerId) {
        GuMoLayer layer = guMoLayerRepo.findByRoleIdAndLayerId(roleId, layerId).orElse(null);
        if (layer != null && layer.getStarFlag() > 0 && !layer.getBoxFlag()) {
            layer.setBoxFlag(true);
            guMoLayerRepo.save(layer);
            return true;
        }
        return false;
    }
}
