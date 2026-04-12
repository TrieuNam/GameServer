package com.SouthMillion.task_service.controller;

import com.SouthMillion.task_service.exception.FashionBusinessException;
import com.SouthMillion.task_service.exception.FashionErrorCodes;
import com.SouthMillion.task_service.service.ShiZhuangService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShiZhuangController error contract")
class ShiZhuangControllerAdviceTest {

    private MockMvc mockMvc;

        @Mock
    private ShiZhuangService service;

        @InjectMocks
        private ShiZhuangController controller;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(controller)
                                .setControllerAdvice(new ShiZhuangExceptionHandler())
                                .build();
        }

    @Test
    @DisplayName("levelup thiếu item trả 400 + code/itemId")
    void levelUp_notEnoughItem_returns400WithItemId() throws Exception {
        willThrow(FashionBusinessException.notEnoughItem(120001, "Không đủ vật phẩm nâng cấp thời trang"))
                .given(service)
                .levelUpClothes("1", 301, 0);

        mockMvc.perform(post("/api/shizhuang/levelup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("roleId", "1")
                        .param("clothesId", "301")
                        .param("consumeMode", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(FashionErrorCodes.NOT_ENOUGH_ITEM))
                .andExpect(jsonPath("$.itemId").value(120001));
    }

    @Test
    @DisplayName("levelup thiếu vàng trả 400 + code/itemId")
    void levelUp_notEnoughGold_returns400WithGoldItemId() throws Exception {
        willThrow(FashionBusinessException.notEnoughCurrency(40000, "Không đủ vàng nâng cấp thời trang"))
                .given(service)
                .levelUpClothes("2", 302, 0);

        mockMvc.perform(post("/api/shizhuang/levelup")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("roleId", "2")
                        .param("clothesId", "302")
                        .param("consumeMode", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(FashionErrorCodes.NOT_ENOUGH_CURRENCY))
                .andExpect(jsonPath("$.itemId").value(40000));
    }
}
