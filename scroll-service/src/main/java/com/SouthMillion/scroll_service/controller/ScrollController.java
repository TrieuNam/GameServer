package com.SouthMillion.scroll_service.controller;
import com.SouthMillion.scroll_service.entity.*;
import com.SouthMillion.scroll_service.service.ScrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/scroll") @RequiredArgsConstructor
public class ScrollController {
    private final ScrollService scrollService;

    @GetMapping("/{roleId}/info")
    public ScrollMeta getInfo(@PathVariable Long roleId) {
        return scrollService.getMeta(roleId);
    }

    @GetMapping("/{roleId}/list")
    public List<ScrollItem> getList(@PathVariable Long roleId) {
        return scrollService.getList(roleId);
    }

    @PostMapping("/{roleId}/draw")
    public Map<String, Object> draw(@PathVariable Long roleId, @RequestParam(defaultValue = "1") int count) {
        return scrollService.draw(roleId, count);
    }
}
