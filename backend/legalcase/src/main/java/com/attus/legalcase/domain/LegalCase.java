package com.attus.legalcase.domain;

import com.attus.legalcase.domain.enums.CaseStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "legal_case")
@Getter
@Setter
@SQLDelete(sql = "UPDATE legal_case SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "cnj_number", nullable = false, unique = true, length = 25)
    private String cnjNumber;

    @Column(nullable = false)
    private String plaintiff;

    @Column(nullable = false)
    private String defendant;

    @Column(nullable = false)
    private String court;

    @Column(name = "judicial_district")
    private String judicialDistrict;

    @Column(name = "claim_value", precision = 15, scale = 2)
    private BigDecimal claimValue;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CaseStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Setter(AccessLevel.NONE)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    @Setter(AccessLevel.NONE)
    private LocalDateTime deletedAt;
}
