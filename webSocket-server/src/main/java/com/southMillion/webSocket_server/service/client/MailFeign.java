package com.SouthMillion.webSocket_server.service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Feign Client for mail-service  (path prefix: /api/mail)
 *
 * Khớp với MailController endpoints hiện tại:
 *   POST   /api/mail/send           – gửi thư
 *   POST   /api/mail/send-bulk      – gửi hàng loạt
 *   GET    /api/mail/list/{roleId}  – lấy danh sách thư
 *   PUT    /api/mail/{mailId}/read  – đánh dấu đã đọc  (POST cũ → PUT mới)
 *   POST   /api/mail/{mailId}/claim – nhận vật phẩm đính kèm  (fetch cũ → claim mới)
 *   DELETE /api/mail/{mailId}       – xoá thư
 *
 * ⚠  Các endpoint sau đã bị xoá khỏi MailController, cần bổ sung lại nếu muốn dùng:
 *     GET  /api/mail/detail/{mailId}         (chi tiết thư)
 *     DELETE /api/mail/batch/read/{roleId}   (xoá tất cả thư đã đọc)
 *     POST /api/mail/fetch/all/{roleId}      (nhận toàn bộ vật phẩm)
 */
@FeignClient(name = "mail-service", path = "/api/mail")
public interface MailFeign {

    /** GET /api/mail/list/{roleId} – danh sách thư tóm tắt */
    @GetMapping("/list/{roleId}")
    Map<String, Object> getMailList(@PathVariable("roleId") String roleId);

    /**
     * GET /api/mail/detail/{mailId}
     * ⚠ Endpoint này chưa có trong MailController hiện tại.
     *   Trả về null / exception cho đến khi mail-service bổ sung.
     */
    @GetMapping("/detail/{mailId}")
    Map<String, Object> getMailDetail(@PathVariable("mailId") Long mailId);

    /** PUT /api/mail/{mailId}/read – đánh dấu đã đọc (đổi từ POST → PUT) */
    @PutMapping("/{mailId}/read")
    Map<String, Object> markAsRead(@PathVariable("mailId") Long mailId);

    /** DELETE /api/mail/{mailId} – xoá thư */
    @DeleteMapping("/{mailId}")
    Map<String, Object> deleteMail(@PathVariable("mailId") Long mailId);

    /**
     * POST /api/mail/{mailId}/claim – nhận vật phẩm đính kèm  (đổi từ /fetch → /claim)
     * roleId truyền qua query-param để mail-service validate ownership.
     */
    @PostMapping("/{mailId}/claim")
    Map<String, Object> fetchAttachment(@PathVariable("mailId") Long mailId,
                                        @RequestParam("roleId") String roleId);

    /**
     * POST /api/mail/send – gửi thư (hợp nhất sendSystemMail & sendPlayerMail).
     * Body phải chứa "type": "SYSTEM" hoặc "PLAYER".
     */
    @PostMapping("/send")
    Map<String, Object> sendMail(@RequestBody Map<String, Object> request);

    /** POST /api/mail/send-bulk – gửi hàng loạt */
    @PostMapping("/send-bulk")
    Map<String, Object> sendBulkMail(@RequestBody Map<String, Object> request);

    /** DELETE /api/mail/batch/read/{roleId} – xoá tất cả thư đã đọc */
    @DeleteMapping("/batch/read/{roleId}")
    Map<String, Object> deleteAllReadMails(@PathVariable("roleId") String roleId);

    /** POST /api/mail/fetch/all/{roleId} – nhận toàn bộ vật phẩm đính kèm */
    @PostMapping("/fetch/all/{roleId}")
    Map<String, Object> fetchAllAttachments(@PathVariable("roleId") String roleId);
}
