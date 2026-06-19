package com.attus.legalcase.dto;

import com.attus.legalcase.domain.enums.CaseStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull CaseStatus newStatus) {}
