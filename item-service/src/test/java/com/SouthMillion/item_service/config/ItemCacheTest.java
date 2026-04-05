package com.SouthMillion.item_service.config;

import com.SouthMillion.item_service.service.client.ConfigServiceFeign;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.SouthMillion.dto.item.ItemMetaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemCache Tests")
class ItemCacheTest {

    @Mock private ConfigServiceFeign feign;
    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> valueOps;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ItemCache cache;

    @BeforeEach
    void setUp() {
        ItemProps props = new ItemProps(
                null,
                List.of("gameworld/item/equipment.json"),
                300,
                10_000,
                300,
                "item:meta:");

        cache = new ItemCache(feign, props, redis, objectMapper, new SimpleMeterRegistry());

        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenReturn(null);
    }

    @Test
    @DisplayName("getOrLoad() resolves equipment attr groups via unpack.json")
    void getOrLoad_mapsEquipmentFieldsFromEquipmentJson() throws Exception {
        JsonNode catalog = objectMapper.readTree("""
                {
                  "hujian": [
                    {
                      "id": "1004",
                      "part": "0",
                      "level": "1",
                      "quality": "5",
                      "speed_max": "32",
                      "hp_max": "333",
                      "att_max": "70",
                      "def_max": "21",
                      "exp": "9",
                      "frist_att": "4",
                      "second_att": "5",
                      "item_type": "2",
                      "sellprice": "1",
                      "pile_limit": "1",
                      "invalid_time": "0",
                      "is_special": "0",
                      "is_virtual": "0"
                    }
                  ]
                }
                """);
        JsonNode unpack = objectMapper.readTree("""
                {
                  "color_att": [
                    { "att_group": "4", "att_type": "11", "att_num_max": "200" },
                    { "att_group": "5", "att_type": "25", "att_num_max": "1" }
                  ]
                }
                """);

        given(feign.getFile(eq("gameworld/item/equipment.json"), isNull()))
                .willReturn(ResponseEntity.ok(catalog));
        given(feign.getFile(eq("gameworld/logicconfig/unpack.json"), isNull()))
                .willReturn(ResponseEntity.ok(unpack));

        ItemMetaDTO dto = cache.getOrLoad(1004);

        assertThat(dto.itemType()).isEqualTo("equip");
        assertThat(dto.quality()).isEqualTo(5);
        assertThat(dto.equipType()).isEqualTo(0);
        assertThat(dto.level()).isEqualTo(1);
        assertThat(dto.hp()).isEqualTo(333);
        assertThat(dto.attack()).isEqualTo(70);
        assertThat(dto.defend()).isEqualTo(21);
        assertThat(dto.speed()).isEqualTo(32);
        assertThat(dto.attrType1()).isEqualTo(11);
        assertThat(dto.attrValue1()).isEqualTo(200);
        assertThat(dto.attrType2()).isEqualTo(25);
        assertThat(dto.attrValue2()).isEqualTo(1);
    }
}
