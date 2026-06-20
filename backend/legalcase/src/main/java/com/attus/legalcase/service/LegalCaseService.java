package com.attus.legalcase.service;

import com.attus.legalcase.domain.LegalCase;
import com.attus.legalcase.domain.enums.CaseStatus;
import com.attus.legalcase.dto.CreateLegalCaseRequest;
import com.attus.legalcase.dto.LegalCaseFilter;
import com.attus.legalcase.dto.LegalCaseResponse;
import com.attus.legalcase.dto.UpdateLegalCaseRequest;
import com.attus.legalcase.exception.DuplicateCnjException;
import com.attus.legalcase.exception.InvalidStatusTransitionException;
import com.attus.legalcase.exception.LegalCaseNotFoundException;
import com.attus.legalcase.mapper.LegalCaseMapper;
import com.attus.legalcase.repository.LegalCaseRepository;
import com.attus.legalcase.repository.LegalCaseSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegalCaseService {

    private static final Logger log = LoggerFactory.getLogger(LegalCaseService.class);

    private final LegalCaseRepository repository;
    private final LegalCaseMapper mapper;

    public LegalCaseService(LegalCaseRepository repository, LegalCaseMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public LegalCaseResponse create(CreateLegalCaseRequest request) {
        if (repository.existsByCnjNumber(request.cnjNumber())) {
            throw new DuplicateCnjException(request.cnjNumber());
        }

        LegalCase entity = mapper.toEntity(request);

        try {
            LegalCase saved = repository.saveAndFlush(entity);
            log.info("Legal case created id={} cnj={}", saved.getId(), saved.getCnjNumber());
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Concurrent duplicate CNJ detected cnj={}", request.cnjNumber());
            throw new DuplicateCnjException(request.cnjNumber(), ex);
        }
    }

    @Transactional(readOnly = true)
    public LegalCaseResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<LegalCaseResponse> search(LegalCaseFilter filter, Pageable pageable) {
        return repository.findAll(LegalCaseSpecifications.withFilter(filter), pageable)
                .map(mapper::toResponse);
    }

    @Transactional
    public LegalCaseResponse update(Long id, UpdateLegalCaseRequest request) {
        LegalCase entity = getOrThrow(id);
        mapper.applyUpdate(entity, request);
        log.info("Legal case updated id={}", id);
        return mapper.toResponse(entity);
    }

    @Transactional
    public LegalCaseResponse changeStatus(Long id, CaseStatus newStatus) {
        LegalCase entity = getOrThrow(id);
        CaseStatus current = entity.getStatus();

        if (!current.canTransitionTo(newStatus)) {
            log.warn("Invalid status transition id={} from={} to={}", id, current, newStatus);
            throw new InvalidStatusTransitionException(current, newStatus);
        }

        entity.setStatus(newStatus);
        log.info("Legal case status changed id={} from={} to={}", id, current, newStatus);
        return mapper.toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        LegalCase entity = getOrThrow(id);
        repository.delete(entity);
        log.info("Legal case soft-deleted id={}", id);
    }

    private LegalCase getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new LegalCaseNotFoundException(id));
    }
}
