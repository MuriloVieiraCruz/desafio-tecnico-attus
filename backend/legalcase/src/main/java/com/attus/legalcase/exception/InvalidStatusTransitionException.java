package com.attus.legalcase.exception;

import com.attus.legalcase.domain.enums.CaseStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(CaseStatus from, CaseStatus to) {
        super("Invalid status transition from " + from + " to " + to);
    }
}
