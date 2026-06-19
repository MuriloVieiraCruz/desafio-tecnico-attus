package com.attus.legalcase.repository;

import com.attus.legalcase.domain.LegalCase;
import com.attus.legalcase.domain.enums.CaseStatus;
import com.attus.legalcase.dto.LegalCaseFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LegalCaseRepositoryTest {

    private static final String CNJ = "9068906-21.2026.4.02.3738";

    @Autowired private LegalCaseRepository repository;
    @Autowired private TestEntityManager entityManager;

    private LegalCase newCase() {
        return new LegalCase(LegalCaseRepositoryTest.CNJ, "Plaintiff", "Defendant", "Court", null, null, null);
    }

    @Test
    void existsByCnjNumber_returnsTrueForActiveCase() {
        repository.save(newCase());
        assertThat(repository.existsByCnjNumber(CNJ)).isTrue();
    }

    @Test
    void softDelete_hidesCaseFromQueries() {
        LegalCase saved = repository.save(newCase());
        repository.delete(saved);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(saved.getId())).isEmpty();
        assertThat(repository.existsByCnjNumber(CNJ)).isFalse();
    }

    @Test
    void uniqueConstraint_blocksDuplicateEvenAfterSoftDelete() {
        LegalCase saved = repository.save(newCase());
        repository.delete(saved);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.existsByCnjNumber(CNJ)).isFalse();
        assertThatThrownBy(() -> repository.saveAndFlush(newCase()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void specification_filtersByStatus() {
        repository.save(newCase());
        entityManager.flush();

        Page<LegalCase> filed = repository.findAll(
                LegalCaseSpecifications.withFilter(new LegalCaseFilter(CaseStatus.FILED, null, null, null, null)),
                PageRequest.of(0, 10));
        Page<LegalCase> closed = repository.findAll(
                LegalCaseSpecifications.withFilter(new LegalCaseFilter(CaseStatus.CLOSED, null, null, null, null)),
                PageRequest.of(0, 10));

        assertThat(filed.getTotalElements()).isEqualTo(1);
        assertThat(closed.getTotalElements()).isZero();
    }
}
