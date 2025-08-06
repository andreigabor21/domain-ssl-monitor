package com.media.sslmonitor.service.impl;

import com.media.sslmonitor.dto.CertificateInfo;
import com.media.sslmonitor.dto.DomainCheckRequest;
import com.media.sslmonitor.dto.DomainCheckResponse;
import com.media.sslmonitor.entity.CertificateCheck;
import com.media.sslmonitor.entity.Domain;
import com.media.sslmonitor.exception.DomainNotFoundException;
import com.media.sslmonitor.repository.CertificateCheckRepository;
import com.media.sslmonitor.repository.DomainRepository;
import com.media.sslmonitor.service.DomainService;
import com.media.sslmonitor.service.SSLCertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DomainServiceImpl implements DomainService {

    private final DomainRepository domainRepository;
    private final CertificateCheckRepository certificateCheckRepository;
    private final SSLCertificateService sslCertificateService;

    @Override
    public List<DomainCheckResponse> checkDomains(DomainCheckRequest request) {
        log.info("Starting synchronous check for {} domains", request.getDomains().size());
        long startTime = System.currentTimeMillis();

        List<DomainCheckResponse> collect = request.getDomains().stream()
                .map(this::checkSingleDomain)
                .collect(Collectors.toList());
        long duration = System.currentTimeMillis() - startTime;
        log.info("Synchronous check completed for {} domains in {} ms",
                request.getDomains().size(), duration);
        return collect;
    }

    @Override
    public CompletableFuture<List<DomainCheckResponse>> checkDomainsAsync(DomainCheckRequest request) {
        log.info("Starting asynchronous check for {} domains", request.getDomains().size());
        long startTime = System.currentTimeMillis();

        List<CompletableFuture<DomainCheckResponse>> futures = request.getDomains().stream()
                .map(domain -> sslCertificateService.checkCertificateAsync(domain)
                        .thenApply(this::saveCertificateCheck))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<DomainCheckResponse> results = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

                    long duration = System.currentTimeMillis() - startTime;
                    log.info("Async check completed for {} domains in {} ms",
                            request.getDomains().size(), duration);

                    return results;
                });
    }

    private DomainCheckResponse checkSingleDomain(String domainName) {
        CertificateInfo certInfo = sslCertificateService.checkCertificate(domainName);
        return saveCertificateCheck(certInfo);
    }

    private DomainCheckResponse saveCertificateCheck(CertificateInfo certInfo) {
        Domain domain = domainRepository.findByDomainName(certInfo.getDomain())
                .orElseGet(() -> {
                    Domain newDomain = new Domain();
                    newDomain.setDomainName(certInfo.getDomain());
                    return domainRepository.save(newDomain);
                });

        CertificateCheck check = new CertificateCheck();
        check.setDomain(domain);
        check.setValid(certInfo.isValid());
        check.setExpiryDate(certInfo.getExpiryDate());
        check.setIssuer(certInfo.getIssuer());
        check.setSubject(certInfo.getSubject());
        check.setErrorMessage(certInfo.getErrorMessage());

        certificateCheckRepository.save(check);

        return DomainCheckResponse.fromCertificateInfo(certInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainCheckResponse> getDomainsExpiringSoon(int days) {
        log.info("Finding domains expiring within {} days", days);

        LocalDateTime expiryThreshold = LocalDateTime.now().plusDays(days);

        return certificateCheckRepository.findLatestChecksExpiringBefore(expiryThreshold).stream()
                .map(check -> DomainCheckResponse.builder()
                        .domain(check.getDomain().getDomainName())
                        .isValid(check.isValid())
                        .expiryDate(check.getExpiryDate())
                        .daysUntilExpiry(check.getDaysUntilExpiry())
                        .alertLevel(check.getAlertLevel())
                        .lastChecked(check.getCheckTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CertificateCheck> getDomainHistory(String domainName, Pageable pageable) {
        Domain domain = domainRepository.findByDomainName(domainName)
                .orElseThrow(() -> new DomainNotFoundException("Domain not found: " + domainName));

        return certificateCheckRepository.findByDomainOrderByCheckTimeDesc(domain, pageable);
    }
}