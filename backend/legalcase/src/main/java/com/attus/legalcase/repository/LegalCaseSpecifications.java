package com.attus.legalcase.repository;

import com.attus.legalcase.domain.LegalCase;
import com.attus.legalcase.domain.enums.CaseStatus;
import com.attus.legalcase.dto.LegalCaseFilter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

public final class LegalCaseSpecifications {

    private LegalCaseSpecifications() {}

    public static Specification<LegalCase> withFilter(LegalCaseFilter f) {
        return Specification.allOf(
                hasStatus(f.status()),
                hasCourt(f.court()),
                partyContains(f.party()),
                filedFrom(f.filingDateFrom()),
                filedTo(f.filingDateTo())
        );
    }

    private static Specification<LegalCase> hasStatus(CaseStatus status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<LegalCase> hasCourt(String court) {
        if (!StringUtils.hasText(court)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("court")), court.toLowerCase());
    }

    private static Specification<LegalCase> partyContains(String term) {
        if (!StringUtils.hasText(term)) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            String like = "%" + term.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("plaintiff")), like),
                    cb.like(cb.lower(root.get("defendant")), like)
            );
        };
    }

    private static Specification<LegalCase> filedFrom(LocalDate from) {
        if (from == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("filingDate"), from);
    }

    private static Specification<LegalCase> filedTo(LocalDate to) {
        if (to == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("filingDate"), to);
    }
}
