package com.attus.legalcase.domain.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum CaseStatus {

    FILED,        // Distribuído
    IN_PROGRESS,  // Em andamento
    SUSPENDED,    // Suspenso
    ARCHIVED,     // Arquivado
    CLOSED;       // Baixado

    private static final Map<CaseStatus, Set<CaseStatus>> ALLOWED_TRANSITIONS = Map.of(
            FILED,       EnumSet.of(IN_PROGRESS),
            IN_PROGRESS, EnumSet.of(SUSPENDED, ARCHIVED, CLOSED),
            SUSPENDED,   EnumSet.of(IN_PROGRESS),
            ARCHIVED,    EnumSet.noneOf(CaseStatus.class),
            CLOSED,      EnumSet.noneOf(CaseStatus.class)
    );

    public boolean canTransitionTo(CaseStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(CaseStatus.class))
                .contains(target);
    }
}
