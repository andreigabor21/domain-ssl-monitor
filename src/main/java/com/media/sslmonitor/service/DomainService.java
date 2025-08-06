package com.media.sslmonitor.service;

import com.media.sslmonitor.dto.DomainCheckRequest;
import com.media.sslmonitor.dto.DomainCheckResponse;
import com.media.sslmonitor.entity.CertificateCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DomainService {

    List<DomainCheckResponse> checkDomains(DomainCheckRequest request);
    CompletableFuture<List<DomainCheckResponse>> checkDomainsAsync(DomainCheckRequest request);
    List<DomainCheckResponse> getDomainsExpiringSoon(int days);
    Page<CertificateCheck> getDomainHistory(String domainName, Pageable pageable);
}