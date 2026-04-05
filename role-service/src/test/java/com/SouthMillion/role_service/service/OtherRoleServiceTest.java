package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.entity.Role;
import com.SouthMillion.role_service.repository.RoleRepository;
import com.SouthMillion.role_service.service.client.EquipFeign;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.SouthMillion.dto.role.other.OtherRoleDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtherRoleService Tests")
class OtherRoleServiceTest {

    @Mock
    private RoleRepository roleRepo;

    @Mock
    private EquipFeign equipFeign;

    @InjectMocks
    private OtherRoleService service;

    @Test
    @DisplayName("getOtherRole() aggregates equipped secondary attributes into roleAttrList")
    void getOtherRole_aggregatesEquippedSecondaryAttrs() {
        Role role = new Role();
        role.setRoleId(11L);
        role.setUserId("u-1");
        role.setName("tester");
        role.setLevel(9);
        role.setExp(123L);
        role.setHp(1000L);
        role.setAttackValue(80L);
        role.setDefenseValue(40L);
        role.setSpeed(12);
        role.setCreatedAt(Instant.now());
        role.setUpdatedAt(Instant.now());

        EquipDTOs.EquipItem first = EquipDTOs.EquipItem.builder()
                .equipType(0)
                .itemId(1004)
                .attrType1(11)
                .attrValue1(200)
                .attrType2(25)
                .attrValue2(1)
                .build();
        EquipDTOs.EquipItem second = EquipDTOs.EquipItem.builder()
                .equipType(1)
                .itemId(1005)
                .attrType1(2)
                .attrValue1(30)
                .build();

        given(roleRepo.findById(11L)).willReturn(Optional.of(role));
        given(equipFeign.list(11L)).willReturn(new EquipDTOs.ListResp(List.of(first, second)));

        OtherRoleDTOs.OtherRoleInfo info = service.getOtherRole("u-1", "11");

        assertThat(info.roleAttrList())
                .extracting(OtherRoleDTOs.OtherRoleAttrPair::attrType, OtherRoleDTOs.OtherRoleAttrPair::attrValue)
                .contains(
                        org.assertj.core.groups.Tuple.tuple(1, 1000L),
                        org.assertj.core.groups.Tuple.tuple(2, 110L),
                        org.assertj.core.groups.Tuple.tuple(3, 40L),
                        org.assertj.core.groups.Tuple.tuple(4, 12L),
                        org.assertj.core.groups.Tuple.tuple(11, 200L),
                        org.assertj.core.groups.Tuple.tuple(25, 1L)
                );
        assertThat(info.capability()).isGreaterThan(0L);
    }
}
