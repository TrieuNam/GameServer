package com.SouthMillion.box_service.service;


import org.SouthMillion.dto.box.BoxDTOs;
import org.springframework.stereotype.Service;

@Service
public interface BoxInfoServiceImpl {
    BoxDTOs.EquipInfo getEquipInfo(String roleId);
}