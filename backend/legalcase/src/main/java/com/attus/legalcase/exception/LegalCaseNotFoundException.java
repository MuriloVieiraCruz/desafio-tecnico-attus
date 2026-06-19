package com.attus.legalcase.exception;

public class LegalCaseNotFoundException extends RuntimeException {
    public LegalCaseNotFoundException(Long id) {
        super("Legal case not found: " + id);
    }
}
