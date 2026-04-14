package com.SouthMillion.activity_service.integration;

import com.SouthMillion.activity_service.client.AngelFeign;
import com.SouthMillion.activity_service.client.BagFeign;
import com.SouthMillion.activity_service.client.BoxFeign;
import com.SouthMillion.activity_service.client.ConfigFeign;
import com.SouthMillion.activity_service.client.RoleFeign;
import com.SouthMillion.activity_service.client.WalletFeign;
import com.SouthMillion.activity_service.entity.FriendInvite;
import com.SouthMillion.activity_service.repository.FriendInviteRepository;
import com.SouthMillion.activity_service.repository.FriendInviteShareProgressRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FriendInviteSharePlayIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FriendInviteRepository friendInviteRepository;

    @Autowired
    private FriendInviteShareProgressRepository progressRepository;

    @MockBean
    private WalletFeign walletFeign;

    @MockBean
    private AngelFeign angelFeign;

    @MockBean
    private BagFeign bagFeign;

    @MockBean
    private BoxFeign boxFeign;

    @MockBean
    private ConfigFeign configFeign;

    @MockBean
    private RoleFeign roleFeign;

    @Test
    void sharePlayShouldIncreaseInviteCountOnlyOnceForSameInviterInvitedPair() throws Exception {
        Long invitedRoleId = 20001L;
        Long inviterRoleId = 10001L;

        Map<String, Object> req = Map.of(
                "userId", 90002L,
                "shareUserId", 90001L,
                "roleId", invitedRoleId,
                "shareRoleId", inviterRoleId,
                "shareServerId", 1
        );

        mockMvc.perform(post("/api/activity/internal/friend-invite/share-play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret").value(0))
                .andExpect(jsonPath("$.added").value(1))
                .andExpect(jsonPath("$.friendCount").value(1));

        mockMvc.perform(post("/api/activity/internal/friend-invite/share-play")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret").value(0))
                .andExpect(jsonPath("$.added").value(0))
                .andExpect(jsonPath("$.friendCount").value(1));

        FriendInvite inviter = friendInviteRepository.findByRoleId(inviterRoleId).orElse(null);
        assertThat(inviter).isNotNull();
        assertThat(inviter.getInviteCount()).isEqualTo(1);
        assertThat(progressRepository.existsByInviterRoleIdAndInvitedRoleId(inviterRoleId, invitedRoleId)).isTrue();
    }
}
