package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.config.ItemCache;
import org.SouthMillion.dto.item.ItemMetaDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService Tests")
class ItemServiceTest {

    @Mock private ItemCache cache;

    @InjectMocks private ItemService itemService;

    private static ItemMetaDTO dto(int itemId, String type, Integer pileLimit) {
        return new ItemMetaDTO(itemId, type, 1, 0L, 100L, pileLimit, 0L, false, false);
    }

    // =========================================================
    // meta
    // =========================================================
    @Nested
    @DisplayName("meta()")
    class Meta {

        @Test
        @DisplayName("TC-ITEM-001 [P] Item ton tai – tra ve ItemMetaDTO")
        void meta_found_returnsDto() {
            ItemMetaDTO expected = dto(10, "EQUIP", 1);
            given(cache.getOrLoad(10)).willReturn(expected);

            ItemMetaDTO result = itemService.meta(10);

            assertThat(result).isEqualTo(expected);
            assertThat(result.itemType()).isEqualTo("EQUIP");
        }

        @Test
        @DisplayName("TC-ITEM-002 [N] Item khong ton tai – nem ItemNotFoundException")
        void meta_notFound_throwsException() {
            given(cache.getOrLoad(999)).willThrow(new ItemCache.ItemNotFoundException(999));

            assertThatThrownBy(() -> itemService.meta(999))
                    .isInstanceOf(ItemCache.ItemNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // =========================================================
    // batch
    // =========================================================
    @Nested
    @DisplayName("batch()")
    class Batch {

        @Test
        @DisplayName("TC-ITEM-003 [P] Lay nhieu item theo danh sach id")
        void batch_multipleIds_returnsMap() {
            ItemMetaDTO dto1 = dto(1, "EQUIP", 1);
            ItemMetaDTO dto2 = dto(2, "CONSUMABLE", 99);
            given(cache.getOrLoad(1)).willReturn(dto1);
            given(cache.getOrLoad(2)).willReturn(dto2);

            Map<Integer, ItemMetaDTO> result = itemService.batch(List.of(1, 2));

            assertThat(result).hasSize(2);
            assertThat(result.get(1).itemType()).isEqualTo("EQUIP");
            assertThat(result.get(2).itemType()).isEqualTo("CONSUMABLE");
        }

        @Test
        @DisplayName("TC-ITEM-004 [P] Danh sach rong – tra ve map rong")
        void batch_emptyList_returnsEmptyMap() {
            Map<Integer, ItemMetaDTO> result = itemService.batch(List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("TC-ITEM-004A [P] Mot item loi runtime khong lam fail ca batch")
        void batch_runtimeErrorOnOneItem_returnsPartialMap() {
            ItemMetaDTO dto1 = dto(1, "EQUIP", 1);
            given(cache.getOrLoad(1)).willReturn(dto1);
            given(cache.getOrLoad(40420)).willThrow(new IllegalStateException("broken data"));

            Map<Integer, ItemMetaDTO> result = itemService.batch(List.of(1, 40420));

            assertThat(result).hasSize(1);
            assertThat(result.get(1)).isEqualTo(dto1);
            assertThat(result).doesNotContainKey(40420);
        }
    }

    // =========================================================
    // typeOf
    // =========================================================
    @Nested
    @DisplayName("typeOf()")
    class TypeOf {

        @Test
        @DisplayName("TC-ITEM-005 [P] Lay loai item theo id")
        void typeOf_found_returnsType() {
            given(cache.getOrLoad(5)).willReturn(dto(5, "MATERIAL", 10));

            String type = itemService.typeOf(5);

            assertThat(type).isEqualTo("MATERIAL");
        }
    }

    // =========================================================
    // exists
    // =========================================================
    @Nested
    @DisplayName("exists()")
    class Exists {

        @Test
        @DisplayName("TC-ITEM-006 [P] Item ton tai – tra ve true")
        void exists_found_returnsTrue() {
            given(cache.getOrLoad(7)).willReturn(dto(7, "EQUIP", 1));

            assertThat(itemService.exists(7)).isTrue();
        }

        @Test
        @DisplayName("TC-ITEM-007 [N] Item khong ton tai – tra ve false")
        void exists_notFound_returnsFalse() {
            given(cache.getOrLoad(0)).willThrow(new ItemCache.ItemNotFoundException(0));

            assertThat(itemService.exists(0)).isFalse();
        }
    }

    // =========================================================
    // validStack
    // =========================================================
    @Nested
    @DisplayName("validStack()")
    class ValidStack {

        @Test
        @DisplayName("TC-ITEM-008 [P] So luong hop le (count <= pileLimit)")
        void validStack_countWithinLimit_returnsTrue() {
            given(cache.getOrLoad(1)).willReturn(dto(1, "CONSUMABLE", 99));

            assertThat(itemService.validStack(1, 50)).isTrue();
        }

        @Test
        @DisplayName("TC-ITEM-009 [N] So luong vuot qua pileLimit – tra ve false")
        void validStack_countExceedsLimit_returnsFalse() {
            given(cache.getOrLoad(1)).willReturn(dto(1, "CONSUMABLE", 10));

            assertThat(itemService.validStack(1, 11)).isFalse();
        }

        @Test
        @DisplayName("TC-ITEM-010 [N] pileLimit bang 0 – tra ve false")
        void validStack_zeroLimit_returnsFalse() {
            given(cache.getOrLoad(2)).willReturn(dto(2, "EQUIP", 0));

            assertThat(itemService.validStack(2, 1)).isFalse();
        }

        @Test
        @DisplayName("TC-ITEM-011 [N] pileLimit null – tra ve false")
        void validStack_nullLimit_returnsFalse() {
            given(cache.getOrLoad(3)).willReturn(dto(3, "EQUIP", null));

            assertThat(itemService.validStack(3, 1)).isFalse();
        }

        @Test
        @DisplayName("TC-ITEM-012 [P] count bang chinh xac pileLimit – hop le")
        void validStack_countEqualsLimit_returnsTrue() {
            given(cache.getOrLoad(4)).willReturn(dto(4, "CONSUMABLE", 5));

            assertThat(itemService.validStack(4, 5)).isTrue();
        }
    }
}
