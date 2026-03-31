package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.config.RoleConfigCache;
import com.SouthMillion.role_service.entity.Role;
import com.SouthMillion.role_service.repository.RoleRepository;
import com.SouthMillion.role_service.service.client.ConfigFeign;
import org.SouthMillion.dto.role.RoleDTOs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Tests")
class RoleServiceTest {

    @Mock
    private RoleRepository repo;

    @Mock
    private ConfigFeign configFeign;

    @Mock
    private RoleConfigCache cfg;

    @Mock
    private NewbieGiftService newbieGiftService;

    @InjectMocks
    private RoleService roleService;

    /** Default LevelTable: level 1 can 150 exp (100 + 50*1), level 2 can 200 exp */
    private RoleConfigCache.LevelTable defaultLevelTable;

    @BeforeEach
    void setUp() {
        Map<Integer, Long> expMap = new HashMap<>();
        expMap.put(1, 100L);   // can 100 exp de len level 2
        expMap.put(2, 200L);   // can 200 exp de len level 3
        expMap.put(3, 300L);   // can 300 exp de len level 4
        defaultLevelTable = new RoleConfigCache.LevelTable(expMap, 1, 60);

        RoleConfigCache.BaseAttrCfg baseAttr = new RoleConfigCache.BaseAttrCfg(10, 500, 100, 50);
        given(cfg.levelTable()).willReturn(defaultLevelTable);
        given(cfg.baseAttr()).willReturn(baseAttr);
        given(cfg.randomName()).willReturn("Player_1234");
    }

    // =========================================================
    // create
    // =========================================================
    @Nested
    @DisplayName("create()")
    class CreateRole {

        @Test
        @DisplayName("TC-ROL-001 [P] Tao nhan vat thanh cong")
        void createRole_success() {
            given(repo.findByUserIdAndName("user-001", "Hero")).willReturn(Optional.empty());
            given(repo.saveAndFlush(any(Role.class))).willAnswer(inv -> {
                Role r = inv.getArgument(0);
                r.setRoleId(1L);
                return r;
            });
            given(cfg.other()).willReturn(new RoleConfigCache.OtherCfg(0L, 0));

            RoleDTOs.CreateRoleReq req = RoleDTOs.CreateRoleReq.builder()
                    .userId("user-001")
                    .name("Hero")
                    .build();

            RoleDTOs.RoleResp result = roleService.create(req);

            assertThat(result.getUserId()).isEqualTo("user-001");
            assertThat(result.getName()).isEqualTo("Hero");
            assertThat(result.getLevel()).isEqualTo(1);
            assertThat(result.getCurExp()).isEqualTo(0L);
        }

        @Test
        @DisplayName("TC-ROL-002 [N] Ten nhan vat da ton tai — tu dong them suffix")
        void createRole_duplicateName_autoSuffix() {
            // findByUserIdAndName tra ve ket qua dau tien (trung ten), lan 2 rong
            Role existing = new Role();
            existing.setName("Hero");
            given(repo.findByUserIdAndName(eq("user-001"), eq("Hero"))).willReturn(Optional.of(existing));
            given(repo.findByUserIdAndName(eq("user-001"), argThat(n -> n != null && n.startsWith("Hero_"))))
                    .willReturn(Optional.empty());
            given(repo.saveAndFlush(any(Role.class))).willAnswer(inv -> inv.getArgument(0));
            given(cfg.other()).willReturn(new RoleConfigCache.OtherCfg(0L, 0));

            RoleDTOs.CreateRoleReq req = RoleDTOs.CreateRoleReq.builder()
                    .userId("user-001")
                    .name("Hero")
                    .build();

            // Khong nen throw, phai tu dong doi ten
            assertThatCode(() -> roleService.create(req)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("TC-ROL-003 [P] Base attr duoc set tu RoleConfigCache")
        void createRole_baseAttrSet() {
            given(repo.findByUserIdAndName(anyString(), anyString())).willReturn(Optional.empty());
            given(repo.saveAndFlush(any(Role.class))).willAnswer(inv -> inv.getArgument(0));
            given(cfg.other()).willReturn(new RoleConfigCache.OtherCfg(0L, 0));

            RoleDTOs.CreateRoleReq req = RoleDTOs.CreateRoleReq.builder()
                    .userId("user-001")
                    .name("Warrior")
                    .build();

            RoleDTOs.RoleResp result = roleService.create(req);

            // hp tu BaseAttrCfg(10, 500, 100, 50) -> hp=500
            assertThat(result.getHp()).isGreaterThan(0L);
        }
    }

    // =========================================================
    // addExp
    // =========================================================
    @Nested
    @DisplayName("addExp()")
    class AddExp {

        private Role buildRole(Long roleId, int level, long exp) {
            Role r = new Role();
            r.setRoleId(roleId);
            r.setLevel(level);
            r.setExp(exp);
            r.setHp(500L);
            r.setAttackValue(100L);
            r.setDefenseValue(50L);
            r.setSpeed(10);
            return r;
        }

        @Test
        @DisplayName("TC-ROL-010 [P] Them exp, chua du level up")
        void addExp_noLevelUp() {
            Role role = buildRole(100L, 1, 0L);
            given(repo.findById(100L)).willReturn(Optional.of(role));
            given(repo.saveAndFlush(any(Role.class))).willAnswer(inv -> inv.getArgument(0));

            RoleDTOs.RoleResp resp = roleService.addExp(100L, 50L);

            assertThat(resp.getLevel()).isEqualTo(1);
            assertThat(resp.getCurExp()).isEqualTo(50L);
        }

        @Test
        @DisplayName("TC-ROL-011 [P] Them exp vua du level up 1 lan")
        void addExp_levelUpOnce() {
            Role role = buildRole(100L, 1, 0L);
            given(repo.findById(100L)).willReturn(Optional.of(role));
            given(repo.saveAndFlush(any(Role.class))).willAnswer(inv -> inv.getArgument(0));

            // 100 exp du len level 2 (expMap[1]=100)
            RoleDTOs.RoleResp resp = roleService.addExp(100L, 100L);

            assertThat(resp.getLevel()).isEqualTo(2);
            assertThat(resp.getCurExp()).isEqualTo(0L);
        }

        @Test
        @DisplayName("TC-ROL-012 [P] Them exp du level up nhieu lan")
        void addExp_multipleLevelUps() {
            Role role = buildRole(100L, 1, 0L);
            given(repo.findById(100L)).willReturn(Optional.of(role));
            given(repo.saveAndFlush(any(Role.class))).willAnswer(inv -> inv.getArgument(0));

            // 100 + 200 + 300 = 600 du len level 4
            RoleDTOs.RoleResp resp = roleService.addExp(100L, 600L);

            assertThat(resp.getLevel()).isGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("TC-ROL-013 [P] addExp voi 0 khong thay doi gi")
        void addExp_zero_noChange() {
            Role role = buildRole(100L, 5, 80L);
            given(repo.findById(100L)).willReturn(Optional.of(role));

            RoleDTOs.RoleResp resp = roleService.addExp(100L, 0L);

            assertThat(resp.getLevel()).isEqualTo(5);
            assertThat(resp.getCurExp()).isEqualTo(80L);
            // saveAndFlush khong duoc goi khi add <= 0
            then(repo).should(never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("TC-ROL-015 [N] RoleId khong ton tai")
        void addExp_roleNotFound_throws() {
            given(repo.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.addExp(999L, 100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role not found");
        }
    }

    // =========================================================
    // getById
    // =========================================================
    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("TC-ROL-020 [P] getById tra ve role")
        void getById_found() {
            Role role = new Role();
            role.setRoleId(1L);
            role.setName("Hero");
            role.setLevel(1);
            role.setExp(0L);
            given(repo.findById(1L)).willReturn(Optional.of(role));

            Optional<RoleDTOs.RoleResp> result = roleService.getById(1L);

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Hero");
        }

        @Test
        @DisplayName("TC-ROL-021 [P] getById khong tim thay")
        void getById_notFound() {
            given(repo.findById(999L)).willReturn(Optional.empty());

            Optional<RoleDTOs.RoleResp> result = roleService.getById(999L);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // rename
    // =========================================================
    @Nested
    @DisplayName("rename()")
    class Rename {

        @Test
        @DisplayName("TC-ROL-030 [P] Doi ten thanh cong")
        void rename_success() {
            Role role = new Role();
            role.setRoleId(1L);
            role.setName("OldName");
            role.setUserId("user-001");
            given(repo.findById(1L)).willReturn(Optional.of(role));
            given(repo.findByUserIdAndName("user-001", "NewName")).willReturn(Optional.empty());
            given(repo.saveAndFlush(any(Role.class))).willAnswer(inv -> inv.getArgument(0));

            RoleDTOs.RoleResp result = roleService.rename(1L, "NewName");

            assertThat(result.getName()).isEqualTo("NewName");
        }

        @Test
        @DisplayName("TC-ROL-031 [N] Ten moi trong nem exception")
        void rename_emptyName_throws() {
            assertThatThrownBy(() -> roleService.rename(1L, ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("TC-ROL-032 [N] Role khong ton tai nem exception")
        void rename_roleNotFound_throws() {
            given(repo.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> roleService.rename(999L, "NewName"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role not found");
        }
    }
}
