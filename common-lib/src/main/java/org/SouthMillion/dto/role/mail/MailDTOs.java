package org.SouthMillion.dto.role.mail;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class MailDTOs {

    public record MailListReq(@NotBlank String userId) {
    }

    public record MailSummary(
            String mailId, String title, boolean read, boolean fetched,
            boolean hasReward, Instant expireAt, Instant createdAt
    ) {
    }

    public record MailListResp(List<MailSummary> mails) {
    }

    public record MailDetailResp(
            String mailId, String userId, String title, String content,
            boolean read, boolean fetched, List<MailItem> items,
            Instant expireAt, Instant createdAt
    ) {
    }

    public record MailDeleteResp(String mailId, boolean deleted) {
    }

    public record FetchMailResp(String mailId, boolean fetched, List<MailItem> items) {
    }
}