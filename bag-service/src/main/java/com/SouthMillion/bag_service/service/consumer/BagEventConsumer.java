package com.SouthMillion.bag_service.service.consumer;

import com.SouthMillion.bag_service.service.BagDomainService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.event.bag.BagChangedEvent;
import org.SouthMillion.dto.role.event.BagGrantEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BagEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BagEventConsumer.class);

    private final BagDomainService svc;
    private final StringRedisTemplate redis;

    // === NEW: inject producer
    private final KafkaTemplate<String, Object> kafka;
    @Value("${app.kafka.bag-changed-topic:gameh5.bag.changed}")
    private String bagChangedTopic;

    @Value("${app.kafka.idempotent-ttl-sec:600}")
    private long idemTtl;

    @KafkaListener(topics = "${app.kafka.bag-grant-topic:gameh5.bag.grant}")
    public void onGrant(BagGrantEvent evt, Acknowledgment ack) {
        if (evt == null) { ack.acknowledge(); return; }

        String eventId = evt.getEventId();
        if (eventId == null || eventId.isBlank()) {
            log.warn("BagGrantEvent thiếu eventId, bỏ qua để tránh cấp trùng.");
            ack.acknowledge();
            return;
        }

        String key = "bag:evt:" + eventId;
        try {
            if (redis.hasKey(key)) {
                log.debug("Skip nhanh do Redis có key: {}", key);
                ack.acknowledge();
                return;
            }

            var items = evt.getItems().stream()
                    .map(i -> BagDTOs.GrantItem.builder()
                            .itemId(i.getItemId())
                            .num(i.getNum())
                            .bind(Boolean.TRUE.equals(i.getBind()) ? 1 : 0)
                            .expireAt(i.getExpireAt())
                            .build())
                    .toList();

            // === 1) Cập nhật DB (grant) – @Transactional bên trong
            var result = svc.grant(evt.getUserId(), Long.valueOf(evt.getRoleId()), items, eventId, false);
            boolean processed = (result != null && !result.isEmpty());

            if (processed) {
                // === 2) Chuẩn bị map itemId -> newNum để publish
                var newNumByItem = result.stream()
                        .filter(iv -> iv.getItemId() != null)
                        .collect(java.util.stream.Collectors.toMap(
                                BagDTOs.ItemView::getItemId,
                                iv -> iv.getNum() == null ? 0L : iv.getNum(),
                                (a,b) -> b));

                var now = java.time.Instant.now();

                // === 3) Gửi bag.changed cho từng item từ sự kiện gốc
                // (đảm bảo cùng key = roleId để giữ thứ tự)
                for (var it : evt.getItems()) {
                    Integer itemId = it.getItemId();
                    long delta = it.getNum() == null ? 0L : it.getNum();
                    Long newNum = newNumByItem.get(itemId); // thường có; nếu null có thể bỏ qua/push all sau

                    var out = BagChangedEvent.builder()
                            .eventId(eventId)
                            .userId(evt.getUserId())
                            .roleId(evt.getRoleId())
                            .itemId(itemId)
                            .delta(delta)
                            .newNum(newNum)
                            .reason("grant")
                            .at(now)
                            .build();

                    // Gửi đồng bộ để chỉ ack sau khi publish OK:
                    kafka.send(bagChangedTopic, evt.getRoleId(), out).get(3, java.util.concurrent.TimeUnit.SECONDS);
                }

                // === 4) Đặt Redis idempotent (sau khi DB + publish OK)
                redis.opsForValue().set(key, "1", java.time.Duration.ofSeconds(idemTtl));
                log.info("Grant processed & published changed: eventId={}, itemCount={}", eventId, result.size());
            } else {
                log.info("Grant ignored (idempotent duplicate): eventId={}", eventId);
            }

            // Chỉ ack sau khi mọi thứ OK
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Grant event xử lý lỗi (eventId={}): {}", eventId, ex.getMessage(), ex);
            // Không set Redis để cho redelivery
            // Không ack để container retry theo config
            throw new RuntimeException(ex);
        }
    }
}