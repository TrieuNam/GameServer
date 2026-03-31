package com.SouthMillion.admin.repository;

import com.SouthMillion.admin.entity.ServiceConfig;
import com.SouthMillion.admin.entity.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Service Configuration Repository
 */
@Repository
public interface ServiceConfigRepository extends JpaRepository<ServiceConfig, Long> {

    /**
     * Find service by name
     */
    Optional<ServiceConfig> findByServiceName(String serviceName);

    /**
     * Find all enabled services
     */
    List<ServiceConfig> findByEnabledTrue();

    /**
     * Find services by phase
     */
    List<ServiceConfig> findByPhaseOrderByStartupOrder(String phase);

    /**
     * Find services by status
     */
    List<ServiceConfig> findByStatus(ServiceStatus status);

    /**
     * Find services with auto-start enabled
     */
    List<ServiceConfig> findByAutoStartTrueOrderByStartupOrder();

    /**
     * Find all services ordered by startup order
     */
    @Query("SELECT s FROM ServiceConfig s ORDER BY s.startupOrder, s.serviceName")
    List<ServiceConfig> findAllByStartupOrder();

    /**
     * Check if service exists by name
     */
    boolean existsByServiceName(String serviceName);
}
