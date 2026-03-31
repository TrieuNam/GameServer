package com.SouthMillion.scroll_service.repository;
import com.SouthMillion.scroll_service.entity.ScrollMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ScrollMetaRepository extends JpaRepository<ScrollMeta, Long> {
    Optional<ScrollMeta> findByRoleId(Long roleId);
}
