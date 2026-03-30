package com.SouthMillion.activity_service.repository;
import com.SouthMillion.activity_service.entity.LuckUnpacking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface LuckUnpackingRepository extends JpaRepository<LuckUnpacking, Long> {
    Optional<LuckUnpacking> findByRoleId(Long roleId);
}
