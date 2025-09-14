package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.entity.AdRewardClaim;
import com.SouthMillion.role_service.repository.AdRewardClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.advertisment.AdvertisementDTOs;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final AdRewardClaimRepository repo;

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
            } catch (DataIntegrityViolationException ex) {
                // unique key hit by race
            }
        }
        Instant nextDayStart = todayUtc.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new AdvertisementDTOs.AdInfo(req.seq(), true, nextDayStart);
    }
}
