package com.SouthMillion.user_service.repository;

import com.SouthMillion.user_service.model.RoleAttr;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleAttrRepository extends JpaRepository<RoleAttr, Long> {
    List<RoleAttr> findByRole_RoleId(String roleId);
}