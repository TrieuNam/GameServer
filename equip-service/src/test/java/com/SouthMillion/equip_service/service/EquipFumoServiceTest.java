package com.SouthMillion.equip_service.service;

import com.SouthMillion.equip_service.config.EquipProperties;
import com.SouthMillion.equip_service.entity.EquipFumoEntity;
import com.SouthMillion.equip_service.repository.EquipFumoRepository;
import com.SouthMillion.equip_service.service.client.BagInternalFeign;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EquipFumoService Tests")
class EquipFumoServiceTest {

    @Mock
    private EquipFumoRepository repo;

    @Mock
    private BagInternalFeign bagFeign;

    @Mock
    private EquipProperties props;

    @InjectMocks
    private EquipFumoService service;

    @Test
    @DisplayName("list() returns fixed-order slots and fills missing parts with null")
    void list_fixedOrderAndFillMissing() {
        given(props.getFumoSlotCount()).willReturn(4);

        EquipFumoEntity part0 = EquipFumoEntity.builder().roleId(1L).equipType(0).level(1).exp(10).endTime(100).build();
        EquipFumoEntity part2 = EquipFumoEntity.builder().roleId(1L).equipType(2).level(3).exp(30).endTime(300).build();
        given(repo.findByRoleId(1L)).willReturn(List.of(part2, part0));

        EquipFumoDTOs.FumoListResp resp = service.list(1L);

        assertThat(resp.fumoList()).hasSize(4);
        assertThat(resp.fumoList().get(0)).isNotNull();
        assertThat(resp.fumoList().get(0).level()).isEqualTo(1);
        assertThat(resp.fumoList().get(1)).isNull();
        assertThat(resp.fumoList().get(2)).isNotNull();
        assertThat(resp.fumoList().get(2).level()).isEqualTo(3);
        assertThat(resp.fumoList().get(3)).isNull();
    }

    @Test
    @DisplayName("addExp() consumes computed cost and persists result")
    void addExp_consumesAndSaves() {
        EquipFumoEntity current = EquipFumoEntity.builder().roleId(1L).equipType(0).level(0).exp(0).endTime(0).build();
        given(repo.findByRoleIdAndEquipType(1L, 0)).willReturn(Optional.of(current));

        given(props.getFumoCostCoreThresholdLevel()).willReturn(15);
        given(props.getFumoCostPowderBase()).willReturn(375);
        given(props.getFumoCostPowderStep()).willReturn(125);
        given(props.getFumoCostPowderItemId()).willReturn(40900);
        given(props.getFumoCostFaliItemId()).willReturn(40901);
        given(props.getFumoCostShengmingItemId()).willReturn(40902);
        given(props.getFumoCostSecondaryBase()).willReturn(2);
        given(props.getFumoCostSecondaryStep()).willReturn(2);

        given(props.getFumoBaseExp()).willReturn(100);
        given(props.getFumoGrowExp()).willReturn(0);
        given(props.getFumoMaxLevel()).willReturn(25);

        given(bagFeign.consume(any(BagConsumeReq.class))).willReturn(ResponseEntity.noContent().build());
        given(repo.save(any(EquipFumoEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        EquipFumoDTOs.FumoOneResp resp = service.addExp(new EquipFumoDTOs.AddExpReq("1", 0, 20, Map.of()));

        assertThat(resp.fumoData()).isNotNull();
        assertThat(resp.fumoData().level()).isEqualTo(0);
        assertThat(resp.fumoData().exp()).isEqualTo(20);

        ArgumentCaptor<BagConsumeReq> consumeCaptor = ArgumentCaptor.forClass(BagConsumeReq.class);
        verify(bagFeign).consume(consumeCaptor.capture());
        BagConsumeReq consumeReq = consumeCaptor.getValue();
        assertThat(consumeReq.getCosts()).hasSize(3);
        assertThat(consumeReq.getCosts()).anySatisfy(c -> {
            assertThat(c.getItemId()).isEqualTo(40900);
            assertThat(c.getAmount()).isEqualTo(375);
        });
        assertThat(consumeReq.getCosts()).anySatisfy(c -> {
            assertThat(c.getItemId()).isEqualTo(40901);
            assertThat(c.getAmount()).isEqualTo(2);
        });
        assertThat(consumeReq.getCosts()).anySatisfy(c -> {
            assertThat(c.getItemId()).isEqualTo(40902);
            assertThat(c.getAmount()).isEqualTo(2);
        });

        verify(repo).save(any(EquipFumoEntity.class));
    }

    @Test
    @DisplayName("addExp() returns null fumoData when consume fails")
    void addExp_consumeFailedReturnsNullData() {
        given(repo.findByRoleIdAndEquipType(1L, 0)).willReturn(Optional.empty());
        given(props.getFumoCostCoreThresholdLevel()).willReturn(15);
        given(props.getFumoCostPowderBase()).willReturn(375);
        given(props.getFumoCostPowderStep()).willReturn(125);
        given(props.getFumoCostPowderItemId()).willReturn(40900);
        given(props.getFumoCostFaliItemId()).willReturn(40901);
        given(props.getFumoCostShengmingItemId()).willReturn(40902);
        given(props.getFumoCostSecondaryBase()).willReturn(2);
        given(props.getFumoCostSecondaryStep()).willReturn(2);

        given(bagFeign.consume(any(BagConsumeReq.class))).willReturn(ResponseEntity.badRequest().build());

        EquipFumoDTOs.FumoOneResp resp = service.addExp(new EquipFumoDTOs.AddExpReq("1", 0, 20, Map.of()));

        assertThat(resp.fumoData()).isNull();
        verify(repo, never()).save(any(EquipFumoEntity.class));
    }

    @Test
    @DisplayName("transform() consumes powder and grants target materials by count")
    void transform_consumesAndAdds() {
        given(props.getFumoTransformFaliPowderCost()).willReturn(75);
        given(props.getFumoTransformShengmingPowderCost()).willReturn(75);
        given(props.getFumoTransformMohePowderCost()).willReturn(375);
        given(props.getFumoTransformPowderItemId()).willReturn(40900);
        given(props.getFumoTransformFaliItemId()).willReturn(40901);
        given(props.getFumoTransformShengmingItemId()).willReturn(40902);
        given(props.getFumoTransformMoheItemId()).willReturn(40903);

        given(bagFeign.consume(any(BagConsumeReq.class))).willReturn(ResponseEntity.noContent().build());
        given(bagFeign.add(any(BagAddItemReq.class))).willReturn(ResponseEntity.ok(List.of()));

        EquipFumoDTOs.OkResp resp = service.transform("1", 2, 1, 1);

        assertThat(resp.ok()).isTrue();

        ArgumentCaptor<BagConsumeReq> consumeCaptor = ArgumentCaptor.forClass(BagConsumeReq.class);
        verify(bagFeign).consume(consumeCaptor.capture());
        assertThat(consumeCaptor.getValue().getItemId()).isEqualTo(40900);
        assertThat(consumeCaptor.getValue().getAmount()).isEqualTo(600);

        ArgumentCaptor<BagAddItemReq> addCaptor = ArgumentCaptor.forClass(BagAddItemReq.class);
        verify(bagFeign).add(addCaptor.capture());
        List<BagAddItemReq.Item> items = addCaptor.getValue().getItems();
        assertThat(items).hasSize(3);
        assertThat(items).anySatisfy(i -> {
            assertThat(i.getItemId()).isEqualTo(40901);
            assertThat(i.getAmount()).isEqualTo(2);
        });
        assertThat(items).anySatisfy(i -> {
            assertThat(i.getItemId()).isEqualTo(40902);
            assertThat(i.getAmount()).isEqualTo(1);
        });
        assertThat(items).anySatisfy(i -> {
            assertThat(i.getItemId()).isEqualTo(40903);
            assertThat(i.getAmount()).isEqualTo(1);
        });
    }
}
