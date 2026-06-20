package com.attus.legalcase.controller;

import com.attus.legalcase.domain.enums.CaseStatus;
import com.attus.legalcase.dto.LegalCaseResponse;
import com.attus.legalcase.exception.DuplicateCnjException;
import com.attus.legalcase.exception.InvalidStatusTransitionException;
import com.attus.legalcase.exception.LegalCaseNotFoundException;
import com.attus.legalcase.service.LegalCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LegalCaseController.class)
class LegalCaseControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private LegalCaseService service;

    private LegalCaseResponse sample() {
        return new LegalCaseResponse(1L, "9068906-21.2026.4.02.3738",
                "Município de São Paulo", "Empresa XYZ", "1ª Vara",
                null, null, null, CaseStatus.FILED, null, null);
    }

    @Test
    void create_validRequest_returns201WithLocation() throws Exception {
        when(service.create(any())).thenReturn(sample());

        String body = """
            { "cnjNumber": "9068906-21.2026.4.02.3738", "plaintiff": "Município de São Paulo",
              "defendant": "Empresa XYZ", "court": "1ª Vara" }""";

        mockMvc.perform(post("/api/v1/legal-cases").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("FILED"));
    }

    @Test
    void create_invalidCnj_returns400WithFieldError() throws Exception {
        String body = """
            { "cnjNumber": "123", "plaintiff": "P", "defendant": "D", "court": "C" }""";

        mockMvc.perform(post("/api/v1/legal-cases").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem:validation-error"))
                .andExpect(jsonPath("$.errors.cnjNumber").exists());

        verify(service, never()).create(any());
    }

    @Test
    void create_blankRequiredField_returns400() throws Exception {
        String body = """
            { "cnjNumber": "9068906-21.2026.4.02.3738", "plaintiff": "", "defendant": "D", "court": "C" }""";

        mockMvc.perform(post("/api/v1/legal-cases").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.plaintiff").exists());
    }

    @Test
    void create_duplicateCnj_returns409() throws Exception {
        when(service.create(any())).thenThrow(new DuplicateCnjException("9068906-21.2026.4.02.3738"));

        String body = """
            { "cnjNumber": "9068906-21.2026.4.02.3738", "plaintiff": "P", "defendant": "D", "court": "C" }""";

        mockMvc.perform(post("/api/v1/legal-cases").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem:duplicate-cnj"));
    }

    @Test
    void findById_whenMissing_returns404() throws Exception {
        when(service.findById(99L)).thenThrow(new LegalCaseNotFoundException(99L));

        mockMvc.perform(get("/api/v1/legal-cases/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem:legal-case-not-found"));
    }

    @Test
    void search_returnsPagedModel() throws Exception {
        Page<LegalCaseResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 20), 1);
        when(service.search(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/legal-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void changeStatus_invalidTransition_returns422() throws Exception {
        when(service.changeStatus(eq(1L), eq(CaseStatus.IN_PROGRESS)))
                .thenThrow(new InvalidStatusTransitionException(CaseStatus.CLOSED, CaseStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/v1/legal-cases/1/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"newStatus\": \"IN_PROGRESS\" }"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem:invalid-status-transition"));
    }

    @Test
    void changeStatus_invalidEnumValue_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/legal-cases/1/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"newStatus\": \"FOO\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/legal-cases/1"))
                .andExpect(status().isNoContent());

        verify(service).delete(1L);
    }
}
