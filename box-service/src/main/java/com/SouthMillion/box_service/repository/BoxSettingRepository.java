package com.SouthMillion.box_service.repository;

import com.SouthMillion.box_service.enity.BoxSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoxSettingRepository extends JpaRepository<BoxSetting, String> { }