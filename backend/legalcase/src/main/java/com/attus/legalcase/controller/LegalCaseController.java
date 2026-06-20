package com.attus.legalcase.controller;

import com.attus.legalcase.controller.documentation.LegalCaseControllerDoc;
import com.attus.legalcase.domain.enums.CaseStatus;
import com.attus.legalcase.dto.*;
import com.attus.legalcase.service.LegalCaseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/legal-cases")
public class LegalCaseController implements LegalCaseControllerDoc {

    private final LegalCaseService service;

    public LegalCaseController(LegalCaseService service) {
        this.service = service;
    }

    @Override
    @PostMapping
    public ResponseEntity<LegalCaseResponse> create(@Valid @RequestBody CreateLegalCaseRequest request) {
        LegalCaseResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Override
    @GetMapping("/{id}")
    public LegalCaseResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Override
    @GetMapping
    public PagedModel<LegalCaseResponse> search(
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) String party,
            @RequestParam(required = false) String court,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate filingDateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate filingDateTo,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        LegalCaseFilter filter = new LegalCaseFilter(status, party, court, filingDateFrom, filingDateTo);
        Page<LegalCaseResponse> page = service.search(filter, pageable);
        return new PagedModel<>(page);
    }

    @Override
    @PutMapping("/{id}")
    public LegalCaseResponse update(@PathVariable Long id, @Valid @RequestBody UpdateLegalCaseRequest request) {
        return service.update(id, request);
    }

    @Override
    @PatchMapping("/{id}/status")
    public LegalCaseResponse changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return service.changeStatus(id, request.newStatus());
    }

    @Override
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
