package com.attus.legalcase.dto;

import com.attus.legalcase.domain.enums.CaseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LegalCaseResponse(
        Long id,
        String cnjNumber,
        String plaintiff,
        String defendant,
        String court,
        String judicialDistrict,
        BigDecimal claimValue,
        LocalDate filingDate,
        CaseStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
