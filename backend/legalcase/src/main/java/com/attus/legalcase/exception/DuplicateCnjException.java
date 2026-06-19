package com.attus.legalcase.exception;

public class DuplicateCnjException extends RuntimeException {
    public DuplicateCnjException(String cnj) {
        super("A legal case with CNJ " + cnj + " already exists");
    }
    public DuplicateCnjException(String cnj, Throwable cause) {
        super("A legal case with CNJ " + cnj + " already exists", cause);
    }
}
