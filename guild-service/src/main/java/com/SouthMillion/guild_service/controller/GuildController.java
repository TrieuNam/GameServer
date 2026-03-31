package com.SouthMillion.guild_service.controller;

import com.SouthMillion.guild_service.dto.GuildDTO;
import com.SouthMillion.guild_service.service.GuildService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Guild REST Controller
 * 
 * Provides RESTful API for guild management
 * Used by GuildHandler (WebSocket) and direct HTTP clients
 */
@Slf4j
@RestController
@RequestMapping("/api/guild")
@RequiredArgsConstructor
@Validated
public class GuildController {

    private final GuildService guildService;

    /**
     * Create guild
     * POST /api/guild/create
     */
    @PostMapping("/create")
    public ResponseEntity<GuildDTO.Response<GuildDTO.InfoResponse>> createGuild(
            @Valid @RequestBody GuildDTO.CreateRequest request) {
        log.info("REST API: Create guild - name={}, leaderId={}", request.getName(), request.getLeaderId());
        GuildDTO.Response<GuildDTO.InfoResponse> response = guildService.createGuild(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get guild info
     * GET /api/guild/{guildId}
     */
    @GetMapping("/{guildId}")
    public ResponseEntity<GuildDTO.Response<GuildDTO.InfoResponse>> getGuildInfo(
            @PathVariable @NotNull Long guildId) {
        log.info("REST API: Get guild info - guildId={}", guildId);
        GuildDTO.Response<GuildDTO.InfoResponse> response = guildService.getGuildInfo(guildId);
        return ResponseEntity.ok(response);
    }

    /**
     * Compatibility endpoint for websocket-server.
     * GET /api/guild/player/{roleId}
     */
    @GetMapping("/player/{roleId}")
    public ResponseEntity<Map<String, Object>> getPlayerGuild(
            @PathVariable @NotBlank String roleId) {
        log.info("REST API: Get player guild - roleId={}", roleId);
        return ResponseEntity.ok(guildService.getPlayerGuildCompat(roleId));
    }

    /**
     * Search guilds
     * POST /api/guild/search
     */
    @PostMapping("/search")
    public ResponseEntity<GuildDTO.Response<GuildDTO.PageResponse<GuildDTO.ListItem>>> searchGuilds(
            @Valid @RequestBody GuildDTO.SearchRequest request) {
        log.info("REST API: Search guilds - keyword={}, page={}, size={}", 
                 request.getKeyword(), request.getPage(), request.getSize());
        GuildDTO.Response<GuildDTO.PageResponse<GuildDTO.ListItem>> response = guildService.searchGuilds(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Apply to join guild
     * POST /api/guild/apply
     */
    @PostMapping("/apply")
    public ResponseEntity<GuildDTO.Response<Void>> applyToGuild(
            @Valid @RequestBody GuildDTO.JoinRequest request) {
        log.info("REST API: Apply to guild - roleId={}, guildId={}", request.getRoleId(), request.getGuildId());
        GuildDTO.Response<Void> response = guildService.applyToGuild(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Process application
     * POST /api/guild/application/process
     */
    @PostMapping("/application/process")
    public ResponseEntity<GuildDTO.Response<Void>> processApplication(
            @Valid @RequestBody GuildDTO.ProcessApplicationRequest request) {
        log.info("REST API: Process application - appId={}, processorId={}, approve={}", 
                 request.getApplicationId(), request.getProcessorId(), request.getApprove());
        GuildDTO.Response<Void> response = guildService.processApplication(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Leave guild
     * DELETE /api/guild/{guildId}/member/{roleId}
     */
    @DeleteMapping("/{guildId}/member/{roleId}")
    public ResponseEntity<GuildDTO.Response<Void>> leaveGuild(
            @PathVariable @NotNull Long guildId,
            @PathVariable @NotBlank String roleId) {
        log.info("REST API: Leave guild - guildId={}, roleId={}", guildId, roleId);
        GuildDTO.Response<Void> response = guildService.leaveGuild(guildId, roleId);
        return ResponseEntity.ok(response);
    }

    /**
     * Kick member
     * DELETE /api/guild/{guildId}/kick
     */
    @DeleteMapping("/{guildId}/kick")
    public ResponseEntity<GuildDTO.Response<Void>> kickMember(
            @PathVariable @NotNull Long guildId,
            @RequestParam @NotBlank String kickerId,
            @RequestParam @NotBlank String targetId) {
        log.info("REST API: Kick member - guildId={}, kickerId={}, targetId={}", guildId, kickerId, targetId);
        GuildDTO.Response<Void> response = guildService.kickMember(guildId, kickerId, targetId);
        return ResponseEntity.ok(response);
    }

    /**
     * Promote member
     * PUT /api/guild/{guildId}/promote
     */
    @PutMapping("/{guildId}/promote")
    public ResponseEntity<GuildDTO.Response<Void>> promoteMember(
            @PathVariable @NotNull Long guildId,
            @RequestParam @NotBlank String promoterId,
            @RequestParam @NotBlank String targetId) {
        log.info("REST API: Promote member - guildId={}, promoterId={}, targetId={}", guildId, promoterId, targetId);
        GuildDTO.Response<Void> response = guildService.promoteMember(guildId, promoterId, targetId);
        return ResponseEntity.ok(response);
    }

    /**
     * Demote member
     * PUT /api/guild/{guildId}/demote
     */
    @PutMapping("/{guildId}/demote")
    public ResponseEntity<GuildDTO.Response<Void>> demoteMember(
            @PathVariable @NotNull Long guildId,
            @RequestParam @NotBlank String demoterId,
            @RequestParam @NotBlank String targetId) {
        log.info("REST API: Demote member - guildId={}, demoterId={}, targetId={}", guildId, demoterId, targetId);
        GuildDTO.Response<Void> response = guildService.demoteMember(guildId, demoterId, targetId);
        return ResponseEntity.ok(response);
    }

    /**
     * Transfer leadership
     * PUT /api/guild/transfer-leader
     */
    @PutMapping("/transfer-leader")
    public ResponseEntity<GuildDTO.Response<Void>> transferLeadership(
            @Valid @RequestBody GuildDTO.TransferLeaderRequest request) {
        log.info("REST API: Transfer leadership - guildId={}, from={}, to={}", 
                 request.getGuildId(), request.getCurrentLeaderId(), request.getNewLeaderId());
        GuildDTO.Response<Void> response = guildService.transferLeadership(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Disband guild
     * DELETE /api/guild/{guildId}/disband
     */
    @DeleteMapping("/{guildId}/disband")
    public ResponseEntity<GuildDTO.Response<Void>> disbandGuild(
            @PathVariable @NotNull Long guildId,
            @RequestParam @NotBlank String leaderId) {
        log.info("REST API: Disband guild - guildId={}, leaderId={}", guildId, leaderId);
        GuildDTO.Response<Void> response = guildService.disbandGuild(guildId, leaderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Donate to guild
     * POST /api/guild/donate
     */
    @PostMapping("/donate")
    public ResponseEntity<GuildDTO.Response<Void>> donate(
            @Valid @RequestBody GuildDTO.DonateRequest request) {
        log.info("REST API: Donate to guild - roleId={}, guildId={}, amount={}", 
                 request.getRoleId(), request.getGuildId(), request.getAmount());
        GuildDTO.Response<Void> response = guildService.donate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Upgrade technology
     * POST /api/guild/tech/upgrade
     */
    @PostMapping("/tech/upgrade")
    public ResponseEntity<GuildDTO.Response<Void>> upgradeTech(
            @Valid @RequestBody GuildDTO.UpgradeTechRequest request) {
        log.info("REST API: Upgrade tech - guildId={}, roleId={}, techType={}", 
                 request.getGuildId(), request.getRoleId(), request.getTechType());
        GuildDTO.Response<Void> response = guildService.upgradeTech(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Edit guild notice
     * PUT /api/guild/notice
     */
    @PutMapping("/notice")
    public ResponseEntity<GuildDTO.Response<Void>> editNotice(
            @Valid @RequestBody GuildDTO.EditNoticeRequest request) {
        log.info("REST API: Edit notice - guildId={}, roleId={}", request.getGuildId(), request.getRoleId());
        GuildDTO.Response<Void> response = guildService.editNotice(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get guild members
     * GET /api/guild/{guildId}/members
     */
    @GetMapping("/{guildId}/members")
    public ResponseEntity<GuildDTO.Response<List<GuildDTO.MemberInfo>>> getMembers(
            @PathVariable @NotNull Long guildId) {
        log.info("REST API: Get members - guildId={}", guildId);
        GuildDTO.Response<List<GuildDTO.MemberInfo>> response = guildService.getMembers(guildId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get guild applications
     * GET /api/guild/{guildId}/applications
     */
    @GetMapping("/{guildId}/applications")
    public ResponseEntity<GuildDTO.Response<List<GuildDTO.ApplicationInfo>>> getApplications(
            @PathVariable @NotNull Long guildId) {
        log.info("REST API: Get applications - guildId={}", guildId);
        GuildDTO.Response<List<GuildDTO.ApplicationInfo>> response = guildService.getApplications(guildId);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check
     * GET /api/guild/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Guild Service is running!");
    }
}
