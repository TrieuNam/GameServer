package com.SouthMillion.pet_service;

import com.SouthMillion.pet_service.model.entity.PetGuardState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PetGuardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetStateAndSaveState() throws Exception {
        Long roleId = 12345L;
        // 1. Save state
        String json = "{" +
                "\"roleId\":" + roleId + "," +
                "\"passLevel\":2," +
                "\"fetchFlag\":3" +
                "}";
        mockMvc.perform(post("/api/petguard/state")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());

        // 2. Get state
        mockMvc.perform(get("/api/petguard/state/" + roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value(roleId))
                .andExpect(jsonPath("$.passLevel").value(2))
                .andExpect(jsonPath("$.fetchFlag").value(3));
    }
}
