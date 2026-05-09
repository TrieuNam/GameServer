package com.SouthMillion.activity_service.dto;

import com.SouthMillion.activity_service.entity.LoopMine;
import org.springframework.data.domain.Page;

import java.util.List;

public class BattleLogResponse {

    private List<LoopMine> logs;
    private int totalPages;
    private long totalElements;

    public BattleLogResponse(Page<LoopMine> pageData) {
        this.logs = pageData.getContent();
        this.totalPages = pageData.getTotalPages();
        this.totalElements = pageData.getTotalElements();
    }

    public List<LoopMine> getLogs() {
        return logs;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }
}