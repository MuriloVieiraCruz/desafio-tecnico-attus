package com.attus.legalcase.service;

import com.attus.legalcase.domain.LegalCase;
import com.attus.legalcase.domain.enums.CaseStatus;
import com.attus.legalcase.dto.*;
import com.attus.legalcase.exception.DuplicateCnjException;
import com.attus.legalcase.exception.InvalidStatusTransitionException;
import com.attus.legalcase.exception.LegalCaseNotFoundException;
import com.attus.legalcase.mapper.LegalCaseMapper;
import com.attus.legalcase.repository.LegalCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegalCaseServiceTest {

    @Mock private LegalCaseRepository repository;
    @Mock private LegalCaseMapper mapper;
    @InjectMocks private LegalCaseService service;

    private static final String CNJ = "9068906-21.2026.4.02.3738";

    private CreateLegalCaseRequest createRequest;
    private LegalCase entity;
    private LegalCaseResponse response;

    @BeforeEach
    void setUp() {
        createRequest = new CreateLegalCaseRequest(CNJ, "Plaintiff", "Defendant", "Court", null, null, null);
        entity = new LegalCase(CNJ, "Plaintiff", "Defendant", "Court", null, null, null);
        response = new LegalCaseResponse(1L, CNJ, "Plaintiff", "Defendant", "Court",
                null, null, null, CaseStatus.FILED, null, null);
    }

    @Test
    void create_persistsAndReturnsResponse() {
        when(repository.existsByCnjNumber(CNJ)).thenReturn(false);
        when(mapper.toEntity(createRequest)).thenReturn(entity);
        when(repository.saveAndFlush(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        assertThat(service.create(createRequest)).isEqualTo(response);
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void create_whenCnjAlreadyExists_throwsDuplicate() {
        when(repository.existsByCnjNumber(CNJ)).thenReturn(true);

        assertThatThrownBy(() -> service.create(createRequest))
                .isInstanceOf(DuplicateCnjException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void create_whenRaceConditionViolatesConstraint_throwsDuplicate() {
        when(repository.existsByCnjNumber(CNJ)).thenReturn(false);
        when(mapper.toEntity(createRequest)).thenReturn(entity);
        when(repository.saveAndFlush(entity))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> service.create(createRequest))
                .isInstanceOf(DuplicateCnjException.class);
    }

    @Test
    void findById_whenExists_returnsResponse() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        assertThat(service.findById(1L)).isEqualTo(response);
    }

    @Test
    void findById_whenMissing_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(LegalCaseNotFoundException.class);
    }

    @Test
    void changeStatus_validTransition_updatesStatus() {
        entity.setStatus(CaseStatus.FILED);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        service.changeStatus(1L, CaseStatus.IN_PROGRESS);

        assertThat(entity.getStatus()).isEqualTo(CaseStatus.IN_PROGRESS);
    }

    @Test
    void changeStatus_invalidTransition_throwsAndKeepsStatus() {
        entity.setStatus(CaseStatus.CLOSED);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.changeStatus(1L, CaseStatus.IN_PROGRESS))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(entity.getStatus()).isEqualTo(CaseStatus.CLOSED);
    }

    @Test
    void update_whenMissing_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        var req = new UpdateLegalCaseRequest("P", "D", "C", null, null, null);

        assertThatThrownBy(() -> service.update(99L, req))
                .isInstanceOf(LegalCaseNotFoundException.class);
    }

    @Test
    void delete_whenExists_softDeletes() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        service.delete(1L);

        verify(repository).delete(entity);
    }

    @Test
    void delete_whenMissing_throwsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(LegalCaseNotFoundException.class);
    }

    @Test
    void search_delegatesToRepositoryAndMaps() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<LegalCase> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponse(entity)).thenReturn(response);

        Page<LegalCaseResponse> result =
                service.search(new LegalCaseFilter(null, null, null, null, null), pageable);

        assertThat(result.getContent()).containsExactly(response);
    }
}
