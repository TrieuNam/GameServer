package com.SouthMillion.globalserver_service.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.SouthMillion.dto.ItemDTO;
import org.SouthMillion.dto.globalserver.MailDTO;
import org.SouthMillion.utils.JsonUtil;

import java.util.List;

@Entity
@Table(name = "mail")
@Getter
@Setter
public class MailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Integer mailType;
    private Integer mailIndex;
    private Long recvTime;
    private Integer isRead;
    private Integer isFetch;
    private String subject;
    private String content;
    private String itemDataJson; // List<ItemDTO> dạng JSON

    public static MailDTO fromEntity(MailEntity entity) {
        MailDTO dto = new MailDTO();
        dto.setMailType(entity.getMailType());
        dto.setMailIndex(entity.getMailIndex());
        dto.setRecvTime(entity.getRecvTime());
        dto.setIsRead(entity.getIsRead());
        dto.setIsFetch(entity.getIsFetch());
        dto.setSubject(entity.getSubject());
        dto.setContent(entity.getContent());
        dto.setItemData(JsonUtil.fromJsonList(entity.getItemDataJson(), ItemDTO.class));
        return dto;
    }

    public static MailEntity toEntity(MailDTO dto, Long userId) {
       MailEntity entity = new MailEntity();
        entity.setUserId(userId);
        entity.setMailType(dto.getMailType());
        entity.setMailIndex(dto.getMailIndex());
        entity.setRecvTime(dto.getRecvTime());
        entity.setIsRead(dto.getIsRead());
        entity.setIsFetch(dto.getIsFetch());
        entity.setSubject(dto.getSubject());
        entity.setContent(dto.getContent());
        entity.setItemDataJson(JsonUtil.toJson(dto.getItemData()));
        return entity;
    }
}
