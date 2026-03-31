package com.SouthMillion.activity_service.repository;
import com.SouthMillion.activity_service.entity.NewAreaPreferential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface NewAreaPreferentialRepository extends JpaRepository<NewAreaPreferential, Long> {
    Optional<NewAreaPreferential> findByRoleId(Long roleId);
}
