package com.SouthMillion.serverInfo_service.service;

import com.SouthMillion.serverInfo_service.entity.ServerInfo;
import com.SouthMillion.serverInfo_service.repository.ServerInfoRepository;
import org.SouthMillion.dto.serverInfor.ServerInfoDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServerInfoService Tests")
class ServerInfoServiceTest {

    @Mock private ServerInfoRepository repository;
    @Mock private RedisService         redisService;

    @InjectMocks private ServerInfoService serverInfoService;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static ServerInfo entity(Long startTime, Long combineTime) {
        ServerInfo e = new ServerInfo();
        e.setId(1);
        e.setRealStartTime(startTime);
        e.setRealCombineTime(combineTime);
        return e;
    }

    private static ServerInfoDto dto(Long startTime, Long combineTime) {
        ServerInfoDto d = new ServerInfoDto();
        d.setRealStartTime(startTime);
        d.setRealCombineTime(combineTime);
        return d;
    }

    // =========================================================
    // getServerInfo()
    // =========================================================
    @Nested
    @DisplayName("getServerInfo()")
    class GetServerInfo {

        @Test
        @DisplayName("TC-SRVINFO-001 [P] Cache hit – tra ve DTO tu cache")
        void getServerInfo_cacheHit_returnsCached() {
            ServerInfoDto cached = dto(1000L, 2000L);
            given(redisService.getServerInfo()).willReturn(cached);

            ServerInfoDto result = serverInfoService.getServerInfo();

            assertThat(result).isEqualTo(cached);
            then(repository).should(never()).findById(any());
        }

        @Test
        @DisplayName("TC-SRVINFO-002 [P] Cache miss, tim thay trong DB – luu cache va tra ve DTO")
        void getServerInfo_cacheMiss_loadsFromDb() {
            given(redisService.getServerInfo()).willReturn(null);
            given(repository.findById(1)).willReturn(Optional.of(entity(1000L, 2000L)));

            ServerInfoDto result = serverInfoService.getServerInfo();

            assertThat(result.getRealStartTime()).isEqualTo(1000L);
            assertThat(result.getRealCombineTime()).isEqualTo(2000L);
            then(redisService).should().setServerInfo(any());
        }

        @Test
        @DisplayName("TC-SRVINFO-003 [N] Cache miss va DB rong – nem RuntimeException")
        void getServerInfo_cacheMissAndNotInDb_throws() {
            given(redisService.getServerInfo()).willReturn(null);
            given(repository.findById(1)).willReturn(Optional.empty());

            assertThatThrownBy(() -> serverInfoService.getServerInfo())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    // =========================================================
    // updateServerInfo()
    // =========================================================
    @Nested
    @DisplayName("updateServerInfo()")
    class UpdateServerInfo {

        @Test
        @DisplayName("TC-SRVINFO-004 [P] Cap nhat thong tin server – luu DB, xoa cache va cap nhat")
        void updateServerInfo_savesAndUpdatesCache() {
            ServerInfoDto input = dto(9000L, 8000L);
            given(repository.findById(1)).willReturn(Optional.of(entity(1000L, 2000L)));
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            ServerInfoDto result = serverInfoService.updateServerInfo(input);

            assertThat(result).isEqualTo(input);
            then(repository).should().save(any(ServerInfo.class));
            then(redisService).should().evictServerInfo();
            then(redisService).should().setServerInfo(input);
        }

        @Test
        @DisplayName("TC-SRVINFO-005 [P] DB rong – tao entity moi va luu")
        void updateServerInfo_dbEmpty_createsNewEntity() {
            ServerInfoDto input = dto(5000L, null);
            given(repository.findById(1)).willReturn(Optional.empty());
            given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

            serverInfoService.updateServerInfo(input);

            then(repository).should().save(argThat(e ->
                    e.getRealCombineTime() == 0L)); // null combineTime defaults to 0
        }
    }

    // =========================================================
    // getServerCombineTime()
    // =========================================================
    @Nested
    @DisplayName("getServerCombineTime()")
    class GetServerCombineTime {

        @Test
        @DisplayName("TC-SRVINFO-006 [P] Cache co combine time – tra ve tu cache")
        void getServerCombineTime_cacheHitWithTime_returnsFromCache() {
            ServerInfoDto cached = dto(1000L, 5000L);
            given(redisService.getServerInfo()).willReturn(cached);

            Long result = serverInfoService.getServerCombineTime();

            assertThat(result).isEqualTo(5000L);
            then(repository).should(never()).findById(any());
        }

        @Test
        @DisplayName("TC-SRVINFO-007 [P] Cache co combine time null – lay tu DB")
        void getServerCombineTime_cacheHitNullCombineTime_loadsFromDb() {
            ServerInfoDto cachedNullTime = dto(1000L, null);
            given(redisService.getServerInfo()).willReturn(cachedNullTime);
            given(repository.findById(1)).willReturn(Optional.of(entity(1000L, 7000L)));

            Long result = serverInfoService.getServerCombineTime();

            assertThat(result).isEqualTo(7000L);
        }

        @Test
        @DisplayName("TC-SRVINFO-008 [P] Cache miss, tim thay trong DB – tra ve combine time")
        void getServerCombineTime_cacheMiss_loadsFromDb() {
            given(redisService.getServerInfo()).willReturn(null);
            given(repository.findById(1)).willReturn(Optional.of(entity(1000L, 6000L)));

            Long result = serverInfoService.getServerCombineTime();

            assertThat(result).isEqualTo(6000L);
            then(redisService).should().setServerInfo(any());
        }

        @Test
        @DisplayName("TC-SRVINFO-009 [N] Cache miss va DB rong – nem RuntimeException")
        void getServerCombineTime_notFound_throws() {
            given(redisService.getServerInfo()).willReturn(null);
            given(repository.findById(1)).willReturn(Optional.empty());

            assertThatThrownBy(() -> serverInfoService.getServerCombineTime())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }
}
