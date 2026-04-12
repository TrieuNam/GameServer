package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.config.LimitCoreConfigCache;
import com.SouthMillion.role_service.entity.PlayerLimitCore;
import com.SouthMillion.role_service.repository.PlayerLimitCoreRepository;
import com.SouthMillion.role_service.service.client.BagFeign;
import com.SouthMillion.role_service.service.client.LimitCoreItemFeign;
import com.SouthMillion.role_service.service.client.WalletFeign;
import org.SouthMillion.dto.bag.BagDTOs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimitCoreServiceTest {

    @Mock
    private PlayerLimitCoreRepository repo;
    @Mock
    private LimitCoreConfigCache cfg;
    @Mock
    private LimitCoreItemFeign itemFeign;
    @Mock
    private WalletFeign walletFeign;
    @Mock
    private BagFeign bagFeign;

    @InjectMocks
    private LimitCoreService service;

    @Test
    void getAllLevels_padsMissingTypesToSix() {
        PlayerLimitCore t1 = new PlayerLimitCore();
        t1.setRoleId(2001L);
        t1.setLimitType(1);
        t1.setLevel(4);

        PlayerLimitCore t6 = new PlayerLimitCore();
        t6.setRoleId(2001L);
        t6.setLimitType(6);
        t6.setLevel(1);

        when(repo.findByRoleId(2001L)).thenReturn(List.of(t1, t6));

        List<Integer> levels = service.getAllLevels(2001L);

        assertThat(levels).containsExactly(4, 0, 0, 0, 0, 1);
    }

    @Test
    void doDraw_capsByPoolSizeEvenWhenRewardNumIsHigher() {
        when(cfg.getRewardNum()).thenReturn(3);
        when(cfg.getCoreboxEntries(0)).thenReturn(List.of(
                new LimitCoreConfigCache.CoreboxEntry(0, 40500, 1, 1, 1),
                new LimitCoreConfigCache.CoreboxEntry(0, 40501, 1, 1, 1)
        ));
        when(repo.findByRoleId(2001L)).thenReturn(List.of());
        when(bagFeign.add(any())).thenReturn(List.of(BagDTOs.ItemView.builder().itemId(40500).num(1).build()));

        Map<String, Object> result = service.doDraw(2001L, 0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> drawnItems = (List<Map<String, Object>>) result.get("drawnItems");
        assertThat(drawnItems).hasSize(2);

        ArgumentCaptor<org.SouthMillion.dto.bag.BagAddItemReq> reqCaptor =
                ArgumentCaptor.forClass(org.SouthMillion.dto.bag.BagAddItemReq.class);
        verify(bagFeign).add(reqCaptor.capture());
        assertThat(reqCaptor.getValue().getItems()).hasSize(2);
    }

    @Test
    void doDraw_paidBoxWithoutEnoughDiamond_throws() {
        when(cfg.getPrice1()).thenReturn(100);
        when(walletFeign.hasEnough(anyString(), anyString(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> service.doDraw(2001L, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("kim cương");
    }
}
