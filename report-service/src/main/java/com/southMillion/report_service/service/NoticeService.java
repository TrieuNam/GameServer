package com.southMillion.report_service.service;

import com.southMillion.report_service.entity.NoticeEntity;
import com.southMillion.report_service.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.report.NoticeDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    public NoticeDTO createNotice(NoticeDTO dto) {
        NoticeEntity entity = NoticeEntity.builder()
                .type(dto.getType())
                .content(dto.getContent())
                .code(dto.getCode())
                .itemId(dto.getItemId())
                .createdTime(System.currentTimeMillis())
                .build();
        entity = noticeRepository.save(entity);
        dto.setId(entity.getId());
        dto.setCreatedTime(entity.getCreatedTime());
        return dto;
    }

    public List<NoticeDTO> getAllNotice() {
        return noticeRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    private NoticeDTO toDTO(NoticeEntity entity) {
        return NoticeDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .content(entity.getContent())
                .code(entity.getCode())
                .itemId(entity.getItemId())
                .createdTime(entity.getCreatedTime())
                .build();
    }

    // Thêm các method tìm kiếm theo type, itemId, code...
}