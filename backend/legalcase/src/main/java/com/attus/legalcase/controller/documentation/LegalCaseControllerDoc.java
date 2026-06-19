package com.attus.legalcase.controller.documentation;

import com.attus.legalcase.domain.enums.CaseStatus;
import com.attus.legalcase.dto.CreateLegalCaseRequest;
import com.attus.legalcase.dto.LegalCaseResponse;
import com.attus.legalcase.dto.UpdateLegalCaseRequest;
import com.attus.legalcase.dto.UpdateStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

@Tag(name = "Legal Cases", description = "Management of legal cases for digital attorney services.")
public interface LegalCaseControllerDoc {

    @Operation(summary = "Create a legal case",
            description = "Registers a new legal case. The CNJ number must be valid (format and check digits) "
                    + "and unique. The case is created with status FILED.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Legal case created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LegalCaseResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "cnjNumber": "0001234-56.2024.8.26.0100",
                                      "plaintiff": "Município de São Paulo",
                                      "defendant": "Empresa XYZ Ltda",
                                      "court": "1ª Vara da Fazenda Pública",
                                      "judicialDistrict": "São Paulo",
                                      "claimValue": 150000.00,
                                      "filingDate": "2024-03-15",
                                      "status": "FILED",
                                      "createdAt": "2026-06-19T10:30:00",
                                      "updatedAt": "2026-06-19T10:30:00"
                                    }"""))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "type": "urn:problem:validation-error",
                                      "title": "Validation error",
                                      "status": 400,
                                      "detail": "One or more fields are invalid",
                                      "correlationId": "9f1c2b3a-4d5e-6f70-8190-a1b2c3d4e5f6",
                                      "errors": {
                                        "cnjNumber": "invalid CNJ number",
                                        "plaintiff": "must not be blank"
                                      }
                                    }"""))),
            @ApiResponse(responseCode = "409", description = "A legal case with the same CNJ already exists",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "type": "urn:problem:duplicate-cnj",
                                      "title": "Duplicate CNJ",
                                      "status": 409,
                                      "detail": "A legal case with CNJ 0001234-56.2024.8.26.0100 already exists",
                                      "correlationId": "9f1c2b3a-4d5e-6f70-8190-a1b2c3d4e5f6"
                                    }""")))
    })
    ResponseEntity<LegalCaseResponse> create(
            @RequestBody(description = "Data for the new legal case", required = true,
                    content = @Content(schema = @Schema(implementation = CreateLegalCaseRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "cnjNumber": "0001234-56.2024.8.26.0100",
                                      "plaintiff": "Município de São Paulo",
                                      "defendant": "Empresa XYZ Ltda",
                                      "court": "1ª Vara da Fazenda Pública",
                                      "judicialDistrict": "São Paulo",
                                      "claimValue": 150000.00,
                                      "filingDate": "2024-03-15"
                                    }""")))
            CreateLegalCaseRequest request);

    @Operation(summary = "Get a legal case by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Legal case found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LegalCaseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Legal case not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "type": "urn:problem:legal-case-not-found",
                                      "title": "Legal case not found",
                                      "status": 404,
                                      "detail": "Legal case not found: 999",
                                      "correlationId": "9f1c2b3a-4d5e-6f70-8190-a1b2c3d4e5f6"
                                    }""")))
    })
    LegalCaseResponse findById(
            @Parameter(name = "id", description = "Legal case id", required = true, in = ParameterIn.PATH, example = "1")
            Long id);

    @Operation(summary = "Search legal cases",
            description = "Paginated search with optional filters. Soft-deleted cases are never returned.")
    @ApiResponse(responseCode = "200", description = "Page of legal cases",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PagedModel.class)))
    PagedModel<LegalCaseResponse> search(
            @Parameter(description = "Filter by status", in = ParameterIn.QUERY, example = "FILED")
            CaseStatus status,
            @Parameter(description = "Filter by plaintiff or defendant (partial, case-insensitive)",
                    in = ParameterIn.QUERY, example = "município")
            String party,
            @Parameter(description = "Filter by court (exact, case-insensitive)", in = ParameterIn.QUERY)
            String court,
            @Parameter(description = "Filing date from (inclusive, ISO-8601)", in = ParameterIn.QUERY, example = "2024-01-01")
            LocalDate filingDateFrom,
            @Parameter(description = "Filing date to (inclusive, ISO-8601)", in = ParameterIn.QUERY, example = "2024-12-31")
            LocalDate filingDateTo,
            @ParameterObject Pageable pageable);

    @Operation(summary = "Update legal case details",
            description = "Updates the editable fields. The CNJ number and the status are not changed here.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Legal case updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LegalCaseResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Legal case not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    LegalCaseResponse update(
            @Parameter(name = "id", description = "Legal case id", required = true, in = ParameterIn.PATH, example = "1")
            Long id,
            @RequestBody(description = "Editable legal case data", required = true,
                    content = @Content(schema = @Schema(implementation = UpdateLegalCaseRequest.class)))
            UpdateLegalCaseRequest request);

    @Operation(summary = "Change the status of a legal case",
            description = "Applies a status transition following the allowed state machine: "
                    + "FILED → IN_PROGRESS → (SUSPENDED ⇄ IN_PROGRESS) → ARCHIVED | CLOSED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LegalCaseResponse.class))),
            @ApiResponse(responseCode = "404", description = "Legal case not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Invalid status transition",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "type": "urn:problem:invalid-status-transition",
                                      "title": "Invalid status transition",
                                      "status": 422,
                                      "detail": "Invalid status transition from CLOSED to IN_PROGRESS",
                                      "correlationId": "9f1c2b3a-4d5e-6f70-8190-a1b2c3d4e5f6"
                                    }""")))
    })
    LegalCaseResponse changeStatus(
            @Parameter(name = "id", description = "Legal case id", required = true, in = ParameterIn.PATH, example = "1")
            Long id,
            @RequestBody(description = "Target status", required = true,
                    content = @Content(schema = @Schema(implementation = UpdateStatusRequest.class),
                            examples = @ExampleObject(value = "{ \"newStatus\": \"IN_PROGRESS\" }")))
            UpdateStatusRequest request);

    @Operation(summary = "Delete a legal case (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Legal case deleted",
                    content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Legal case not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    void delete(
            @Parameter(name = "id", description = "Legal case id", required = true, in = ParameterIn.PATH, example = "1")
            Long id);
}
