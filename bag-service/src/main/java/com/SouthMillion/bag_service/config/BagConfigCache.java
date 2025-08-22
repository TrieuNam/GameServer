package com.SouthMillion.bag_service.config;

import com.SouthMillion.bag_service.service.config.ConfigFeign;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class BagConfigCache {

    private final ConfigFeign configFeign;
    private final ObjectMapper om = new ObjectMapper();
    private final AtomicReference<String> etag = new AtomicReference<>();
    @Getter
    private volatile BagCfg cfg = new BagCfg();

    @Getter
    public static class BagCfg {
        public List<Map<String,Object>> bag = List.of();          // bag_id / start_num / add_num / max_num
        public List<Map<String,Object>> expand_price = List.of(); // times + price per bag
        public List<Map<String,Object>> job_bag = List.of();      // old_id/job_type/new_id
    }

    public synchronized void refresh() {
        ResponseEntity<byte[]> resp = configFeign.getLogic("bag_cfg_auto.json", etag.get());
        if (resp.getStatusCode().is2xxSuccessful() && resp.getBody()!=null) {
            String json = new String(resp.getBody(), StandardCharsets.UTF_8);
            try {
                BagCfg newCfg = om.readValue(json, BagCfg.class);
                if (newCfg != null) {
                    this.cfg = newCfg;
                }
            } catch (Exception e) {
                // log & ignore
            }
            if (resp.getHeaders().getETag()!=null) {
                etag.set(resp.getHeaders().getETag());
            }
        }
    }
}