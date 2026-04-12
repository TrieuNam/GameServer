package com.SouthMillion.block_service.repository;

import com.SouthMillion.block_service.entity.BlockNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockNodeRepository extends JpaRepository<BlockNode, Long> {
    List<BlockNode> findByRoleId(Long roleId);
    List<BlockNode> findByRoleIdAndMapId(Long roleId, Integer mapId);
    Optional<BlockNode> findByRoleIdAndMapIdAndBlockIndex(Long roleId, Integer mapId, Integer blockIndex);
    void deleteByRoleIdAndMapIdAndBlockIndex(Long roleId, Integer mapId, Integer blockIndex);
    void deleteByRoleIdAndBlockIndex(Long roleId, Integer blockIndex);
}
