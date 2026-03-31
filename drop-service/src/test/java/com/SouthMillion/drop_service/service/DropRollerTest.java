package com.SouthMillion.drop_service.service;

import com.SouthMillion.drop_service.config.AppProperties;
import com.SouthMillion.drop_service.repository.DropRepository;
import com.SouthMillion.drop_service.service.client.BagFeign;
import com.SouthMillion.drop_service.service.client.ItemMetaFeign;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagAddItemResp;
import org.SouthMillion.dto.drop.CompiledDrop;
import org.SouthMillion.dto.drop.DropXml;
import org.SouthMillion.dto.drop.RollRequest;
import org.SouthMillion.dto.drop.RollResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("DropRoller Tests")
class DropRollerTest {

    @Mock private DropRepository repo;
    @Mock private PityService pity;
    @Mock private AppProperties props;
    @Mock private ItemMetaFeign itemMetaFeign;
    @Mock private BagFeign bagFeign;
    @Mock private AppProperties.Bag bagProps;

    private DropRoller dropRoller;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        dropRoller = new DropRoller(repo, pity, props, Optional.of(itemMetaFeign), Optional.of(bagFeign));
    }

    @Test
    @DisplayName("TC-DROP-001 [P] applyToBag gui quality, bind va bagType vao bag-service")
    void roll_applyToBag_propagatesQualityBindAndBagType() {
        given(repo.getCompiled(99)).willReturn(compiledDrop(201, 2, 1));
        given(pity.enabled()).willReturn(false);
        given(props.getBag()).willReturn(bagProps);
        given(props.getItem()).willReturn(new AppProperties.Item());
        given(bagProps.isApplyEnabled()).willReturn(true);
        given(bagProps.getDefaultBagType()).willReturn(4);
        given(itemMetaFeign.batchMeta("201")).willReturn(Map.of("201", Map.of("quality", 6, "bag_type", 8)));
        given(bagFeign.add(any(BagAddItemReq.class))).willReturn(BagAddItemResp.ok(List.of()));

        RollRequest req = new RollRequest();
        req.setRoleId("1001");
        req.setDropId(99);
        req.setTimes(1);
        req.getOptions().setApplyToBag(true);

        RollResult result = dropRoller.roll(req);

        assertThat(result.getApply().isSuccess()).isTrue();
        ArgumentCaptor<BagAddItemReq> captor = ArgumentCaptor.forClass(BagAddItemReq.class);
        then(bagFeign).should().add(captor.capture());
        BagAddItemReq.Item item = captor.getValue().getItems().getFirst();
        assertThat(item.getQuality()).isEqualTo(6);
        assertThat(item.getBagType()).isEqualTo(8);
        assertThat(item.getBound()).isTrue();
    }

    private CompiledDrop compiledDrop(int itemId, int num, int bind) {
        DropXml.DropItemProb item = new DropXml.DropItemProb();
        item.setItemId(itemId);
        item.setNum(num);
        item.setIsBind(bind);
        item.setBroadcast(0);
        item.setProb(100);

        DropXml.DropItemProbList list = new DropXml.DropItemProbList();
        list.setItems(List.of(item));

        DropXml dropXml = new DropXml();
        dropXml.setDropId(99);
        dropXml.setDropItemProbList(list);
        return new CompiledDrop(dropXml);
    }
}