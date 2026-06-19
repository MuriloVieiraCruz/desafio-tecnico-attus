package com.attus.legalcase.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CnjValidatorTest {

    private final CnjValidator validator = new CnjValidator();

    @Test
    void acceptsValidCnj() {
        assertThat(validator.isValid("9068906-21.2026.4.02.3738", null)).isTrue();
    }

    @Test
    void acceptsNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123",
            "9068906212026402373",
            "9068906-21.2026.4.02.373",
            "ABCDEFG-21.2026.4.02.3738"
    })
    void rejectsMalformedFormat(String value) {
        assertThat(validator.isValid(value, null)).isFalse();
    }

    @Test
    void rejectsInvalidCheckDigits() {
        assertThat(validator.isValid("9068906-22.2026.4.02.3738", null)).isFalse();
    }
}
