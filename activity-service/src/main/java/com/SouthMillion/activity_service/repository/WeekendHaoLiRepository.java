package com.SouthMillion.activity_service.repository;

import com.SouthMillion.activity_service.entity.WeekendHaoLi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.Optional;

@Repository
class WeekendHaoLiRepositoryImpl {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<WeekendHaoLi> findByRoleId(Long roleId) {
        String queryStr = "SELECT w FROM WeekendHaoLi w WHERE w.roleId = :roleId";
        TypedQuery<WeekendHaoLi> query = entityManager.createQuery(queryStr, WeekendHaoLi.class);
        query.setParameter("roleId", roleId);
        return query.getResultStream().findFirst();
    }
}