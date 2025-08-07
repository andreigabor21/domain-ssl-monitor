package com.media.sslmonitor.controller;

import com.media.sslmonitor.dto.DomainCheckRequest;
import com.media.sslmonitor.dto.DomainCheckResponse;
import com.media.sslmonitor.entity.CertificateCheck;
import com.media.sslmonitor.service.DomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/domains")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Domain SSL Monitor", description = "Endpoints for SSL certificate monitoring")
public class DomainController {

    private final DomainService domainService;

    @PostMapping("/check")
    @Operation(summary = "Check SSL certificates for multiple domains")
    public ResponseEntity<List<DomainCheckResponse>> checkDomains(
            @Valid @RequestBody DomainCheckRequest request) {
        log.info("Received request to check {} domains", request.getDomains().size());
        List<DomainCheckResponse> responses = domainService.checkDomains(request);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/check-async")
    @Operation(summary = "Check SSL certificates for multiple domains async")
    public CompletableFuture<ResponseEntity<List<DomainCheckResponse>>> checkDomainsAsync(
            @Valid @RequestBody DomainCheckRequest request) {
        log.info("Received async request to check {} domains", request.getDomains().size());

        return domainService.checkDomainsAsync(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error in async domain check", ex);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/expiring")
    @Operation(summary = "Get domains with certificates expiring soon")
    public ResponseEntity<List<DomainCheckResponse>> getExpiringDomains(
            @Parameter(description = "Number of days threshold")
            @RequestParam(defaultValue = "30") int days) {
        log.info("Getting domains expiring within {} days", days);
        return ResponseEntity.ok(domainService.getDomainsExpiringSoon(days));
    }

    @GetMapping("/{domainName}/history")
    @Operation(summary = "Get certificate check history for a domain")
    public ResponseEntity<Page<CertificateCheck>> getDomainHistory(
            @PathVariable String domainName,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Getting history for domain: {}", domainName);
        return ResponseEntity.ok(domainService.getDomainHistory(domainName, pageable));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Domain SSL Monitor"
        ));
    }
}