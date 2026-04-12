package com.SouthMillion.block_service.service;

import com.SouthMillion.block_service.entity.BlockNode;
import com.SouthMillion.block_service.entity.RoleBlockState;
import com.SouthMillion.block_service.repository.BlockNodeRepository;
import com.SouthMillion.block_service.repository.RoleBlockStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockStateService {

    private static final int RET_INIT = 0;
    private static final int RET_INLAY = 3;
    private static final int RET_REMOVE = 4;
    private static final int RET_ACTIVATE = 5;
    private static final int RET_MAP_WEAR = 6;
    private static final int RET_COMPOSE = 7;
    private static final int RET_COMPOSE_FAIL = 8;

    private final RoleBlockStateRepository stateRepo;
    private final BlockNodeRepository blockNodeRepo;

    public Map<String, Object> info(Long roleId) {
        RoleBlockState state = getOrCreateState(roleId);
        List<BlockNode> blocks = blockNodeRepo.findByRoleId(roleId);
        return toResponse(state, blocks, RET_INIT);
    }

    @Transactional
    public Map<String, Object> inlay(Long roleId, Integer mapId, Integer blockIndex, Integer posX, Integer posY) {
        RoleBlockState state = getOrCreateState(roleId);
        List<BlockNode> blocks = blockNodeRepo.findByRoleId(roleId);
        
        int index = blockIndex != null && blockIndex > 0 ? blockIndex : state.getNextBlockIndex();
        Optional<BlockNode> existing = blocks.stream()
            .filter(node -> Objects.equals(node.getBlockIndex(), index))
            .findFirst();

        BlockNode node;
        if (existing.isPresent()) {
            node = existing.get();
        } else {
            node = BlockNode.builder()
                .roleId(roleId)
                .blockId(1000 + index)
                .mapId(n(mapId))
                .blockIndex(index)
                .posX(n(posX))
                .posY(n(posY))
                .color((index % 5) + 1)
                .build();
            blockNodeRepo.save(node);
            state.setNextBlockIndex(index + 1);
        }

        node.setMapId(n(mapId));
        node.setPosX(n(posX));
        node.setPosY(n(posY));
        blockNodeRepo.save(node);

        stateRepo.save(state);
        
        blocks = blockNodeRepo.findByRoleId(roleId);
        return toResponse(state, blocks, RET_INLAY);
    }

    @Transactional
    public Map<String, Object> remove(Long roleId, Integer mapId, Integer blockIndex) {
        RoleBlockState state = getOrCreateState(roleId);
        blockNodeRepo.findByRoleIdAndMapIdAndBlockIndex(roleId, n(mapId), n(blockIndex))
            .ifPresent(node -> {
                node.setMapId(0);
                node.setPosX(0);
                node.setPosY(0);
                blockNodeRepo.save(node);
            });

        List<BlockNode> blocks = blockNodeRepo.findByRoleId(roleId);
        return toResponse(state, blocks, RET_REMOVE);
    }

    @Transactional
    public Map<String, Object> compose(Long roleId, Integer mapId, List<Integer> blockIndexList) {
        RoleBlockState state = getOrCreateState(roleId);
        List<BlockNode> blocks = blockNodeRepo.findByRoleId(roleId);
        
        List<Integer> indexes = blockIndexList == null ? List.of() 
            : blockIndexList.stream().filter(Objects::nonNull).collect(Collectors.toList());
        
        long matches = blocks.stream().filter(node -> indexes.contains(node.getBlockIndex())).count();
        
        if (indexes.size() != 3 || matches != 3) {
            return toResponse(state, blocks, RET_COMPOSE_FAIL);
        }

        // Remove consumed blocks (delete by roleId + blockIndex, regardless of mapId)
        indexes.forEach(idx -> blockNodeRepo.deleteByRoleIdAndBlockIndex(roleId, idx));

        // Create composed block (uninstalled, sitting in bag)
        int newIndex = state.getNextBlockIndex();
        BlockNode composed = BlockNode.builder()
            .roleId(roleId)
            .blockId(2000 + newIndex)
            .mapId(0)
            .blockIndex(newIndex)
            .posX(0)
            .posY(0)
            .color(9)
            .build();
        blockNodeRepo.save(composed);

        state.setNextBlockIndex(newIndex + 1);
        stateRepo.save(state);

        // Return only the newly composed block so client can push it without duplicating existing blocks
        return toResponse(state, List.of(composed), RET_COMPOSE);
    }

    @Transactional
    public Map<String, Object> activate(Long roleId, Integer seq) {
        RoleBlockState state = getOrCreateState(roleId);
        state.setActivateSeq(n(seq));
        stateRepo.save(state);
        
        List<BlockNode> blocks = blockNodeRepo.findByRoleId(roleId);
        return toResponse(state, blocks, RET_ACTIVATE);
    }

    @Transactional
    public Map<String, Object> wear(Long roleId, Integer mapId) {
        RoleBlockState state = getOrCreateState(roleId);
        state.setWearingMapId(n(mapId));
        stateRepo.save(state);
        
        List<BlockNode> blocks = blockNodeRepo.findByRoleId(roleId);
        return toResponse(state, blocks, RET_MAP_WEAR);
    }

    private RoleBlockState getOrCreateState(Long roleId) {
        return stateRepo.findByRoleId(roleId).orElseGet(() -> {
            RoleBlockState state = RoleBlockState.builder()
                .roleId(roleId)
                .activateSeq(0)
                .wearingMapId(0)
                .nextBlockIndex(1)
                .build();
            return stateRepo.save(state);
        });
    }

    private Map<String, Object> toResponse(RoleBlockState state, List<BlockNode> blocks, int sendType) {
        Map<String, Object> result = new HashMap<>();
        result.put("sendType", sendType);
        result.put("activateSeq", state.getActivateSeq());
        result.put("mapId", state.getWearingMapId());
        
        List<Map<String, Object>> blockList = blocks.stream()
            .sorted(Comparator.comparingInt(BlockNode::getBlockIndex))
            .map(this::toBlockMap)
            .collect(Collectors.toList());
        
        result.put("blockList", blockList);
        return result;
    }

    private Map<String, Object> toBlockMap(BlockNode node) {
        Map<String, Object> data = new HashMap<>();
        data.put("blockId", node.getBlockId());
        data.put("mapId", node.getMapId());
        data.put("posX", node.getPosX());
        data.put("posY", node.getPosY());
        data.put("color", node.getColor());
        data.put("blockIndex", node.getBlockIndex());
        return data;
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }
}
