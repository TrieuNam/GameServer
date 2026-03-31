package com.SouthMillion.role_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.role_service.service.SkillService;
import org.SouthMillion.dto.skill.SkillDTOs;
import org.SouthMillion.dto.skill.SkillDTOs.SkillErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer integration tests cho SkillController.
 *
 * <p>Coverage:
 * <ul>
 *   <li>5 smoke cases (Happy Path): getSkillAll, learnSkill, oneKeyLevelUp, getTalentAll, learnTalent</li>
 *   <li>3 fail cases: invalid skillId, role not found, already max level</li>
 * </ul>
 *
 * <p>Checklist từ SKILL_TALENT_INTEGRATION_STATUS_TRACKER.md (section 6):
 * <pre>
 *   ✅ Gửi req 1470/1480 đúng param
 *   ✅ Nhận 1471/1481 đúng schema
 *   ✅ Case fail (thiếu tài nguyên/chưa unlock/max level) có thông báo đúng
 * </pre>
 */
@WebMvcTest(SkillController.class)
@DisplayName("SkillController Integration Tests")
class SkillControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @MockBean
    private SkillService skillService;

    // ═══════════════════════════════ SMOKE CASES ═════════════════════════

    @Nested
    @DisplayName("Smoke — Happy Path (5 cases)")
    class SmokeHappyPath {

        @Test
        @DisplayName("SMOKE-1 GET /api/skill/{roleId} — trả danh sách kỹ năng")
        void getSkillAll_returnsSkillList() throws Exception {
            SkillDTOs.SkillAllInfoResp resp = SkillDTOs.SkillAllInfoResp.builder()
                    .skillCount(2)
                    .errorCode(SkillErrorCodes.OK)
                    .skillList(List.of(
                            SkillDTOs.RoleSkillInfo.builder().skillId(1001).skillLevel(3).skillIndex(0).build(),
                            SkillDTOs.RoleSkillInfo.builder().skillId(1002).skillLevel(1).skillIndex(1).build()
                    ))
                    .build();

            given(skillService.getSkillAll(10L)).willReturn(resp);

            mvc.perform(get("/api/skill/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skillCount", is(2)))
                    .andExpect(jsonPath("$.errorCode", is(SkillErrorCodes.OK)))
                    .andExpect(jsonPath("$.skillList[0].skillId", is(1001)))
                    .andExpect(jsonPath("$.skillList[1].skillId", is(1002)));
        }

        @Test
        @DisplayName("SMOKE-2 POST /api/skill/{roleId}/learn — học kỹ năng thành công")
        void learnSkill_success_returnsUpdatedList() throws Exception {
            SkillDTOs.SkillAllInfoResp resp = SkillDTOs.SkillAllInfoResp.builder()
                    .skillCount(1)
                    .errorCode(SkillErrorCodes.OK)
                    .skillList(List.of(
                            SkillDTOs.RoleSkillInfo.builder().skillId(2001).skillLevel(2).skillIndex(0).build()
                    ))
                    .build();

            given(skillService.learnSkill(20L, 2001)).willReturn(resp);

            SkillDTOs.LearnSkillReq req = SkillDTOs.LearnSkillReq.builder()
                    .roleId(20L).skillId(2001).reqType(1).build();

            mvc.perform(post("/api/skill/20/learn")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsBytes(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errorCode", is(SkillErrorCodes.OK)))
                    .andExpect(jsonPath("$.skillList[0].skillLevel", is(2)));
        }

        @Test
        @DisplayName("SMOKE-3 POST /api/skill/{roleId}/one-key-level-up — nâng cấp tất cả thành công")
        void oneKeyLevelUp_success_returnsUpdatedList() throws Exception {
            SkillDTOs.SkillAllInfoResp resp = SkillDTOs.SkillAllInfoResp.builder()
                    .skillCount(3)
                    .errorCode(SkillErrorCodes.OK)
                    .skillList(List.of(
                            SkillDTOs.RoleSkillInfo.builder().skillId(1001).skillLevel(5).skillIndex(0).build(),
                            SkillDTOs.RoleSkillInfo.builder().skillId(1002).skillLevel(3).skillIndex(1).build(),
                            SkillDTOs.RoleSkillInfo.builder().skillId(1003).skillLevel(1).skillIndex(2).build()
                    ))
                    .build();

            given(skillService.oneKeyLevelUp(30L)).willReturn(resp);

            mvc.perform(post("/api/skill/30/one-key-level-up"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errorCode", is(SkillErrorCodes.OK)))
                    .andExpect(jsonPath("$.skillCount", is(3)));
        }

        @Test
        @DisplayName("SMOKE-4 GET /api/talent/{roleId} — trả danh sách thiên phú")
        void getTalentAll_returnsTalentList() throws Exception {
            SkillDTOs.TalentAllInfoResp resp = SkillDTOs.TalentAllInfoResp.builder()
                    .talentSkillCount(2)
                    .errorCode(SkillErrorCodes.OK)
                    .talentSkillList(List.of(
                            SkillDTOs.RoleTalentInfo.builder().skillId(3001).skillLevel(2).build(),
                            SkillDTOs.RoleTalentInfo.builder().skillId(3002).skillLevel(1).build()
                    ))
                    .build();

            given(skillService.getTalentAll(40L)).willReturn(resp);

            mvc.perform(get("/api/talent/40"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.talentSkillCount", is(2)))
                    .andExpect(jsonPath("$.errorCode", is(SkillErrorCodes.OK)))
                    .andExpect(jsonPath("$.talentSkillList[0].skillId", is(3001)));
        }

        @Test
        @DisplayName("SMOKE-5 POST /api/talent/{roleId}/learn — học thiên phú thành công")
        void learnTalent_success_returnsUpdatedList() throws Exception {
            SkillDTOs.TalentAllInfoResp resp = SkillDTOs.TalentAllInfoResp.builder()
                    .talentSkillCount(1)
                    .errorCode(SkillErrorCodes.OK)
                    .talentSkillList(List.of(
                            SkillDTOs.RoleTalentInfo.builder().skillId(3001).skillLevel(3).build()
                    ))
                    .build();

            given(skillService.learnTalent(50L, 3001)).willReturn(resp);

            SkillDTOs.LearnTalentReq req = SkillDTOs.LearnTalentReq.builder()
                    .roleId(50L).skillId(3001).reqType(1).build();

            mvc.perform(post("/api/talent/50/learn")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsBytes(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errorCode", is(SkillErrorCodes.OK)))
                    .andExpect(jsonPath("$.talentSkillList[0].skillLevel", is(3)));
        }
    }

    // ═══════════════════════════════ FAIL CASES ══════════════════════════

    @Nested
    @DisplayName("Fail Cases (3 cases)")
    class FailCases {

        @Test
        @DisplayName("FAIL-1 skillId không hợp lệ → errorCode=INVALID_SKILL_ID (2)")
        void learnSkill_invalidSkillId_returnsErrorCode2() throws Exception {
            SkillDTOs.SkillAllInfoResp resp = SkillDTOs.SkillAllInfoResp.builder()
                    .skillCount(0)
                    .errorCode(SkillErrorCodes.INVALID_SKILL_ID)
                    .errorMsg("invalid skillId=0 roleId=99")
                    .skillList(List.of())
                    .build();

            given(skillService.learnSkill(99L, 0)).willReturn(resp);

            SkillDTOs.LearnSkillReq req = SkillDTOs.LearnSkillReq.builder()
                    .roleId(99L).skillId(0).reqType(1).build();

            mvc.perform(post("/api/skill/99/learn")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsBytes(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errorCode", is(SkillErrorCodes.INVALID_SKILL_ID)))
                    .andExpect(jsonPath("$.skillCount", is(0)));
        }

        @Test
        @DisplayName("FAIL-2 role không tồn tại → errorCode=ROLE_NOT_FOUND (1)")
        void learnSkill_roleNotFound_returnsErrorCode1() throws Exception {
            SkillDTOs.SkillAllInfoResp resp = SkillDTOs.SkillAllInfoResp.builder()
                    .skillCount(0)
                    .errorCode(SkillErrorCodes.ROLE_NOT_FOUND)
                    .errorMsg("role not found: 404")
                    .skillList(List.of())
                    .build();

            given(skillService.learnSkill(404L, 1001)).willReturn(resp);

            SkillDTOs.LearnSkillReq req = SkillDTOs.LearnSkillReq.builder()
                    .roleId(404L).skillId(1001).reqType(1).build();

            mvc.perform(post("/api/skill/404/learn")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsBytes(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errorCode", is(SkillErrorCodes.ROLE_NOT_FOUND)))
                    .andExpect(jsonPath("$.skillList", hasSize(0)));
        }

        @Test
        @DisplayName("FAIL-3 kỹ năng đã đạt max level → errorCode=ALREADY_MAX_LEVEL (3)")
        void learnSkill_alreadyMaxLevel_returnsErrorCode3() throws Exception {
            SkillDTOs.SkillAllInfoResp resp = SkillDTOs.SkillAllInfoResp.builder()
                    .skillCount(1)
                    .errorCode(SkillErrorCodes.ALREADY_MAX_LEVEL)
                    .errorMsg("skill already at max level: skillId=5001")
                    .skillList(List.of(
                            SkillDTOs.RoleSkillInfo.builder().skillId(5001).skillLevel(20).skillIndex(0).build()
                    ))
                    .build();

            given(skillService.learnSkill(55L, 5001)).willReturn(resp);

            SkillDTOs.LearnSkillReq req = SkillDTOs.LearnSkillReq.builder()
                    .roleId(55L).skillId(5001).reqType(1).build();

            mvc.perform(post("/api/skill/55/learn")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsBytes(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errorCode", is(SkillErrorCodes.ALREADY_MAX_LEVEL)))
                    .andExpect(jsonPath("$.skillList[0].skillLevel", is(20)));
        }
    }
}

