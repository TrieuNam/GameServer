package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign Client for guild-service
 *
 * Khớp với GuildController endpoints hiện tại (POST /api/guild/...):
 *
 * ⚠ GuildController đã thay đổi nhiều so với phiên bản cũ:
 *   - Không còn GET /api/guild/player/{roleId}  → cần bổ sung lại vào guild-service
 *   - join/leave/approve đổi thành apply/application/process + DELETE member
 *   - donate/tech/upgrade không còn nhận guildId trong path (nhận trong body)
 *   - search guilds đổi từ GET → POST
 */
@FeignClient(name = "guild-service")
public interface GuildFeign {

    /** GET /api/guild/{guildId} – thông tin bang hội */
    @GetMapping("/api/guild/{guildId}")
    Map<String, Object> getGuildInfo(@PathVariable("guildId") Long guildId);

    /**
     * GET /api/guild/player/{roleId}
     * ⚠ Endpoint này CHƯA CÓ trong GuildController mới.
     *   Cần thêm vào guild-service GuildController để GuildHandler hoạt động đúng.
     *   Hiện tại sẽ trả về 404 / exception.
     */
    @GetMapping("/api/guild/player/{roleId}")
    Map<String, Object> getPlayerGuild(@PathVariable("roleId") String roleId);

    /** POST /api/guild/create – tạo bang hội.  Body: {roleId, name} */
    @PostMapping("/api/guild/create")
    Map<String, Object> createGuild(@RequestBody Map<String, Object> request);

    /**
     * POST /api/guild/apply – nộp đơn xin gia nhập bang hội.
     * Body: {roleId, guildId}
     * (thay thế endpoint cũ POST /api/guild/{guildId}/join)
     */
    @PostMapping("/api/guild/apply")
    Map<String, Object> joinGuild(@RequestBody Map<String, Object> request);

    /**
     * DELETE /api/guild/{guildId}/member/{roleId} – rời bang hội.
     * (thay thế endpoint cũ POST /api/guild/{guildId}/leave)
     */
    @DeleteMapping("/api/guild/{guildId}/member/{roleId}")
    Map<String, Object> leaveGuild(@PathVariable("guildId") Long guildId,
                                   @PathVariable("roleId") String roleId);

    /**
     * POST /api/guild/donate – nộp cống hiến.
     * Body: {roleId, guildId, amount}
     * (guildId không còn trong path, chuyển vào body)
     */
    @PostMapping("/api/guild/donate")
    Map<String, Object> donate(@RequestBody Map<String, Object> request);

    /**
     * POST /api/guild/tech/upgrade – nâng cấp công nghệ.
     * Body: {roleId, guildId, techType}
     * (guildId không còn trong path, chuyển vào body)
     */
    @PostMapping("/api/guild/tech/upgrade")
    Map<String, Object> upgradeTech(@RequestBody Map<String, Object> request);

    /** GET /api/guild/{guildId}/members – danh sách thành viên */
    @GetMapping("/api/guild/{guildId}/members")
    List<Map<String, Object>> getMembers(@PathVariable("guildId") Long guildId);

    /**
     * POST /api/guild/search – tìm kiếm bang hội.
     * Body: {keyword, page, size}
     * (đổi từ GET /api/guild/search → POST /api/guild/search)
     */
    @PostMapping("/api/guild/search")
    List<Map<String, Object>> searchGuilds(@RequestBody Map<String, Object> request);

    /**
     * POST /api/guild/application/process – xử lý đơn xin gia nhập.
     * Body: {applicationId, processorId, approve}
     * (thay thế endpoint cũ POST /api/guild/{guildId}/approve)
     */
    @PostMapping("/api/guild/application/process")
    Map<String, Object> approveApplication(@RequestBody Map<String, Object> request);

    /** GET /api/guild/{guildId}/applications – danh sách đơn chờ duyệt */
    @GetMapping("/api/guild/{guildId}/applications")
    List<Map<String, Object>> getApplications(@PathVariable("guildId") Long guildId);
}
