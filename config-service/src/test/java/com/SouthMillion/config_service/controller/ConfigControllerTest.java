package com.SouthMillion.config_service.controller;

import com.SouthMillion.config_service.config.TestWebConfig;
import com.SouthMillion.config_service.core.ConfigStore;
import com.github.benmanes.caffeine.cache.Cache;

import org.SouthMillion.dto.config.ConfigFileData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ConfigController.class, properties = {
        "config.mode=classpath",
        "config.cache.l1-enabled=false"
})
@Import(TestWebConfig.class) // nạp ConfigProperties + Cache thật (được spy bên dưới)
class ConfigControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ConfigStore store;

    @MockitoSpyBean
    Cache<String, ConfigFileData> testL1Cache;

    private static ConfigFileData file(String key, String ct, String body, String etag, long lm, String rev) {
        return new ConfigFileData(
                key, rev, lm, etag, ct, body.getBytes(StandardCharsets.UTF_8)
        );
    }

    @BeforeEach
    void setUp() {
        // common stubs
        when(store.currentRevision()).thenReturn("rev-xyz");
    }

    @Test
    void version_ok() throws Exception {
        mvc.perform(get("/config/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value("rev-xyz"))
                .andExpect(jsonPath("$.ts").isNumber())
                .andExpect(jsonPath("$.mode").value("classpath"));
    }

    @Test
    void get_item_200_and_304_with_ifNoneMatch() throws Exception {
        long lm = Instant.now().getEpochSecond();
        var key = "config/gameworld/item/equipment";
        var data = file(key, "application/json", "{\"a\":1}", "\"etag1\"", lm, "rev-1");

        when(store.getFileByKey(key)).thenReturn(Optional.of(data));

        // Lần 1: 200 OK
        mvc.perform(get("/config/gameworld/item/equipment"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"etag1\""))
                .andExpect(header().string("X-Config-Revision", "rev-1"))
                .andExpect(header().string("Last-Modified", notNullValue()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"a\":1}"));

        // Lần 2: 304 Not Modified với If-None-Match
        mvc.perform(get("/config/gameworld/item/equipment").header("If-None-Match", "\"etag1\""))
                .andExpect(status().isNotModified());
    }

    @Test
    void get_logic_with_subpath() throws Exception {
        long lm = Instant.now().getEpochSecond();
        var key = "config/gameworld/logic/randactivity/activity_main";
        var data = file(key, "application/json", "{\"name\":\"activity_main\"}", "\"e-logic\"", lm, "rev-1");
        when(store.getFileByKey(key)).thenReturn(Optional.of(data));

        mvc.perform(get("/config/gameworld/logic/randactivity/activity_main"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"activity_main\"}"));
    }

    @Test
    void get_drop_xml() throws Exception {
        long lm = Instant.now().getEpochSecond();
        var key = "config/gameworld/drop/2000";
        var data = file(key, "application/xml", "<drop id=\"2000\"/>", "\"e-drop\"", lm, "rev-2");
        when(store.getFileByKey(key)).thenReturn(Optional.of(data));

        mvc.perform(get("/config/gameworld/drop/2000"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/xml"))
                .andExpect(content().xml("<drop id=\"2000\"/>"));
    }

    @Test
    void bundle_multiple_keys() throws Exception {
        long lm = Instant.now().getEpochSecond();
        var k1 = "config/gameworld/item/equipment";
        var k2 = "config/gameworld/drop/2000";

        when(store.getFileByKey(k1)).thenReturn(Optional.of(
                file(k1, "application/json", "{\"a\":1}", "\"e1\"", lm, "rev-1")));
        when(store.getFileByKey(k2)).thenReturn(Optional.of(
                file(k2, "application/xml", "<d/>", "\"e2\"", lm, "rev-1")));

        mvc.perform(get("/config/bundle").param("keys", k1 + "," + k2))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].key").value(k1))
                .andExpect(jsonPath("$[0].data").value("{\"a\":1}"))
                .andExpect(jsonPath("$[1].key").value(k2))
                .andExpect(jsonPath("$[1].data").value("<d/>"));
    }

    @Test
    void by_path_relative_under_config() throws Exception {
        long lm = Instant.now().getEpochSecond();
        var rel = "gameworld/battlemonstermanager.xml";
        var key = "config/" + rel;
        var data = file(key, "application/xml", "<bm/>", "\"bm\"", lm, "rev-3");

        when(store.getByRelativePath(rel)).thenReturn(Optional.of(data));

        mvc.perform(get("/config/by-path").param("p", rel))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/xml"))
                .andExpect(content().xml("<bm/>"));
    }

    @Test
    void index_and_list_paging() throws Exception {
        when(store.listItems()).thenReturn(List.of("equipment", "gemstone"));
        when(store.listLogic()).thenReturn(List.of("funopen"));
        when(store.listDrops()).thenReturn(List.of("2000", "2001", "2002"));
        when(store.listGlobal()).thenReturn(List.of("keyconfig"));
        when(store.listSkill()).thenReturn(List.of());
        when(store.listMonster()).thenReturn(List.of("monster_group"));
        when(store.listServerConfig()).thenReturn(List.of("role_name", "string"));

        mvc.perform(get("/config/index"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.count").value(2))
                .andExpect(jsonPath("$.drop.count").value(3))
                .andExpect(jsonPath("$.serverconfig.count").value(2));

        mvc.perform(get("/config/list/item").param("offset","1").param("limit","1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"gemstone\"]"));
    }

    @Test
    void reload_calls_store_and_invalidates_cache() throws Exception {
        mvc.perform(post("/config/internal/reload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.revision").value("rev-xyz"));

        verify(store, times(1)).reload();
        verify(testL1Cache, times(1)).invalidateAll();
    }

    @Test
    void not_found_returns_404() throws Exception {
        when(store.getFileByKey("config/gameworld/item/notexists")).thenReturn(Optional.empty());

        mvc.perform(get("/config/gameworld/item/notexists"))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_item_304_with_ifModifiedSince() throws Exception {
        long lm = Instant.now().getEpochSecond();
        var key = "config/gameworld/item/equipment";
        var data = file(key, "application/json", "{\"a\":1}", "\"etagX\"", lm, "rev-1");
        when(store.getFileByKey(key)).thenReturn(Optional.of(data));

        mvc.perform(get("/config/gameworld/item/equipment")
                        .header("If-Modified-Since", lm * 1000))
                .andExpect(status().isNotModified());
    }
}