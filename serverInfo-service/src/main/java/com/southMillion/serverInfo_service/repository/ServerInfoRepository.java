package com.southMillion.serverInfo_service.repository;

import com.southMillion.serverInfo_service.entity.ServerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerInfoRepository extends JpaRepository<ServerInfo, Integer> {
}