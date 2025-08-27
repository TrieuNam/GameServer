package com.SouthMillion.pet_service.config;

import com.SouthMillion.pet_service.entity.PetRoleEntity;

import java.util.List;
import java.util.Map;

/**
 * Tính attr/capability cơ bản (MVP):
 *  - Base từ pet(pet_id).pet_att
 *  - Cộng dồn pet_up theo (pet_type, <= level)
 *  - (TODO) cộng gem/ts-gem/advance/order nếu cần sau
 */
public class AttrCalculator {

    public static class Result {
        public final List<Integer> attrList; // [hp, atk, def, spd]
        public final long capability;
        public Result(List<Integer> attrList, long capability) {
            this.attrList = attrList; this.capability = capability;
        }
    }

    @SuppressWarnings("unchecked")
    public static Result compute(PetRoleEntity pe, PetConfigCache cfg) {
        var petBase = cfg.petBaseById().get(pe.getPetId());
        if (petBase == null) {
            return new Result(List.of(0,0,0,0), 0);
        }
        int petType = asInt(petBase.get("pet_type"));
        // base pet_att
        int hp = 0, atk = 0, def = 0, spd = 0;
        var petAtt = (List<Map<String,Object>>) petBase.getOrDefault("pet_att", List.of());
        for (var a : petAtt) {
            int type = asInt(a.get("type"));
            int add  = asInt(a.get("add"));
            switch (type) {
                case 1 -> hp  += add;
                case 2 -> atk += add;
                case 3 -> def += add;
                case 4 -> spd += add;
            }
        }
        // sum up_att up to current level
        var upListByType = cfg.petUpByType().getOrDefault(petType, List.of());
        int curLevel = Math.max(1, pe.getLevel());
        for (var row : upListByType) {
            int lvl = asInt(row.get("pet_level"));
            if (lvl > curLevel) break;
            var upAtt = (List<Map<String,Object>>) row.getOrDefault("up_att", List.of());
            for (var a : upAtt) {
                int type = asInt(a.get("type"));
                int add  = asInt(a.get("add"));
                switch (type) {
                    case 1 -> hp  += add;
                    case 2 -> atk += add;
                    case 3 -> def += add;
                    case 4 -> spd += add;
                }
            }
        }
        // TODO: cộng gem/ts-gem/order nếu cần

        // capability tạm: wHp=1, wAtk=1, wDef=1, wSpd=1 (bạn có thể kéo weight từ other[])
        long cap = (long) hp + atk + def + spd;
        return new Result(List.of(hp, atk, def, spd), cap);
    }

    private static int asInt(Object o){
        if (o == null) return 0;
        if (o instanceof Integer i) return i;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s && !s.isBlank()) return Integer.parseInt(s.trim());
        return 0;
    }
}