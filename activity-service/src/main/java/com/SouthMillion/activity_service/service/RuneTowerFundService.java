package com.SouthMillion.activity_service.service;

import com.SouthMillion.activity_service.entity.RuneTowerFund;
import com.SouthMillion.activity_service.repository.RuneTowerFundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.QueryTimeoutException;
import java.util.List;

@Service
public class RuneTowerFundService {

    private final RuneTowerFundRepository fundRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public RuneTowerFundService(RuneTowerFundRepository fundRepository) {
        this.fundRepository = fundRepository;
    }

    @Transactional
    public void saveAllFundsInBatch(List<RuneTowerFund> funds) {
        try {
            for (int i = 0; i < funds.size(); i++) {
                entityManager.persist(funds.get(i));
                if (i % 50 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
            entityManager.flush();
            entityManager.clear();
        } catch (QueryTimeoutException e) {
            entityManager.getTransaction().rollback();
            throw e;
        } catch (Exception ex) {
            entityManager.getTransaction().rollback();
            throw ex;
        }
    }
}