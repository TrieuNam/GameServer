package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.entity.AdRewardClaim;
import com.SouthMillion.role_service.repository.AdRewardClaimRepository;
import com.SouthMillion.role_service.service.client.WalletFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.advertisment.AdvertisementDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final AdRewardClaimRepository repo;

    @Autowired(required = false)
    private WalletFeign walletFeign;

    @Transactional
    public AdvertisementDTOs.AdInfo claim(AdvertisementDTOs.AdFetchReq req) {
        LocalDate todayUtc = Instant.now().atZone(ZoneOffset.UTC).toLocalDate();
        boolean already = repo.findByUserIdAndSeqAndClaimDay(req.userId(), req.seq(), todayUtc).isPresent();
        if (!already) {
            AdRewardClaim c = new AdRewardClaim();
            c.setUserId(req.userId());
            c.setSeq(req.seq());
            c.setDiamond(req.isDia() == 1);
            c.setParam(req.param());
            c.setClaimDay(todayUtc);
            try {
                repo.saveAndFlush(c);
                creditWallet(req);
            } catch (DataIntegrityViolationException ex) {
                // unique key hit by race — do not credit
            }
        }
        Instant nextDayStart = todayUtc.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new AdvertisementDTOs.AdInfo(req.seq(), true, nextDayStart);
    }

    private void creditWallet(AdvertisementDTOs.AdFetchReq req) {
        if (walletFeign == null) {
            log.warn("[Ad] walletFeign not available, skipping reward credit for userId={} seq={}", req.userId(), req.seq());
            return;
        }
        boolean isDia = req.isDia() == 1;
        long itemId = isDia ? 2L : 1L;
        long amount = isDia ? 10L : 1000L;
        try {
            walletFeign.batchAdd(WalletDTOs.BatchReq.builder()
                    .roleId(req.userId())
                    .changes(List.of(WalletDTOs.Change.builder().itemId(itemId).amount(amount).build()))
                    .reason(301)
                    .idemKey("ad-" + req.userId() + "-" + req.seq() + "-" + LocalDate.now(ZoneOffset.UTC))
                    .build());
            log.info("[Ad] Credited {} {} to userId={} seq={}", amount, isDia ? "diamond" : "gold", req.userId(), req.seq());
        } catch (Exception e) {
            log.warn("[Ad] Wallet credit failed for userId={} seq={}: {}", req.userId(), req.seq(), e.getMessage());
        }
    }
}
