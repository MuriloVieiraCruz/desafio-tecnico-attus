package com.attus.legalcase.mapper;

import com.attus.legalcase.domain.LegalCase;
import com.attus.legalcase.dto.CreateLegalCaseRequest;
import com.attus.legalcase.dto.LegalCaseResponse;
import com.attus.legalcase.dto.UpdateLegalCaseRequest;
import org.springframework.stereotype.Component;

@Component
public class LegalCaseMapper {

    public LegalCase toEntity(CreateLegalCaseRequest r) {
        return new LegalCase(r.cnjNumber(), r.plaintiff(), r.defendant(),
                r.court(), r.judicialDistrict(), r.claimValue(), r.filingDate());
    }

    public void applyUpdate(LegalCase entity, UpdateLegalCaseRequest r) {
        entity.setPlaintiff(r.plaintiff());
        entity.setDefendant(r.defendant());
        entity.setCourt(r.court());
        entity.setJudicialDistrict(r.judicialDistrict());
        entity.setClaimValue(r.claimValue());
        entity.setFilingDate(r.filingDate());
    }

    public LegalCaseResponse toResponse(LegalCase e) {
        return new LegalCaseResponse(e.getId(), e.getCnjNumber(), e.getPlaintiff(),
                e.getDefendant(), e.getCourt(), e.getJudicialDistrict(), e.getClaimValue(),
                e.getFilingDate(), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
