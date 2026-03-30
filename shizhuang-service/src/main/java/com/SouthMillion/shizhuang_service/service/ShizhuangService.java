package com.SouthMillion.shizhuang_service.service;

import com.SouthMillion.shizhuang_service.client.WalletFeignClient;
import com.SouthMillion.shizhuang_service.entity.PlayerShizhuang;
import com.SouthMillion.shizhuang_service.repository.PlayerShizhuangRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.ShiZhuang.ShizhuangDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShizhuangService {

    private final PlayerShizhuangRepository repository;
    private final WalletFeignClient         walletFeignClient;

    // ------------------------------------------------------------------
    // listByRole — lấy tất cả shizhuang của role
    // ------------------------------------------------------------------
    public ShizhuangDTOs.ShizhuangListResp listByRole(Long roleId) {
        List<PlayerShizhuang> entities = repository.findByRoleId(roleId);
        List<ShizhuangDTOs.ShizhuangInfo> items = entities.stream()
                .map(this::toInfo)
                .collect(Collectors.toList());
        return ShizhuangDTOs.ShizhuangListResp.builder()
                .totalCount(items.size())
                .items(items)
                .build();
    }

    // ------------------------------------------------------------------
    // getInfo — lấy thông tin một shizhuang; trả null nếu không tìm thấy
    // ------------------------------------------------------------------
    public ShizhuangDTOs.ShizhuangInfo getInfo(Long roleId, int shizhuangId) {
        return repository.findByRoleIdAndShizhuangId(roleId, shizhuangId)
                .map(this::toInfo)
                .orElse(null);
    }

    // ------------------------------------------------------------------
    // activate — kích hoạt shizhuang (tạo mới nếu chưa có)
    // ------------------------------------------------------------------
    @Transactional
    public ShizhuangDTOs.OperationResp activate(ShizhuangDTOs.ActivateReq req) {
        Long roleIdLong = Long.parseLong(req.getRoleId());
        Optional<PlayerShizhuang> opt =
                repository.findByRoleIdAndShizhuangId(roleIdLong, req.getShizhuangId());

        PlayerShizhuang shizhuang = opt.orElseGet(() -> PlayerShizhuang.builder()
                .roleId(roleIdLong)
                .shizhuangId(req.getShizhuangId())
                .build());

        shizhuang.setActivated(true);
        repository.save(shizhuang);
        return ShizhuangDTOs.OperationResp.ok();
    }

    // ------------------------------------------------------------------
    // wear — mặc shizhuang (chỉ được nếu đã kích hoạt)
    // ------------------------------------------------------------------
    @Transactional
    public ShizhuangDTOs.OperationResp wear(ShizhuangDTOs.WearReq req) {
        Long roleIdLong = Long.parseLong(req.getRoleId());
        Optional<PlayerShizhuang> current =
                repository.findByRoleIdAndWearingTrue(roleIdLong);

        Optional<PlayerShizhuang> targetOpt =
                repository.findByRoleIdAndShizhuangId(roleIdLong, req.getShizhuangId());

        if (targetOpt.isEmpty()) {
            return ShizhuangDTOs.OperationResp.fail("Shizhuang not found");
        }

        PlayerShizhuang target = targetOpt.get();
        if (!target.isActivated()) {
            return ShizhuangDTOs.OperationResp.fail("Shizhuang not activated");
        }

        current.ifPresent(c -> {
            c.setWearing(false);
            repository.save(c);
        });

        target.setWearing(true);
        repository.save(target);
        return ShizhuangDTOs.OperationResp.ok();
    }

    // ------------------------------------------------------------------
    // levelUp — nâng cấp shizhuang, trừ vàng qua WalletFeignClient
    // ------------------------------------------------------------------
    @Transactional
    public ShizhuangDTOs.OperationResp levelUp(ShizhuangDTOs.LevelUpReq req) {
        Long roleIdLong = Long.parseLong(req.getRoleId());
        Optional<PlayerShizhuang> opt =
                repository.findByRoleIdAndShizhuangId(roleIdLong, req.getShizhuangId());

        if (opt.isEmpty()) {
            return ShizhuangDTOs.OperationResp.fail("Shizhuang not found");
        }

        PlayerShizhuang shizhuang = opt.get();
        if (!shizhuang.isActivated()) {
            return ShizhuangDTOs.OperationResp.fail("Shizhuang not activated");
        }

        try {
            walletFeignClient.deductCurrency(Map.of(
                    "roleId", req.getRoleId(),
                    "currencyType", "gold",
                    "amount", 100
            ));
        } catch (Exception e) {
            log.warn("LevelUp deductCurrency failed: {}", e.getMessage());
            return ShizhuangDTOs.OperationResp.fail(e.getMessage());
        }

        shizhuang.setLevel(shizhuang.getLevel() + 1);
        repository.save(shizhuang);
        return ShizhuangDTOs.OperationResp.ok();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------
    private ShizhuangDTOs.ShizhuangInfo toInfo(PlayerShizhuang e) {
        return ShizhuangDTOs.ShizhuangInfo.builder()
                .roleId(String.valueOf(e.getRoleId()))
                .shizhuangId(e.getShizhuangId())
                .level(e.getLevel())
                .star(e.getStar())
                .activated(e.isActivated())
                .wearing(e.isWearing())
                .build();
    }
}
