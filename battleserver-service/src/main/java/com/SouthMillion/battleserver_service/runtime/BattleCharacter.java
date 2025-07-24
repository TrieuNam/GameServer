package com.SouthMillion.battleserver_service.runtime;

import lombok.Data;
import org.SouthMillion.dto.battle.BuffStatusDTO;
import org.SouthMillion.dto.battle.MonsterInstanceDTO;
import org.SouthMillion.dto.battle.PassiveSkillDTO;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Data
public class BattleCharacter {
    private String instanceId;
    private String name;
    private int hp, maxHp, attack, defense, speed;
    private boolean alive = true;
    private List<String> skillIds = new ArrayList<>();
    private List<String> passiveSkillIds = new ArrayList<>();
    private List<BuffStatusDTO> activeBuffs = new ArrayList<>();
    private String state = "normal"; // stun, frozen, normal
    private String side; // "player", "pet", "monster"

    // -- Chuẩn hóa xác định side (dùng cho pick target) --
    public boolean isMonsterSide() { return "monster".equalsIgnoreCase(side); }
    public boolean isPlayerSide()  { return "player".equalsIgnoreCase(side); }
    public boolean isPetSide()     { return "pet".equalsIgnoreCase(side); }
    public boolean isPlayerSideAndAlive() { return (isPlayerSide() || isPetSide()) && isAlive(); }
    public boolean isMonsterSideAndAlive() { return isMonsterSide() && isAlive(); }
    public boolean isAlive() { return alive && hp > 0; }
    public boolean canAct()  { return isAlive() && "normal".equals(state); }

    // -- Cộng passive (theo list passiveSkillIds, tra config) --
    public void applyPassive(List<PassiveSkillDTO> allPassives) {
        if (passiveSkillIds == null) return;
        for (String pid : passiveSkillIds) {
            PassiveSkillDTO p = allPassives.stream()
                    .filter(pp -> pp.getSkillId().equals(pid))
                    .findFirst().orElse(null);
            if (p == null) continue;
            int value = 0;
            try { value = Integer.parseInt(p.getAtt_num2()); } catch (Exception ignore) {}
            // att_type/skill_att_type mapping theo cấu hình thực tế
            switch (p.getSkillAttType()) {
                case "1": this.maxHp += value; this.hp += value; break;   // +HP
                case "2": this.attack += value; break;                    // +ATK
                case "3": this.defense += value; break;                   // +DEF
                case "4": this.speed += value; break;                     // +SPEED
                // ... mapping thêm nếu file passive_skill.json có nhiều loại buff khác
            }
        }
    }

    // -- Áp dụng buff mới --
    public void applyBuff(BuffStatusDTO buff) {
        activeBuffs.add(buff);
        switch (buff.getEffectType()) {
            case "stun":   this.state = "stun"; break;
            case "frozen": this.state = "frozen"; break;
            case "heal":   this.heal(buff.getValue()); break; // heal ngay
            // Shield, reflect, poison, immune: xử lý khi bị damage hoặc tick mỗi round
        }
    }

    // -- Nhận damage, xử lý shield, reflect, immune --
    public int takeDamage(int dmg, BattleCharacter from) {
        int realDmg = dmg;
        if (hasBuff("immune")) return 0; // miễn nhiễm mọi dame

        // Shield absorb trước
        BuffStatusDTO shield = getBuff("shield");
        if (shield != null && shield.getValue() > 0) {
            int shieldVal = shield.getValue();
            if (shieldVal >= realDmg) {
                shield.setValue(shieldVal - realDmg);
                realDmg = 0;
            } else {
                realDmg -= shieldVal;
                shield.setValue(0);
            }
            if (shield.getValue() <= 0) removeBuff("shield");
        }

        // Reflect: trả lại % dame cho attacker
        BuffStatusDTO reflect = getBuff("reflect");
        if (reflect != null && from != null) {
            int reflectDmg = (int)(dmg * reflect.getValue() / 100.0);
            from.takeDamage(reflectDmg, null); // không tiếp tục reflect
        }

        this.hp = Math.max(0, this.hp - realDmg);
        if (this.hp == 0) this.alive = false;
        return realDmg;
    }

    // -- Tick buff mỗi round: giảm duration, trigger poison, remove nếu hết --
    public void tickBuffs() {
        Iterator<BuffStatusDTO> it = activeBuffs.iterator();
        while (it.hasNext()) {
            BuffStatusDTO buff = it.next();
            buff.setDuration(buff.getDuration() - 1);
            switch (buff.getEffectType()) {
                case "poison":
                    int poisonDmg = buff.getValue();
                    this.hp = Math.max(0, this.hp - poisonDmg);
                    if (this.hp == 0) this.alive = false;
                    break;
                // Shield, reflect, immune: không cần xử lý mỗi round
            }
            if (buff.getDuration() <= 0) it.remove();
        }
        // Reset trạng thái nếu không còn stun/frozen
        if (!hasBuff("stun") && !hasBuff("frozen")) state = "normal";
    }

    // -- Hồi máu --
    public void heal(int amount) { this.hp = Math.min(maxHp, this.hp + amount); }

    // -- Kiểm tra có buff loại gì không --
    public boolean hasBuff(String type) {
        return activeBuffs.stream().anyMatch(b -> type.equals(b.getEffectType()) && b.getDuration() > 0);
    }

    // -- Lấy buff theo type --
    public BuffStatusDTO getBuff(String type) {
        return activeBuffs.stream().filter(b -> type.equals(b.getEffectType()) && b.getDuration() > 0).findFirst().orElse(null);
    }

    // -- Xóa buff loại type --
    public void removeBuff(String type) {
        activeBuffs.removeIf(b -> type.equals(b.getEffectType()));
    }

    // -- Convert về MonsterInstanceDTO để trả về kết quả cuối trận --
    public MonsterInstanceDTO toMonsterInstanceDTO() {
        MonsterInstanceDTO mi = new MonsterInstanceDTO();
        mi.setInstanceId(this.instanceId);
        mi.setHp(this.hp);
        mi.setAttack(this.attack);
        mi.setDefense(this.defense);
        mi.setSpeed(this.speed);
        mi.setAlive(this.isAlive());
        mi.setActiveBuffs(new ArrayList<>(this.activeBuffs));
        mi.setState(this.state);
        mi.setSkillIds(this.skillIds != null ? new ArrayList<>(this.skillIds) : null);
        mi.setPassiveSkillIds(this.passiveSkillIds != null ? new ArrayList<>(this.passiveSkillIds) : null);
        return mi;
    }
}
