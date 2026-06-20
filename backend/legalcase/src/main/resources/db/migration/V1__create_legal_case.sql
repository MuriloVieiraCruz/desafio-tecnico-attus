CREATE TABLE legal_case (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cnj_number        VARCHAR(25)  NOT NULL,
    plaintiff         VARCHAR(255) NOT NULL,
    defendant         VARCHAR(255) NOT NULL,
    court             VARCHAR(255) NOT NULL,
    judicial_district VARCHAR(255),
    claim_value       NUMERIC(15, 2),
    filing_date       DATE,
    status            VARCHAR(30)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT uk_legal_case_cnj_number UNIQUE (cnj_number)
);