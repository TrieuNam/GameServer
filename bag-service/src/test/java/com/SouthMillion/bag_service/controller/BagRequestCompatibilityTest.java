package com.SouthMillion.bag_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.SouthMillion.dto.bag.BagConsumeReq;
import org.SouthMillion.dto.bag.BagDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Bag request compatibility tests")
class BagRequestCompatibilityTest {

    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("README-style grant payload uses quantity alias successfully")
    void grantReq_quantityAlias_deserializes() throws Exception {
        String json = """
                {
                  "userId": "1001",
                  "roleId": "2001",
                  "eventId": "evt-1",
                  "items": [
                    {"itemId": 50001, "quantity": 3, "bind": 1, "bag_type": 2}
                  ]
                }
                """;

        BagDTOs.GrantReq req = om.readValue(json, BagDTOs.GrantReq.class);

        assertThat(req.getItems()).hasSize(1);
        assertThat(req.getItems().getFirst().getNum()).isEqualTo(3);
        assertThat(req.getItems().getFirst().getBind()).isEqualTo(1);
        assertThat(req.getItems().getFirst().getBagType()).isEqualTo(2);
    }

    @Test
    @DisplayName("Internal add payload supports quantity and bind aliases")
    void bagAddItemReq_aliases_deserialize() throws Exception {
        String json = """
                {
                  "userId": 1001,
                  "roleId": 2001,
                  "items": [
                    {"itemId": 50001, "quantity": 2, "bind": true, "quality": 4, "bag_type": 1}
                  ]
                }
                """;

        BagAddItemReq req = om.readValue(json, BagAddItemReq.class);

        assertThat(req.getItems()).hasSize(1);
        assertThat(req.getItems().getFirst().getAmount()).isEqualTo(2);
        assertThat(req.getItems().getFirst().getBound()).isTrue();
        assertThat(req.getItems().getFirst().getQuality()).isEqualTo(4);
        assertThat(req.getItems().getFirst().getBagType()).isEqualTo(1);
    }

    @Test
    @DisplayName("Internal consume payload supports quantity alias on amount and costs")
    void bagConsumeReq_quantityAlias_deserializes() throws Exception {
        String json = """
                {
                  "userId": 1001,
                  "roleId": 2001,
                  "itemId": 50001,
                  "quantity": 2,
                  "costs": [
                    {"itemId": 40003, "quantity": 5}
                  ]
                }
                """;

        BagConsumeReq req = om.readValue(json, BagConsumeReq.class);

        assertThat(req.getAmount()).isEqualTo(2);
        assertThat(req.getCosts()).hasSize(1);
        assertThat(req.getCosts().getFirst().getAmount()).isEqualTo(5);
    }
}

