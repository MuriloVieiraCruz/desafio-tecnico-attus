package com.attus.legalcase.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class CnjValidator implements ConstraintValidator<ValidCnj, String> {

    // NNNNNNN-DD.AAAA.J.TR.OOOO
    private static final Pattern CNJ_PATTERN =
            Pattern.compile("^\\d{7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // nullability handled by @NotBlank
        }
        if (!CNJ_PATTERN.matcher(value).matches()) {
            return false;
        }
        return hasValidCheckDigits(value);
    }

    private boolean hasValidCheckDigits(String formatted) {
        String sequential  = formatted.substring(0, 7);
        String checkDigits = formatted.substring(8, 10);
        String year        = formatted.substring(11, 15);
        String segment     = formatted.substring(16, 17);
        String court       = formatted.substring(18, 20);
        String origin      = formatted.substring(21, 25);

        // ISO 7064 MOD 97-10: sequential+year+segment+court+origin+checkDigits  => mod 97 == 1
        String base = sequential + year + segment + court + origin + checkDigits;
        return mod97(base) == 1;
    }

    private int mod97(String digits) {
        int mod = 0;
        for (int i = 0; i < digits.length(); i++) {
            mod = (mod * 10 + (digits.charAt(i) - '0')) % 97;
        }
        return mod;
    }
}
