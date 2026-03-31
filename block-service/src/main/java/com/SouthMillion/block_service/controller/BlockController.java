package com.SouthMillion.block_service.controller;

import com.SouthMillion.block_service.service.BlockStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/block")
@RequiredArgsConstructor
@Slf4j
public class BlockController {

    private final BlockStateService blockStateService;

    @GetMapping("/info")
    public Map<String, Object> info(@RequestParam("roleId") String roleId) {
        Long parsedRoleId = Long.parseLong(roleId);
        log.debug("[block-service] info roleId={}", parsedRoleId);
        return blockStateService.info(parsedRoleId);
    }

    @PostMapping("/inlay")
    public Map<String, Object> inlay(@RequestBody Map<String, Object> req) {
        return blockStateService.inlay(
            asLong(req.get("roleId")),
            asInt(req.get("mapId")),
            asInt(req.get("blockIndex")),
            asInt(req.get("posX")),
            asInt(req.get("posY"))
        );
    }

    @PostMapping("/remove")
    public Map<String, Object> remove(@RequestBody Map<String, Object> req) {
        return blockStateService.remove(
            asLong(req.get("roleId")),
            asInt(req.get("mapId")),
            asInt(req.get("blockIndex"))
        );
    }

    @PostMapping("/compose")
    public Map<String, Object> compose(@RequestBody Map<String, Object> req) {
        Object rawList = req.get("blockIndexList");
        List<Integer> blockIndexList = rawList instanceof List<?> list
            ? list.stream().map(this::asInt).toList()
            : List.of();
        return blockStateService.compose(
            asLong(req.get("roleId")),
            asInt(req.get("mapId")),
            blockIndexList
        );
    }

    @PostMapping("/activate")
    public Map<String, Object> activate(@RequestBody Map<String, Object> req) {
        return blockStateService.activate(
            asLong(req.get("roleId")),
            asInt(req.get("seq"))
        );
    }

    @PostMapping("/wear")
    public Map<String, Object> wear(@RequestBody Map<String, Object> req) {
        return blockStateService.wear(
            asLong(req.get("roleId")),
            asInt(req.get("mapId"))
        );
    }

    private Integer asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
