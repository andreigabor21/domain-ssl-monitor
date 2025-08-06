package com.media.sslmonitor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.sslmonitor.dto.DomainCheckRequest;
import com.media.sslmonitor.dto.DomainCheckResponse;
import com.media.sslmonitor.entity.CertificateCheck;
import com.media.sslmonitor.entity.Domain;
import com.media.sslmonitor.exception.DomainNotFoundException;
import com.media.sslmonitor.service.DomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebMvcTest(DomainController.class)
class DomainControllerTest {

    private static final String FIRST_DOMAIN = "example.com";
    private static final String SECOND_DOMAIN = "test.com";
    private static final String INVALID_DOMAIN = "nonexistent.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DomainService domainService;

    @Autowired
    private ObjectMapper objectMapper;

    private DomainCheckRequest testRequest;
    private DomainCheckResponse testResponse;
    private CertificateCheck testCertificateCheck;

    @BeforeEach
    void setUp() {
        testRequest = new DomainCheckRequest();
        testRequest.setDomains(Arrays.asList(FIRST_DOMAIN, SECOND_DOMAIN));

        testResponse = DomainCheckResponse.builder()
                .domain(FIRST_DOMAIN)
                .isValid(true)
                .expiryDate(LocalDateTime.now().plusDays(30))
                .daysUntilExpiry(30)
                .lastChecked(LocalDateTime.now())
                .build();

        Domain testDomain = new Domain();
        testDomain.setId(1L);
        testDomain.setDomainName(FIRST_DOMAIN);

        testCertificateCheck = new CertificateCheck();
        testCertificateCheck.setId(1L);
        testCertificateCheck.setDomain(testDomain);
        testCertificateCheck.setValid(true);
        testCertificateCheck.setExpiryDate(LocalDateTime.now().plusDays(30));
        testCertificateCheck.setIssuer("Test CA");
        testCertificateCheck.setSubject("CN=example.com");
        testCertificateCheck.setCheckTime(LocalDateTime.now());
    }

    @Test
    void checkDomains_ShouldReturnOkWithResults() throws Exception {
        List<DomainCheckResponse> expectedResponses = Collections.singletonList(testResponse);
        when(domainService.checkDomains(any(DomainCheckRequest.class))).thenReturn(expectedResponses);

        mockMvc.perform(post("/api/v1/domains/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].domain").value("example.com"))
                .andExpect(jsonPath("$[0].valid").value(true))
                .andExpect(jsonPath("$[0].daysUntilExpiry").value(30));
    }

    @Test
    void checkDomains_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        DomainCheckRequest invalidRequest = new DomainCheckRequest();
        invalidRequest.setDomains(List.of());

        mockMvc.perform(post("/api/v1/domains/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkDomainsAsync_ShouldReturnOkWithResults() throws Exception {
        List<DomainCheckResponse> expectedResponses = Collections.singletonList(testResponse);
        CompletableFuture<List<DomainCheckResponse>> futureResponse = CompletableFuture.completedFuture(expectedResponses);
        when(domainService.checkDomainsAsync(any(DomainCheckRequest.class))).thenReturn(futureResponse);

        mockMvc.perform(post("/api/v1/domains/check-async")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andDo(result -> {
                    mockMvc.perform(asyncDispatch(result))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                            .andExpect(jsonPath("$").isArray())
                            .andExpect(jsonPath("$[0].domain").value("example.com"))
                            .andExpect(jsonPath("$[0].valid").value(true));
                });
    }

    @Test
    void getExpiringDomains_ShouldReturnDomainsExpiringSoon() throws Exception {
        int days = 30;
        List<DomainCheckResponse> expectedResponses = Collections.singletonList(testResponse);
        when(domainService.getDomainsExpiringSoon(days)).thenReturn(expectedResponses);

        mockMvc.perform(get("/api/v1/domains/expiring")
                        .param("days", String.valueOf(days)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].domain").value("example.com"))
                .andExpect(jsonPath("$[0].daysUntilExpiry").value(30));
    }

    @Test
    void getDomainHistory_ShouldReturnPagedResults() throws Exception {
        Page<CertificateCheck> expectedPage = new PageImpl<>(Arrays.asList(testCertificateCheck));
        when(domainService.getDomainHistory(eq(FIRST_DOMAIN), any(Pageable.class))).thenReturn(expectedPage);

        mockMvc.perform(get("/api/v1/domains/{domainName}/history", FIRST_DOMAIN)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].valid").value(true))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void getDomainHistory_WhenDomainNotFound_ShouldReturnNotFound() throws Exception {
        when(domainService.getDomainHistory(eq(INVALID_DOMAIN), any(Pageable.class)))
                .thenThrow(new DomainNotFoundException("Domain not found: " + INVALID_DOMAIN));

        mockMvc.perform(get("/api/v1/domains/{domainName}/history", INVALID_DOMAIN))
                .andExpect(status().isNotFound());
    }
}