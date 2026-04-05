package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.client.ConfigServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskDefinitionProvider Tests")
class TaskDefinitionProviderTest {

    @Mock
    private ConfigServiceClient configServiceClient;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private TaskDefinitionProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TaskDefinitionProvider(configServiceClient, new ObjectMapper(), stringRedisTemplate);
        ReflectionTestUtils.setField(provider, "taskConfigPath", "gameworld/logicconfig/task_cfg.json");
    }

    @Test
    @DisplayName("manualReload parses array schema and updates cache")
    void manualReload_parsesArraySchema() {
        String json = """
            [
              {
                "taskKey": "daily_login",
                "taskName": "Daily Login",
                "description": "Login once",
                "targetValue": 1,
                "goldReward": 100,
                "expReward": 50,
                "itemRewards": ""
              },
              {
                "taskKey": "kill_monster",
                "taskName": "Kill Monsters",
                "description": "Kill 10 mobs",
                "targetValue": 10,
                "goldReward": 200,
                "expReward": 100,
                "itemRewards": "item:101:5"
              }
            ]
            """;
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ETAG, "\"v1\"");
        given(configServiceClient.getFile(eq("gameworld/logicconfig/task_cfg.json"), isNull()))
            .willReturn(new ResponseEntity<>(json.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK));

        TaskDefinitionProvider.TaskConfigStatus status = provider.manualReload();
        Map<String, TaskDefinitionConfig> configs = provider.getTaskConfigs();

        assertThat(status.taskCount()).isEqualTo(2);
        assertThat(status.source()).isEqualTo("config-service");
        assertThat(status.etag()).isEqualTo("\"v1\"");
        assertThat(configs).containsKeys("daily_login", "kill_monster");
        assertThat(configs.get("kill_monster").targetValue()).isEqualTo(10);
    }

    @Test
    @DisplayName("manualReload supports alias keys in payload")
    void manualReload_supportsAliasKeys() {
        String json = """
            {
              "tasks": [
                {
                  "task_key": "arena_win",
                  "task_name": "Arena Winner",
                  "desc": "Win 3 arena matches",
                  "target": "3",
                  "gold": "90",
                  "exp": "30",
                  "item_rewards": "item:201:1"
                }
              ]
            }
            """;
        given(configServiceClient.getFile(anyString(), isNull()))
            .willReturn(ResponseEntity.ok(json.getBytes(StandardCharsets.UTF_8)));

        provider.manualReload();

        TaskDefinitionConfig cfg = provider.getTaskConfig("arena_win");
        assertThat(cfg).isNotNull();
        assertThat(cfg.targetValue()).isEqualTo(3);
        assertThat(cfg.goldReward()).isEqualTo(90);
        assertThat(cfg.expReward()).isEqualTo(30);
        assertThat(cfg.itemRewards()).isEqualTo("item:201:1");
    }

    @Test
    @DisplayName("scheduled refresh with 304 keeps existing cache")
    void refresh_notModified_keepsCache() {
        String json = "[{\"taskKey\":\"daily_login\",\"targetValue\":1}]";
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ETAG, "\"v1\"");
        given(configServiceClient.getFile(eq("gameworld/logicconfig/task_cfg.json"), isNull()))
            .willReturn(new ResponseEntity<>(json.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK));

        provider.manualReload();

        given(configServiceClient.getFile(eq("gameworld/logicconfig/task_cfg.json"), eq("\"v1\"")))
            .willReturn(new ResponseEntity<>(null, new HttpHeaders(), HttpStatus.NOT_MODIFIED));

        provider.refresh();
        TaskDefinitionProvider.TaskConfigStatus status = provider.status();

        assertThat(provider.getTaskConfigs()).hasSize(1);
        assertThat(provider.getTaskConfig("daily_login")).isNotNull();
        assertThat(status.source()).isEqualTo("cache");
    }

    @Test
    @DisplayName("manualReload parses production task_list schema")
    void manualReload_parsesTaskListSchema() {
        String json = """
            {
              "task_list": [
                {
                  "task_id": "0",
                  "param": "3",
                  "reward": [
                    {"item_id": "40004", "num": "5"},
                    {"item_id": "50001", "num": "2"}
                  ]
                }
              ]
            }
            """;
        given(configServiceClient.getFile(anyString(), isNull()))
            .willReturn(ResponseEntity.ok(json.getBytes(StandardCharsets.UTF_8)));

        provider.manualReload();

        TaskDefinitionConfig cfg = provider.getTaskConfig("0");
        assertThat(cfg).isNotNull();
        assertThat(cfg.targetValue()).isEqualTo(3);
        assertThat(cfg.itemRewards()).isEqualTo("item:40004:5,item:50001:2");
    }

    @Test
    @DisplayName("invalid JSON keeps fallback cache")
    void manualReload_invalidJson_keepsFallback() {
        int fallbackSize = provider.getTaskConfigs().size();
        given(configServiceClient.getFile(anyString(), isNull()))
            .willReturn(ResponseEntity.ok("{invalid-json".getBytes(StandardCharsets.UTF_8)));

        provider.manualReload();

        assertThat(provider.getTaskConfigs()).hasSize(fallbackSize);
        assertThat(provider.status().lastError()).isNotBlank();
    }
}

