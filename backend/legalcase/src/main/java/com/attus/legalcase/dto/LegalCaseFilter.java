package com.attus.legalcase.dto;

import com.attus.legalcase.domain.enums.CaseStatus;

import java.time.LocalDate;

public record LegalCaseFilter(
        CaseStatus status,
        String party,
        String court,
        LocalDate filingDateFrom,
        LocalDate filingDateTo
) {}
