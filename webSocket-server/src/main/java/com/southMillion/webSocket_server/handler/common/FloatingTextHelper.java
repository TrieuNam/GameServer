package com.SouthMillion.webSocket_server.handler.common;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MsgIds;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper for emitting floating-text UI notifications (msgId 1710, PB_SCFloatingTextEvent).
 *
 * Floating text types:
 *   1 = damage   (red,   e.g. combat hit numbers)
 *   2 = loot     (white, e.g. "+Sword[Rare]")
 *   3 = exp      (cyan,  e.g. "+1200 EXP")
 *   4 = buff     (green, e.g. "ATK+15%")
 *   5 = coin     (gold,  e.g. "+500 Gold")
 *
 * Colors (hex):
 *   GOLD   = 0xFFD700   (coin, loot)
 *   GREEN  = 0x00E676   (equip, buff)
 *   CYAN   = 0x00BCD4   (exp)
 *   RED    = 0xFF5252   (damage)
 *   WHITE  = 0xFFFFFF   (generic loot)
 */
@Slf4j
@Component
public class FloatingTextHelper {

    // ── color constants ───────────────────────────────────────────────────────
    public static final int COLOR_GOLD  = 0xFFD700;
    public static final int COLOR_GREEN = 0x00E676;
    public static final int COLOR_CYAN  = 0x00BCD4;
    public static final int COLOR_RED   = 0xFF5252;
    public static final int COLOR_WHITE = 0xFFFFFF;

    // ── type constants ────────────────────────────────────────────────────────
    public static final int TYPE_DAMAGE = 1;
    public static final int TYPE_LOOT   = 2;
    public static final int TYPE_EXP    = 3;
    public static final int TYPE_BUFF   = 4;
    public static final int TYPE_COIN   = 5;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Send one or more floating text items to a player session.
     */
    public void emit(PlayerSession ps, List<FloatingItem> items) {
        if (ps == null || items == null || items.isEmpty()) return;
        try {
            Msgother.PB_SCFloatingTextEvent.Builder event = Msgother.PB_SCFloatingTextEvent.newBuilder();
            for (FloatingItem item : items) {
                Msgother.PB_FloatingTextItem.Builder b = Msgother.PB_FloatingTextItem.newBuilder()
                        .setType(item.type())
                        .setColor(item.color());
                if (item.text() != null) b.setText(item.text());
                if (item.val() != 0f)    b.setVal(item.val());
                event.addItems(b.build());
            }
            Emitters.emit(ps, MsgIds.SC_FLOATING_TEXT_EVENT, event.build().toByteArray());
        } catch (Exception e) {
            log.warn("[FloatingText] emit failed roleId={}: {}", ps.getRoleId(), e.getMessage());
        }
    }

    /**
     * Convenience: emit a single item.
     */
    public void emitOne(PlayerSession ps, int type, String text, int color, float val) {
        emit(ps, List.of(new FloatingItem(type, text, color, val)));
    }

    // ── factory helpers ───────────────────────────────────────────────────────

    /** "+{coin} Gold" in gold color. */
    public void emitCoin(PlayerSession ps, long coin) {
        if (coin <= 0) return;
        emitOne(ps, TYPE_COIN, "+" + coin + " Gold", COLOR_GOLD, coin);
    }

    /** "+{exp} EXP" in cyan color. */
    public void emitExp(PlayerSession ps, long exp) {
        if (exp <= 0) return;
        emitOne(ps, TYPE_EXP, "+" + exp + " EXP", COLOR_CYAN, exp);
    }

    /** "EQUIPPED!" in green. */
    public void emitEquipped(PlayerSession ps) {
        emitOne(ps, TYPE_LOOT, "EQUIPPED!", COLOR_GREEN, 0);
    }

    /** "+{itemText}" in gold. Used for loot/reward items. */
    public void emitLoot(PlayerSession ps, String itemText) {
        if (itemText == null || itemText.isBlank()) return;
        emitOne(ps, TYPE_LOOT, "+" + itemText, COLOR_GOLD, 0);
    }

    /**
     * Convenience builder: construct a batch of floating text items and emit them together.
     * Usage:
     * <pre>
     *   floatingTextHelper.batch()
     *       .coin(500)
     *       .exp(1200)
     *       .send(session);
     * </pre>
     */
    public Batch batch() {
        return new Batch(this);
    }

    // ─────────────────────────────────────────────────────────────────────────

    public record FloatingItem(int type, String text, int color, float val) {}

    public static final class Batch {
        private final FloatingTextHelper helper;
        private final List<FloatingItem> items = new ArrayList<>();

        private Batch(FloatingTextHelper helper) {
            this.helper = helper;
        }

        public Batch coin(long amount) {
            if (amount > 0) items.add(new FloatingItem(TYPE_COIN, "+" + amount + " Gold", COLOR_GOLD, amount));
            return this;
        }

        public Batch exp(long amount) {
            if (amount > 0) items.add(new FloatingItem(TYPE_EXP, "+" + amount + " EXP", COLOR_CYAN, amount));
            return this;
        }

        public Batch equipped() {
            items.add(new FloatingItem(TYPE_LOOT, "EQUIPPED!", COLOR_GREEN, 0));
            return this;
        }

        public Batch loot(String text) {
            if (text != null && !text.isBlank())
                items.add(new FloatingItem(TYPE_LOOT, "+" + text, COLOR_GOLD, 0));
            return this;
        }

        public void send(PlayerSession ps) {
            helper.emit(ps, items);
        }
    }
}
