package com.attus.legalcase.domain;

import com.attus.legalcase.domain.enums.CaseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.attus.legalcase.domain.enums.CaseStatus.ARCHIVED;
import static com.attus.legalcase.domain.enums.CaseStatus.CLOSED;
import static org.assertj.core.api.Assertions.assertThat;

class CaseStatusTest {

    @ParameterizedTest
    @CsvSource({
            "FILED, IN_PROGRESS",
            "IN_PROGRESS, SUSPENDED",
            "IN_PROGRESS, ARCHIVED",
            "IN_PROGRESS, CLOSED",
            "SUSPENDED, IN_PROGRESS"
    })
    void allowsValidTransitions(CaseStatus from, CaseStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "FILED, CLOSED",
            "FILED, ARCHIVED",
            "FILED, SUSPENDED",
            "IN_PROGRESS, FILED",
            "ARCHIVED, IN_PROGRESS",
            "CLOSED, IN_PROGRESS",
            "IN_PROGRESS, IN_PROGRESS"
    })
    void rejectsInvalidTransitions(CaseStatus from, CaseStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void terminalStatesHaveNoTransitions() {
        for (CaseStatus target : CaseStatus.values()) {
            assertThat(ARCHIVED.canTransitionTo(target)).isFalse();
            assertThat(CLOSED.canTransitionTo(target)).isFalse();
        }
    }
}
