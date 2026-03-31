package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.config.SkillConfigCache;
import com.SouthMillion.role_service.repository.RoleRepository;
import com.SouthMillion.role_service.service.client.WalletFeign;
import com.SouthMillion.role_service.entity.RoleSkill;
import com.SouthMillion.role_service.entity.RoleTalent;
import com.SouthMillion.role_service.repository.RoleSkillRepository;
import com.SouthMillion.role_service.repository.RoleTalentRepository;
import org.SouthMillion.dto.skill.SkillDTOs;
import org.SouthMillion.dto.wallet.ResultDTO;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillService Tests")
class SkillServiceTest {

	@Mock
	private RoleRepository roleRepo;

	@Mock
	private RoleSkillRepository skillRepo;

	@Mock
	private RoleTalentRepository talentRepo;

	@Mock
	private SkillConfigCache skillConfigCache;

	@Mock
	private WalletFeign walletFeign;

	@InjectMocks
	private SkillService skillService;

	private static final int DEFAULT_MAX_LEVEL = 20;

	@BeforeEach
	void setUpRoleRepository() {
		lenient().when(roleRepo.existsById(anyLong())).thenReturn(true);
		// SkillConfigCache: fail-open by default (return true for all IDs)
		lenient().when(skillConfigCache.isValidSkillId(any())).thenReturn(true);
		lenient().when(skillConfigCache.isValidTalentId(any())).thenReturn(true);
		lenient().when(skillConfigCache.getSkillMaxLevel(any())).thenReturn(DEFAULT_MAX_LEVEL);
		lenient().when(skillConfigCache.getTalentMaxLevel(any())).thenReturn(DEFAULT_MAX_LEVEL);
		lenient().when(skillConfigCache.getDefaultSkillMaxLevel()).thenReturn(DEFAULT_MAX_LEVEL);
		lenient().when(walletFeign.batchCost(any())).thenReturn(ResultDTO.ok(WalletDTOs.MutateResp.builder()
				.ok(true)
				.build()));
		ReflectionTestUtils.setField(skillService, "economyEnabled", true);
		ReflectionTestUtils.setField(skillService, "economyCurrencyItemId", 1L);
		ReflectionTestUtils.setField(skillService, "skillBaseCost", 100L);
		ReflectionTestUtils.setField(skillService, "skillStepCost", 20L);
		ReflectionTestUtils.setField(skillService, "talentBaseCost", 120L);
		ReflectionTestUtils.setField(skillService, "talentStepCost", 25L);
		ReflectionTestUtils.setField(skillService, "oneKeyDiscountPercent", 0);
	}

	@Nested
	@DisplayName("getSkillAll()")
	class GetSkillAll {

		@Test
		@DisplayName("TC-SKL-001 [P] trả danh sách kỹ năng theo thứ tự skill_index tăng dần")
		void getSkillAll_returnsOrderedSkills() {
			RoleSkill slot2 = new RoleSkill();
			slot2.setRoleId(10L);
			slot2.setSkillId(2002);
			slot2.setSkillLevel(3);
			slot2.setSkillIndex(2);

			RoleSkill slot5 = new RoleSkill();
			slot5.setRoleId(10L);
			slot5.setSkillId(2005);
			slot5.setSkillLevel(1);
			slot5.setSkillIndex(5);

			given(skillRepo.findByRoleIdOrderBySkillIndexAscSkillIdAsc(10L)).willReturn(List.of(slot2, slot5));

			SkillDTOs.SkillAllInfoResp resp = skillService.getSkillAll(10L);

			assertThat(resp.getSkillCount()).isEqualTo(2);
			assertThat(resp.getSkillList())
					.extracting(SkillDTOs.RoleSkillInfo::getSkillIndex)
					.containsExactly(2, 5);
		}
	}

	@Nested
	@DisplayName("learnSkill()")
	class LearnSkill {

		@Test
		@DisplayName("TC-SKL-010 [P] học kỹ năng mới sẽ tạo skill_index tiếp theo")
		void learnSkill_newSkill_assignsNextIndex() {
			given(skillRepo.findByRoleIdAndSkillId(99L, 3001)).willReturn(Optional.empty());
			given(skillRepo.save(any(RoleSkill.class))).willAnswer(inv -> inv.getArgument(0));
			given(skillRepo.findByRoleIdOrderBySkillIndexAscSkillIdAsc(99L)).willAnswer(inv -> {
				RoleSkill saved = new RoleSkill();
				saved.setRoleId(99L);
				saved.setSkillId(3001);
				saved.setSkillLevel(1);
				saved.setSkillIndex(2);
				return List.of(saved);
			});

			SkillDTOs.SkillAllInfoResp resp = skillService.learnSkill(99L, 3001);

			assertThat(resp.getSkillList()).singleElement().satisfies(skill -> {
				assertThat(skill.getSkillId()).isEqualTo(3001);
				assertThat(skill.getSkillLevel()).isEqualTo(1);
				assertThat(skill.getSkillIndex()).isEqualTo(2);
			});
			then(walletFeign).should().batchCost(any());
		}

		@Test
		@DisplayName("TC-SKL-011 [N] skillId không hợp lệ thì không mutate dữ liệu")
		void learnSkill_invalidSkillId_returnsCurrentInfo() {
			SkillDTOs.SkillAllInfoResp resp = skillService.learnSkill(99L, 0);

			assertThat(resp.getSkillCount()).isEqualTo(0);
			then(skillRepo).should(never()).findByRoleIdAndSkillId(anyLong(), any());
			then(skillRepo).should(never()).save(any(RoleSkill.class));
		}

		@Test
		@DisplayName("TC-SKL-012 [N] role không tồn tại thì bỏ qua thao tác học skill")
		void learnSkill_missingRole_skipsMutation() {
			given(roleRepo.existsById(404L)).willReturn(false);

			SkillDTOs.SkillAllInfoResp resp = skillService.learnSkill(404L, 3001);

			assertThat(resp.getSkillCount()).isEqualTo(0);
			then(skillRepo).should(never()).save(any(RoleSkill.class));
			then(walletFeign).should(never()).batchCost(any());
		}

		@Test
		@DisplayName("TC-SKL-013 [N] wallet trừ phí lỗi thì không được nâng cấp skill")
		void learnSkill_walletFail_noMutation() {
			RoleSkill existing = new RoleSkill();
			existing.setRoleId(99L);
			existing.setSkillId(3001);
			existing.setSkillLevel(2);
			existing.setSkillIndex(0);

			given(skillRepo.findByRoleIdAndSkillId(99L, 3001)).willReturn(Optional.of(existing));
			given(skillRepo.findByRoleIdOrderBySkillIndexAscSkillIdAsc(99L)).willReturn(List.of(existing));
			given(walletFeign.batchCost(any())).willReturn(ResultDTO.err(1001, "INSUFFICIENT_FUNDS"));

			SkillDTOs.SkillAllInfoResp resp = skillService.learnSkill(99L, 3001);

			assertThat(resp.getErrorCode()).isEqualTo(SkillDTOs.SkillErrorCodes.INSUFFICIENT_RESOURCE);
			assertThat(existing.getSkillLevel()).isEqualTo(2);
			then(skillRepo).should(never()).save(any(RoleSkill.class));
		}

		@Test
		@DisplayName("TC-SKL-014 [P] wallet thành công thì nâng cấp skill")
		void learnSkill_walletOk_mutates() {
			RoleSkill existing = new RoleSkill();
			existing.setRoleId(99L);
			existing.setSkillId(3002);
			existing.setSkillLevel(1);
			existing.setSkillIndex(0);

			given(skillRepo.findByRoleIdAndSkillId(99L, 3002)).willReturn(Optional.of(existing));
			given(skillRepo.save(any(RoleSkill.class))).willAnswer(inv -> inv.getArgument(0));
			given(skillRepo.findByRoleIdOrderBySkillIndexAscSkillIdAsc(99L)).willReturn(List.of(existing));

			SkillDTOs.SkillAllInfoResp resp = skillService.learnSkill(99L, 3002);

			assertThat(resp.getErrorCode()).isEqualTo(SkillDTOs.SkillErrorCodes.OK);
			assertThat(existing.getSkillLevel()).isEqualTo(2);
			then(skillRepo).should().save(existing);
		}
	}

	@Nested
	@DisplayName("oneKeyLevelUp()")
	class OneKeyLevelUp {

		@Test
		@DisplayName("TC-SKL-020 [P] chỉ tăng các kỹ năng chưa đạt max level")
		void oneKeyLevelUp_capsAtConfiguredMaxLevel() {
			RoleSkill capped = new RoleSkill();
			capped.setRoleId(50L);
			capped.setSkillId(4001);
			capped.setSkillLevel(20);
			capped.setSkillIndex(0);
			given(skillConfigCache.getSkillMaxLevel(4001)).willReturn(20);
			given(skillRepo.findByRoleIdOrderBySkillIndexAscSkillIdAsc(50L)).willReturn(List.of(capped));


			SkillDTOs.SkillAllInfoResp resp = skillService.oneKeyLevelUp(50L);

			then(skillRepo).should(never()).save(any(RoleSkill.class));
			assertThat(resp.getSkillList()).singleElement().satisfies(skill -> {
				assertThat(skill.getSkillId()).isEqualTo(4001);
				assertThat(skill.getSkillLevel()).isEqualTo(20);
			});
		}

		@Test
		@DisplayName("TC-SKL-021 [N] role không tồn tại thì one-key không được mutate")
		void oneKeyLevelUp_missingRole_skipsMutation() {
			given(roleRepo.existsById(404L)).willReturn(false);

			SkillDTOs.SkillAllInfoResp resp = skillService.oneKeyLevelUp(404L);

			assertThat(resp.getSkillCount()).isEqualTo(0);
			then(skillRepo).should(never()).save(any(RoleSkill.class));
		}

		@Test
		@DisplayName("TC-SKL-022 [P] one-key chỉ nâng cấp skill chưa max trong danh sách mixed")
		void oneKeyLevelUp_mixedList_upgradesOnlyNonMax() {
			RoleSkill maxed = new RoleSkill();
			maxed.setRoleId(60L);
			maxed.setSkillId(5001);
			maxed.setSkillLevel(5);
			maxed.setSkillIndex(0);

			RoleSkill upgradable = new RoleSkill();
			upgradable.setRoleId(60L);
			upgradable.setSkillId(5002);
			upgradable.setSkillLevel(2);
			upgradable.setSkillIndex(1);

			given(skillConfigCache.getSkillMaxLevel(5001)).willReturn(5);
			given(skillConfigCache.getSkillMaxLevel(5002)).willReturn(4);
			given(skillRepo.findByRoleIdOrderBySkillIndexAscSkillIdAsc(60L)).willReturn(
					List.of(maxed, upgradable),
					List.of(maxed, upgradable)
			);
			given(skillRepo.save(any(RoleSkill.class))).willAnswer(inv -> inv.getArgument(0));

			SkillDTOs.SkillAllInfoResp resp = skillService.oneKeyLevelUp(60L);

			assertThat(resp.getErrorCode()).isEqualTo(SkillDTOs.SkillErrorCodes.OK);
			assertThat(maxed.getSkillLevel()).isEqualTo(5);
			assertThat(upgradable.getSkillLevel()).isEqualTo(3);
			then(skillRepo).should().save(upgradable);
			then(skillRepo).should(never()).save(maxed);
		}
	}

	@Nested
	@DisplayName("getTalentAll()")
	class GetTalentAll {

		@Test
		@DisplayName("TC-SKL-030 [P] trả talent theo skill_id tăng dần")
		void getTalentAll_returnsOrderedTalents() {
			RoleTalent talentA = new RoleTalent();
			talentA.setRoleId(7L);
			talentA.setSkillId(1001);
			talentA.setSkillLevel(2);

			RoleTalent talentB = new RoleTalent();
			talentB.setRoleId(7L);
			talentB.setSkillId(1003);
			talentB.setSkillLevel(1);

			given(talentRepo.findByRoleIdOrderBySkillIdAsc(7L)).willReturn(List.of(talentA, talentB));

			SkillDTOs.TalentAllInfoResp resp = skillService.getTalentAll(7L);

			assertThat(resp.getTalentSkillCount()).isEqualTo(2);
			assertThat(resp.getTalentSkillList())
					.extracting(SkillDTOs.RoleTalentInfo::getSkillId)
					.containsExactly(1001, 1003);
		}
	}

	@Nested
	@DisplayName("learnTalent()")
	class LearnTalent {

		@Test
		@DisplayName("TC-SKL-040 [N] role không tồn tại thì bỏ qua thao tác học talent")
		void learnTalent_missingRole_skipsMutation() {
			given(roleRepo.existsById(404L)).willReturn(false);

			SkillDTOs.TalentAllInfoResp resp = skillService.learnTalent(404L, 1001);

			assertThat(resp.getTalentSkillCount()).isEqualTo(0);
			then(talentRepo).should(never()).save(any(RoleTalent.class));
		}

		@Test
		@DisplayName("TC-SKL-041 [N] skillId talent không hợp lệ thì không mutate dữ liệu")
		void learnTalent_invalidSkillId_returnsCurrentInfo() {
			SkillDTOs.TalentAllInfoResp resp = skillService.learnTalent(7L, 0);

			assertThat(resp.getTalentSkillCount()).isEqualTo(0);
			then(talentRepo).should(never()).findByRoleIdAndSkillId(anyLong(), any());
			then(talentRepo).should(never()).save(any(RoleTalent.class));
		}

		@Test
		@DisplayName("TC-SKL-042 [N] wallet lỗi thì talent không được nâng cấp")
		void learnTalent_walletFail_noMutation() {
			RoleTalent existing = new RoleTalent();
			existing.setRoleId(7L);
			existing.setSkillId(1001);
			existing.setSkillLevel(2);

			given(talentRepo.findByRoleIdAndSkillId(7L, 1001)).willReturn(Optional.of(existing));
			given(talentRepo.findByRoleIdOrderBySkillIdAsc(7L)).willReturn(List.of(existing));
			given(walletFeign.batchCost(any())).willReturn(ResultDTO.err(1001, "INSUFFICIENT"));

			SkillDTOs.TalentAllInfoResp resp = skillService.learnTalent(7L, 1001);

			assertThat(resp.getErrorCode()).isEqualTo(SkillDTOs.SkillErrorCodes.INSUFFICIENT_RESOURCE);
			assertThat(existing.getSkillLevel()).isEqualTo(2);
			then(talentRepo).should(never()).save(any(RoleTalent.class));
		}
	}
}

