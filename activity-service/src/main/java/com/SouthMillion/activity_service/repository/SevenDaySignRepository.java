package com.SouthMillion.activity_service.repository;
import com.SouthMillion.activity_service.entity.SevenDaySign;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SevenDaySignRepository extends JpaRepository<SevenDaySign, Long> {
    Optional<SevenDaySign> findByRoleId(Long roleId);
}
