package com.SouthMillion.serverInfo_service.repository;

import com.SouthMillion.serverInfo_service.entity.ServerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerInfoRepository extends JpaRepository<ServerInfo, Integer> {
}