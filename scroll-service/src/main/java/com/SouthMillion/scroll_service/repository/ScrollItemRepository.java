package com.SouthMillion.scroll_service.repository;
import com.SouthMillion.scroll_service.entity.ScrollItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ScrollItemRepository extends JpaRepository<ScrollItem, Long> {
    List<ScrollItem> findByRoleId(Long roleId);
}
