package com.southMillion.serverInfo_service.service;

import com.southMillion.serverInfo_service.entity.ServerInfo;
import com.southMillion.serverInfo_service.repository.ServerInfoRepository;
import org.SouthMillion.dto.serverInfor.ServerInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ServerInfoService {
    @Autowired
    private ServerInfoRepository repository;
    @Autowired
    private RedisService redisService;



    public ServerInfoDto getServerInfo() {
        ServerInfoDto cached = redisService.getServerInfo();
        if (cached != null) return cached;

        Optional<ServerInfo> opt = repository.findById(1);
        if (opt.isPresent()) {
            ServerInfoDto dto = toDto(opt.get());
            redisService.setServerInfo(dto);
            return dto;
        }
        throw new RuntimeException("Server info not found, please init database!");
    }

    public ServerInfoDto updateServerInfo(ServerInfoDto dto) {
        ServerInfo entity = repository.findById(1).orElseGet(ServerInfo::new);
        entity.setId(1);
        entity.setRealStartTime(dto.getRealStartTime());
        entity.setRealCombineTime(dto.getRealCombineTime() != null ? dto.getRealCombineTime() : 0L);
        repository.save(entity);
        redisService.evictServerInfo();
        redisService.setServerInfo(dto);
        return dto;
    }

    private ServerInfoDto toDto(ServerInfo entity) {
        ServerInfoDto dto = new ServerInfoDto();
        dto.setRealStartTime(entity.getRealStartTime());
        dto.setRealCombineTime(entity.getRealCombineTime());
        return dto;
    }

    public Long getServerCombineTime() {
        ServerInfoDto cached = redisService.getServerInfo();
        if (cached != null && cached.getRealCombineTime() != null) {
            return cached.getRealCombineTime();
        }
        Optional<ServerInfo> opt = repository.findById(1);
        if (opt.isPresent()) {
            Long combineTime = opt.get().getRealCombineTime();
            // Cache lại nếu cần
            ServerInfoDto dto = toDto(opt.get());
            redisService.setServerInfo(dto);
            return combineTime;
        }
        throw new RuntimeException("Server info not found, please init database!");
    }
}