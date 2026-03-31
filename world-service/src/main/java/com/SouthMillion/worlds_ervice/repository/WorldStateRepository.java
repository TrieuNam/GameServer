package com.SouthMillion.worlds_ervice.repository;

import com.SouthMillion.worlds_ervice.entity.WorldState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorldStateRepository extends JpaRepository<WorldState, Long> {
    
    Optional<WorldState> findByStateKey(String stateKey);
    
    boolean existsByStateKey(String stateKey);
}
