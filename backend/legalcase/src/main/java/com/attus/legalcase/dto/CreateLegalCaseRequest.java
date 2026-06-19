package com.attus.legalcase.dto;

import com.attus.legalcase.validation.ValidCnj;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateLegalCaseRequest(
        @NotBlank @ValidCnj String cnjNumber,
        @NotBlank @Size(max = 255) String plaintiff,
        @NotBlank @Size(max = 255) String defendant,
        @NotBlank @Size(max = 255) String court,
        @Size(max = 255) String judicialDistrict,
        @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal claimValue,
        @PastOrPresent LocalDate filingDate
) {}
