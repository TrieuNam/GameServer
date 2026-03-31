package com.SouthMillion.drop_service.service;

import com.SouthMillion.drop_service.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PityService {
    private final StringRedisTemplate redis;
    private final AppProperties props;

    private String key(String group, String roleId) {
        return "drop:pity:"+group+":"+roleId;
    }

    public int incr(String group, String roleId) {
        Long v = redis.opsForValue().increment(key(group, roleId));
        return v==null?0:v.intValue();
    }
    public int get(String group, String roleId) {
        String v = redis.opsForValue().get(key(group, roleId));
        return v==null?0:Integer.parseInt(v);
    }
    public void reset(String group, String roleId) {
        redis.delete(key(group, roleId));
    }

    public boolean enabled() { return props.getPity().isEnabled(); }
    public int thresholdFor(int dropId) {
        var pd = props.getPity().getPerDrop().get(String.valueOf(dropId));
        if (pd != null && pd.getThreshold()!=null) return pd.getThreshold();
        return props.getPity().getDefaultThreshold();
    }
    public String rareSelectorFor(int dropId) {
        var pd = props.getPity().getPerDrop().get(String.valueOf(dropId));
        return pd!=null && pd.getRareSelector()!=null ? pd.getRareSelector() : props.getPity().getDefaultRareSelector();
    }
    public java.util.List<Integer> rareListFor(int dropId) {
        var pd = props.getPity().getPerDrop().get(String.valueOf(dropId));
        return pd!=null && pd.getRareItemIds()!=null ? pd.getRareItemIds() : java.util.List.of();
    }
    public String groupOf(Integer dropId, String override) {
        return override!=null ? override : ("drop:"+dropId);
    }
}