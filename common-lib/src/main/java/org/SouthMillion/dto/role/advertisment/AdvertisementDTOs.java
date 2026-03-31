package org.SouthMillion.dto.role.advertisment;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class AdvertisementDTOs {
    public record AdFetchReq(@NotBlank String userId, String roleId, int seq, int isDia, Integer param) {
    }

    public record AdInfo(int seq, boolean claimedToday, Instant nextClaimAt) {
    }
}