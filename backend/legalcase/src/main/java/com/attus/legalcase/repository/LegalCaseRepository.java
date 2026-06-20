package com.attus.legalcase.repository;

import com.attus.legalcase.domain.LegalCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LegalCaseRepository extends JpaRepository<LegalCase, Long>,
        JpaSpecificationExecutor<LegalCase> {

    boolean existsByCnjNumber(String cnjNumber);
}
