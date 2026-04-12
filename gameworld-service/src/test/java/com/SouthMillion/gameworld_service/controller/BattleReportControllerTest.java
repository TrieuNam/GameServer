package com.SouthMillion.gameworld_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BattleReportController.class)
@TestPropertySource(properties = "gameworld.battle.report-dir=${java.io.tmpdir}")
class BattleReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void serveBattleReport_supportsLegacyPrefixedFightdataPath() throws Exception {
        String battleId = "battle-report-test-" + UUID.randomUUID();
        Path reportFile = Paths.get(System.getProperty("java.io.tmpdir")).resolve(battleId);
        Files.writeString(reportFile, "battle-data", StandardCharsets.UTF_8);

        try {
            mockMvc.perform(get("/h02_dev_s11/fightdata/common_pve/0/" + battleId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                    .andExpect(content().string("battle-data"));
        } finally {
            Files.deleteIfExists(reportFile);
        }
    }

    @Test
    void serveBattleReport_allowsCrossOriginBrowserFetches() throws Exception {
        String battleId = "battle-report-cors-" + UUID.randomUUID();
        Path reportFile = Paths.get(System.getProperty("java.io.tmpdir")).resolve(battleId);
        Files.writeString(reportFile, "battle-data", StandardCharsets.UTF_8);

        try {
            mockMvc.perform(get("/h02_dev_s11/fightdata/common_pve/0/" + battleId)
                            .header("Origin", "http://localhost:8080"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "*"));
        } finally {
            Files.deleteIfExists(reportFile);
        }
    }
}
