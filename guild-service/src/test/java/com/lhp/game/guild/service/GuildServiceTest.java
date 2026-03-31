package com.lhp.game.guild.service;

import com.SouthMillion.guild_service.dto.GuildDTO;
import com.SouthMillion.guild_service.entity.Guild;
import com.SouthMillion.guild_service.entity.GuildApplication;
import com.SouthMillion.guild_service.entity.GuildMember;
import com.SouthMillion.guild_service.repository.GuildApplicationRepository;
import com.SouthMillion.guild_service.repository.GuildMemberRepository;
import com.SouthMillion.guild_service.repository.GuildRepository;
import com.SouthMillion.guild_service.repository.GuildWarehouseRepository;
import com.SouthMillion.guild_service.service.GuildService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuildService Tests")
class GuildServiceTest {

    @Mock
    private GuildRepository guildRepository;

    @Mock
    private GuildMemberRepository memberRepository;

    @Mock
    private GuildApplicationRepository applicationRepository;

    @Mock
    private GuildWarehouseRepository warehouseRepository;

    @InjectMocks
    private GuildService guildService;

    private static final Long GUILD_ID = 1L;
    private static final String LEADER_ID = "1001";
    private static final String OFFICER_ID = "1002";
    private static final String MEMBER_ID = "1003";

    // =========================================================
    // createGuild
    // =========================================================
    @Nested
    @DisplayName("createGuild()")
    class CreateGuild {

        private GuildDTO.CreateRequest createReq(String name, String leaderId) {
            return GuildDTO.CreateRequest.builder()
                    .name(name)
                    .leaderId(leaderId)
                    .notice("Welcome!")
                    .build();
        }

        @Test
        @DisplayName("TC-GLD-001 [P] Tao bang hoi moi thanh cong")
        void createGuild_success() {
            given(guildRepository.existsByNameIgnoreCase("TestGuild")).willReturn(false);
            given(memberRepository.existsByRoleId(anyLong())).willReturn(false);

            Guild saved = new Guild();
            saved.setId(GUILD_ID);
            saved.setName("TestGuild");
            saved.setLeaderId(Long.parseLong(LEADER_ID));
            saved.setLevel(1);
            saved.setExp(0L);
            given(guildRepository.save(any(Guild.class))).willReturn(saved);
            given(memberRepository.save(any(GuildMember.class))).willAnswer(inv -> inv.getArgument(0));
            given(guildRepository.getGuildRank(anyInt(), anyLong())).willReturn(1L);

            GuildDTO.Response<GuildDTO.InfoResponse> resp = guildService.createGuild(createReq("TestGuild", LEADER_ID));

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData()).isNotNull();
            then(guildRepository).should().save(any(Guild.class));
            then(memberRepository).should().save(argThat(m -> m.getRank() == 3)); // rank 3 = leader
        }

        @Test
        @DisplayName("TC-GLD-002 [N] Ten bang da ton tai – tra ve error(-1)")
        void createGuild_nameTaken_returnsError() {
            given(guildRepository.existsByNameIgnoreCase("ExistingGuild")).willReturn(true);

            GuildDTO.Response<GuildDTO.InfoResponse> resp = guildService.createGuild(createReq("ExistingGuild", LEADER_ID));

            assertThat(resp.getCode()).isEqualTo(-1);
            then(guildRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("TC-GLD-003 [N] Nguoi choi da trong bang hoi – tra ve error(-2)")
        void createGuild_alreadyInGuild_returnsError() {
            given(guildRepository.existsByNameIgnoreCase("NewGuild")).willReturn(false);
            given(memberRepository.existsByRoleId(anyLong())).willReturn(true);

            GuildDTO.Response<GuildDTO.InfoResponse> resp = guildService.createGuild(createReq("NewGuild", LEADER_ID));

            assertThat(resp.getCode()).isEqualTo(-2);
        }
    }

    // =========================================================
    // getGuildInfo
    // =========================================================
    @Nested
    @DisplayName("getGuildInfo()")
    class GetGuildInfo {

        @Test
        @DisplayName("TC-GLD-005 [P] Lay thong tin bang hoi hop le")
        void getGuildInfo_success() {
            Guild guild = new Guild();
            guild.setId(GUILD_ID);
            guild.setName("TestGuild");
            guild.setLeaderId(Long.parseLong(LEADER_ID));
            guild.setLevel(1);
            guild.setExp(0L);
            guild.setActive(true);

            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));
            given(memberRepository.findByGuildIdAndRank(GUILD_ID, 3)).willReturn(Optional.empty());
            given(guildRepository.getGuildRank(anyInt(), anyLong())).willReturn(1L);

            GuildDTO.Response<GuildDTO.InfoResponse> resp = guildService.getGuildInfo(GUILD_ID);

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData().getName()).isEqualTo("TestGuild");
        }

        @Test
        @DisplayName("TC-GLD-006 [N] Bang hoi khong ton tai – tra ve error(-1)")
        void getGuildInfo_notFound_returnsError() {
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.empty());

            GuildDTO.Response<GuildDTO.InfoResponse> resp = guildService.getGuildInfo(GUILD_ID);

            assertThat(resp.getCode()).isEqualTo(-1);
        }

        @Test
        @DisplayName("TC-GLD-007 [N] Bang hoi da giai tan – tra ve error(-2)")
        void getGuildInfo_disbanded_returnsError() {
            Guild guild = mock(Guild.class);
            given(guild.getActive()).willReturn(false);
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));

            GuildDTO.Response<GuildDTO.InfoResponse> resp = guildService.getGuildInfo(GUILD_ID);

            assertThat(resp.getCode()).isEqualTo(-2);
        }
    }

    // =========================================================
    // applyToGuild
    // =========================================================
    @Nested
    @DisplayName("applyToGuild()")
    class ApplyToGuild {

        private GuildDTO.JoinRequest joinReq(String roleId) {
            return GuildDTO.JoinRequest.builder()
                    .guildId(GUILD_ID)
                    .roleId(roleId)
                    .roleName("Player")
                    .roleLevel(10)
                    .power(1000L)
                    .build();
        }

        @Test
        @DisplayName("TC-GLD-010 [P] Nop don xin vao bang thanh cong")
        void applyToGuild_success() {
            Guild guild = mock(Guild.class);
            given(guild.getActive()).willReturn(true);
            given(guild.isFull()).willReturn(false);
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));
            given(memberRepository.existsByRoleId(anyLong())).willReturn(false);
            given(applicationRepository.existsByGuildIdAndRoleIdAndStatus(eq(GUILD_ID), anyLong(), eq(0))).willReturn(false);
            given(applicationRepository.save(any(GuildApplication.class))).willAnswer(inv -> inv.getArgument(0));

            GuildDTO.Response<Void> resp = guildService.applyToGuild(joinReq(MEMBER_ID));

            assertThat(resp.getCode()).isZero();
            then(applicationRepository).should().save(any(GuildApplication.class));
        }

        @Test
        @DisplayName("TC-GLD-011 [N] Bang hoi khong ton tai – tra ve error(-1)")
        void applyToGuild_guildNotFound_returnsError() {
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.empty());

            GuildDTO.Response<Void> resp = guildService.applyToGuild(joinReq(MEMBER_ID));

            assertThat(resp.getCode()).isEqualTo(-1);
        }

        @Test
        @DisplayName("TC-GLD-012 [N] Bang hoi da giai tan – tra ve error(-2)")
        void applyToGuild_disbanded_returnsError() {
            Guild guild = mock(Guild.class);
            given(guild.getActive()).willReturn(false);
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));

            GuildDTO.Response<Void> resp = guildService.applyToGuild(joinReq(MEMBER_ID));

            assertThat(resp.getCode()).isEqualTo(-2);
        }

        @Test
        @DisplayName("TC-GLD-013 [N] Bang hoi day – tra ve error(-3)")
        void applyToGuild_guildFull_returnsError() {
            Guild guild = mock(Guild.class);
            given(guild.getActive()).willReturn(true);
            given(guild.isFull()).willReturn(true);
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));

            GuildDTO.Response<Void> resp = guildService.applyToGuild(joinReq(MEMBER_ID));

            assertThat(resp.getCode()).isEqualTo(-3);
        }

        @Test
        @DisplayName("TC-GLD-014 [N] Nguoi choi da trong bang – tra ve error(-4)")
        void applyToGuild_alreadyInGuild_returnsError() {
            Guild guild = mock(Guild.class);
            given(guild.getActive()).willReturn(true);
            given(guild.isFull()).willReturn(false);
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));
            given(memberRepository.existsByRoleId(anyLong())).willReturn(true);

            GuildDTO.Response<Void> resp = guildService.applyToGuild(joinReq(MEMBER_ID));

            assertThat(resp.getCode()).isEqualTo(-4);
        }

        @Test
        @DisplayName("TC-GLD-015 [N] Da co don cho duyet – tra ve error(-5)")
        void applyToGuild_pendingApplicationExists_returnsError() {
            Guild guild = mock(Guild.class);
            given(guild.getActive()).willReturn(true);
            given(guild.isFull()).willReturn(false);
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));
            given(memberRepository.existsByRoleId(anyLong())).willReturn(false);
            given(applicationRepository.existsByGuildIdAndRoleIdAndStatus(eq(GUILD_ID), anyLong(), eq(0))).willReturn(true);

            GuildDTO.Response<Void> resp = guildService.applyToGuild(joinReq(MEMBER_ID));

            assertThat(resp.getCode()).isEqualTo(-5);
        }
    }

    // =========================================================
    // leaveGuild
    // =========================================================
    @Nested
    @DisplayName("leaveGuild()")
    class LeaveGuild {

        @Test
        @DisplayName("TC-GLD-020 [P] Roi bang thanh cong")
        void leaveGuild_success() {
            GuildMember member = mock(GuildMember.class);
            given(member.isLeader()).willReturn(false);
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(member));

            Guild guild = new Guild();
            guild.setMemberCount(5);
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));
            given(guildRepository.save(any(Guild.class))).willAnswer(inv -> inv.getArgument(0));

            GuildDTO.Response<Void> resp = guildService.leaveGuild(GUILD_ID, MEMBER_ID);

            assertThat(resp.getCode()).isZero();
            then(memberRepository).should().delete(member);
        }

        @Test
        @DisplayName("TC-GLD-021 [N] Nguoi choi khong trong bang – tra ve error(-1)")
        void leaveGuild_notInGuild_returnsError() {
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.empty());

            GuildDTO.Response<Void> resp = guildService.leaveGuild(GUILD_ID, MEMBER_ID);

            assertThat(resp.getCode()).isEqualTo(-1);
        }

        @Test
        @DisplayName("TC-GLD-022 [N] Truong bang khong the roi – tra ve error(-2)")
        void leaveGuild_leaderCannotLeave_returnsError() {
            GuildMember leader = mock(GuildMember.class);
            given(leader.isLeader()).willReturn(true);
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(leader));

            GuildDTO.Response<Void> resp = guildService.leaveGuild(GUILD_ID, LEADER_ID);

            assertThat(resp.getCode()).isEqualTo(-2);
        }
    }

    // =========================================================
    // kickMember
    // =========================================================
    @Nested
    @DisplayName("kickMember()")
    class KickMember {

        @Test
        @DisplayName("TC-GLD-030 [P] Dua thanh vien ra khoi bang thanh cong")
        void kickMember_success() {
            GuildMember kicker = mock(GuildMember.class);
            given(kicker.isOfficerOrAbove()).willReturn(true);
            given(kicker.getRank()).willReturn(3); // leader

            GuildMember target = mock(GuildMember.class);
            given(target.isLeader()).willReturn(false);
            given(target.getRank()).willReturn(1); // regular member

            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(kicker));
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(target));

            Guild guild = new Guild();
            guild.setMemberCount(5);
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));
            given(guildRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            GuildDTO.Response<Void> resp = guildService.kickMember(GUILD_ID, LEADER_ID, MEMBER_ID);

            assertThat(resp.getCode()).isZero();
            then(memberRepository).should().delete(target);
        }

        @Test
        @DisplayName("TC-GLD-031 [N] Nguoi dua khong co quyen – tra ve error(-1)")
        void kickMember_noPermission_returnsError() {
            GuildMember kicker = mock(GuildMember.class);
            given(kicker.isOfficerOrAbove()).willReturn(false);
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(kicker));

            GuildDTO.Response<Void> resp = guildService.kickMember(GUILD_ID, MEMBER_ID, OFFICER_ID);

            assertThat(resp.getCode()).isEqualTo(-1);
        }

        @Test
        @DisplayName("TC-GLD-032 [N] Khong the dua truong bang ra – tra ve error(-3)")
        void kickMember_cannotKickLeader_returnsError() {
            GuildMember kicker = mock(GuildMember.class);
            given(kicker.isOfficerOrAbove()).willReturn(true);
            given(kicker.getRank()).willReturn(2);

            GuildMember target = mock(GuildMember.class);
            given(target.isLeader()).willReturn(true);

            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(kicker));
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(target));

            GuildDTO.Response<Void> resp = guildService.kickMember(GUILD_ID, OFFICER_ID, LEADER_ID);

            assertThat(resp.getCode()).isEqualTo(-3);
        }

        @Test
        @DisplayName("TC-GLD-033 [N] Pho truong khong the dua pho truong – tra ve error(-4)")
        void kickMember_officerCannotKickOfficer_returnsError() {
            GuildMember kicker = mock(GuildMember.class);
            given(kicker.isOfficerOrAbove()).willReturn(true);
            given(kicker.getRank()).willReturn(2); // officer

            GuildMember target = mock(GuildMember.class);
            given(target.isLeader()).willReturn(false);
            given(target.getRank()).willReturn(2); // also officer

            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(kicker));
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(target));

            GuildDTO.Response<Void> resp = guildService.kickMember(GUILD_ID, OFFICER_ID, MEMBER_ID);

            assertThat(resp.getCode()).isEqualTo(-4);
        }
    }

    // =========================================================
    // promoteMember / demoteMember
    // =========================================================
    @Nested
    @DisplayName("promoteMember() / demoteMember()")
    class PromoteDemote {

        @Test
        @DisplayName("TC-GLD-040 [P] Thang cap thanh vien – thanh cong")
        void promoteMember_success() {
            GuildMember promoter = mock(GuildMember.class);
            given(promoter.isLeader()).willReturn(true);

            GuildMember target = mock(GuildMember.class);
            given(target.promote()).willReturn(true);

            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(promoter));
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(target));
            given(memberRepository.save(target)).willReturn(target);

            GuildDTO.Response<Void> resp = guildService.promoteMember(GUILD_ID, LEADER_ID, MEMBER_ID);

            assertThat(resp.getCode()).isZero();
        }

        @Test
        @DisplayName("TC-GLD-041 [N] Khong phai truong bang – tra ve error(-1)")
        void promoteMember_notLeader_returnsError() {
            GuildMember member = mock(GuildMember.class);
            given(member.isLeader()).willReturn(false);
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(member));

            GuildDTO.Response<Void> resp = guildService.promoteMember(GUILD_ID, MEMBER_ID, OFFICER_ID);

            assertThat(resp.getCode()).isEqualTo(-1);
        }

        @Test
        @DisplayName("TC-GLD-042 [N] Khong the thang cap them – tra ve error(-3)")
        void promoteMember_cannotPromoteFurther_returnsError() {
            GuildMember promoter = mock(GuildMember.class);
            given(promoter.isLeader()).willReturn(true);

            GuildMember target = mock(GuildMember.class);
            given(target.promote()).willReturn(false); // already at max rank

            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(promoter));
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(target));

            GuildDTO.Response<Void> resp = guildService.promoteMember(GUILD_ID, LEADER_ID, OFFICER_ID);

            assertThat(resp.getCode()).isEqualTo(-3);
        }

        @Test
        @DisplayName("TC-GLD-045 [P] Ha cap thanh vien – thanh cong")
        void demoteMember_success() {
            GuildMember demoter = mock(GuildMember.class);
            given(demoter.isLeader()).willReturn(true);

            GuildMember target = mock(GuildMember.class);
            given(target.isLeader()).willReturn(false);
            given(target.demote()).willReturn(true);

            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(demoter));
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(target));
            given(memberRepository.save(target)).willReturn(target);

            GuildDTO.Response<Void> resp = guildService.demoteMember(GUILD_ID, LEADER_ID, OFFICER_ID);

            assertThat(resp.getCode()).isZero();
        }
    }

    // =========================================================
    // disbandGuild
    // =========================================================
    @Nested
    @DisplayName("disbandGuild()")
    class DisbandGuild {

        @Test
        @DisplayName("TC-GLD-050 [P] Giai tan bang hoi – thanh cong")
        void disbandGuild_success() {
            Guild guild = mock(Guild.class);
            given(guild.getLeaderId()).willReturn(Long.parseLong(LEADER_ID));
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));
            given(guildRepository.save(guild)).willReturn(guild);

            GuildDTO.Response<Void> resp = guildService.disbandGuild(GUILD_ID, LEADER_ID);

            assertThat(resp.getCode()).isZero();
            then(guild).should().setActive(false);
            then(memberRepository).should().deleteByGuildId(GUILD_ID);
            then(applicationRepository).should().deleteByGuildId(GUILD_ID);
            then(warehouseRepository).should().deleteByGuildId(GUILD_ID);
        }

        @Test
        @DisplayName("TC-GLD-051 [N] Bang hoi khong ton tai – tra ve error(-1)")
        void disbandGuild_notFound_returnsError() {
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.empty());

            GuildDTO.Response<Void> resp = guildService.disbandGuild(GUILD_ID, LEADER_ID);

            assertThat(resp.getCode()).isEqualTo(-1);
        }

        @Test
        @DisplayName("TC-GLD-052 [N] Khong phai truong bang – tra ve error(-2)")
        void disbandGuild_notLeader_returnsError() {
            Guild guild = mock(Guild.class);
            given(guild.getLeaderId()).willReturn(9999L);
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));

            GuildDTO.Response<Void> resp = guildService.disbandGuild(GUILD_ID, LEADER_ID);

            assertThat(resp.getCode()).isEqualTo(-2);
            then(memberRepository).should(never()).deleteByGuildId(any());
        }
    }

    // =========================================================
    // donate
    // =========================================================
    @Nested
    @DisplayName("donate()")
    class Donate {

        private GuildDTO.DonateRequest donateReq(String roleId, long amount) {
            return GuildDTO.DonateRequest.builder()
                    .guildId(GUILD_ID)
                    .roleId(roleId)
                    .amount(amount)
                    .build();
        }

        @Test
        @DisplayName("TC-GLD-060 [P] Quyen gop vao bang thanh cong")
        void donate_success() {
            GuildMember member = mock(GuildMember.class);
            given(member.canDonateToday()).willReturn(true);
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(member));

            Guild guild = mock(Guild.class);
            given(guild.addExp(anyLong())).willReturn(false);
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));
            given(guildRepository.save(guild)).willReturn(guild);
            given(memberRepository.save(member)).willReturn(member);

            GuildDTO.Response<Void> resp = guildService.donate(donateReq(MEMBER_ID, 1000L));

            assertThat(resp.getCode()).isZero();
            then(guild).should().addFunds(1000L);
            then(member).should().donate();
        }

        @Test
        @DisplayName("TC-GLD-061 [N] Nguoi choi khong phai thanh vien – tra ve error(-1)")
        void donate_notMember_returnsError() {
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.empty());

            GuildDTO.Response<Void> resp = guildService.donate(donateReq(MEMBER_ID, 1000L));

            assertThat(resp.getCode()).isEqualTo(-1);
        }

        @Test
        @DisplayName("TC-GLD-062 [N] Da dat gioi han quyen gop ngay – tra ve error(-2)")
        void donate_dailyLimitReached_returnsError() {
            GuildMember member = mock(GuildMember.class);
            given(member.canDonateToday()).willReturn(false);
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(member));

            GuildDTO.Response<Void> resp = guildService.donate(donateReq(MEMBER_ID, 1000L));

            assertThat(resp.getCode()).isEqualTo(-2);
            assertThat(resp.getMessage()).contains("Daily donation limit");
        }
    }

    // =========================================================
    // transferLeadership
    // =========================================================
    @Nested
    @DisplayName("transferLeadership()")
    class TransferLeadership {

        @Test
        @DisplayName("TC-GLD-070 [P] Chuyen quyen truong bang – thanh cong")
        void transferLeadership_success() {
            Guild guild = mock(Guild.class);
            given(guild.getLeaderId()).willReturn(Long.parseLong(LEADER_ID));

            GuildMember currentLeader = mock(GuildMember.class);
            GuildMember newLeader = mock(GuildMember.class);

            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(currentLeader));
            given(memberRepository.findByGuildIdAndRoleId(eq(GUILD_ID), anyLong()))
                    .willReturn(Optional.of(newLeader));
            given(memberRepository.save(any(GuildMember.class))).willAnswer(inv -> inv.getArgument(0));
            given(guildRepository.save(guild)).willReturn(guild);

            GuildDTO.TransferLeaderRequest req = GuildDTO.TransferLeaderRequest.builder()
                    .guildId(GUILD_ID)
                    .currentLeaderId(LEADER_ID)
                    .newLeaderId(OFFICER_ID)
                    .build();

            GuildDTO.Response<Void> resp = guildService.transferLeadership(req);

            assertThat(resp.getCode()).isZero();
            then(currentLeader).should().setRank(2); // demoted to officer
            then(newLeader).should().setRank(3); // promoted to leader
        }

        @Test
        @DisplayName("TC-GLD-071 [N] Khong phai truong bang hien tai – tra ve error(-2)")
        void transferLeadership_notCurrentLeader_returnsError() {
            Guild guild = mock(Guild.class);
            given(guild.getLeaderId()).willReturn(9999L);
            given(guildRepository.findById(GUILD_ID)).willReturn(Optional.of(guild));

            GuildDTO.TransferLeaderRequest req = GuildDTO.TransferLeaderRequest.builder()
                    .guildId(GUILD_ID)
                    .currentLeaderId(LEADER_ID)
                    .newLeaderId(OFFICER_ID)
                    .build();

            GuildDTO.Response<Void> resp = guildService.transferLeadership(req);

            assertThat(resp.getCode()).isEqualTo(-2);
        }
    }

    // =========================================================
    // getMembers
    // =========================================================
    @Nested
    @DisplayName("getMembers()")
    class GetMembers {

        @Test
        @DisplayName("TC-GLD-080 [P] Lay danh sach thanh vien – tra ve dung so luong")
        void getMembers_returnsMemberList() {
            GuildMember m1 = new GuildMember();
            m1.setId(1L);
            m1.setRoleId(Long.parseLong(LEADER_ID));
            m1.setRank(3);

            GuildMember m2 = new GuildMember();
            m2.setId(2L);
            m2.setRoleId(Long.parseLong(MEMBER_ID));
            m2.setRank(1);

            given(memberRepository.findByGuildIdOrderByRankDescContributionDesc(GUILD_ID))
                    .willReturn(List.of(m1, m2));

            GuildDTO.Response<List<GuildDTO.MemberInfo>> resp = guildService.getMembers(GUILD_ID);

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData()).hasSize(2);
        }
    }
}
