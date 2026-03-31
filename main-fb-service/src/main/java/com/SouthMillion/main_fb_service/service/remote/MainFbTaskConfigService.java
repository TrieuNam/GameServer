package com.SouthMillion.main_fb_service.service.remote;

import jakarta.annotation.PostConstruct;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MainFbTaskConfigService {

    private final RemoteMaoxianConfigService cfg;

    private volatile List<TaskCfg> tasks = List.of();

    @PostConstruct
    public void reload() { rebuildFromMaoxian(); }

    public synchronized void rebuildFromMaoxian() {
        var conf = cfg.getConfig();
        var list = conf.getClearance();
        if (list == null) { tasks = List.of(); return; }

        List<TaskCfg> built = list.stream()
                .map(c -> TaskCfg.builder()
                        .stage(safeInt(c.getStage()))
                        .level(safeInt(c.getLevel()))
                        .threeStarScore(safeInt(c.getScore()))            // điểm 3 sao (chỉ để hiển thị)
                        .rewardPreview(c.getWin())                        // chỉ preview, KHÔNG phát lại
                        .build())
                .sorted(Comparator
                        .comparingInt(TaskCfg::getStage)
                        .thenComparingInt(TaskCfg::getLevel))
                .collect(Collectors.toList());

        int i = 1;
        for (TaskCfg t : built) t.setIndex(i++);
        tasks = List.copyOf(built);
    }

    public int totalCount() { return tasks.size(); }

    public int firstIndex() { return tasks.isEmpty() ? 1 : 1; }

    public Optional<TaskCfg> byIndex(int index) {
        if (index <= 0 || index > tasks.size()) return Optional.empty();
        return Optional.of(tasks.get(index - 1));
    }

    /** Trả về index (1-based) của (stage,level) trong tuyến nhiệm vụ; -1 nếu không thấy. */
    public int indexOf(int stage, int level) {
        for (TaskCfg t : tasks) {
            if (t.getStage() == stage && t.getLevel() == level) return t.getIndex();
        }
        return -1;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskCfg {
        private int index;
        private int stage;
        private int level;
        private int threeStarScore;                 // dùng để hiển thị, quét 3 sao
        private List<MaoxianConfig.Drop> rewardPreview; // chỉ preview
    }

    private static int safeInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }
}