package com.media.sslmonitor.service;

import com.media.sslmonitor.dto.CertificateInfo;
import com.media.sslmonitor.dto.DomainCheckRequest;
import com.media.sslmonitor.dto.DomainCheckResponse;
import com.media.sslmonitor.entity.CertificateCheck;
import com.media.sslmonitor.entity.Domain;
import com.media.sslmonitor.exception.DomainNotFoundException;
import com.media.sslmonitor.repository.CertificateCheckRepository;
import com.media.sslmonitor.repository.DomainRepository;
import com.media.sslmonitor.service.impl.DomainServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomainServiceTest {

    private static final String FIRST_DOMAIN = "example.com";
    private static final String SECOND_DOMAIN = "test.com";
    private static final String INVALID_DOMAIN = "nonexistent.com";

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private CertificateCheckRepository certificateCheckRepository;

    @Mock
    private SSLCertificateService sslCertificateService;

    @InjectMocks
    private DomainServiceImpl domainService;

    private Domain testDomain;
    private CertificateInfo testCertificateInfo;
    private CertificateCheck testCertificateCheck;
    private DomainCheckRequest testRequest;

    @BeforeEach
    void setUp() {
        testDomain = new Domain();
        testDomain.setId(1L);
        testDomain.setDomainName(FIRST_DOMAIN);

        testCertificateInfo = new CertificateInfo();
        testCertificateInfo.setDomain(FIRST_DOMAIN);
        testCertificateInfo.setExpiryDate(LocalDateTime.now().plusDays(30));
        testCertificateInfo.setIssuer("Test CA");
        testCertificateInfo.setSubject("CN=example.com");
        testCertificateInfo.setValid(true);

        testCertificateCheck = new CertificateCheck();
        testCertificateCheck.setId(1L);
        testCertificateCheck.setDomain(testDomain);
        testCertificateCheck.setExpiryDate(testCertificateInfo.getExpiryDate());
        testCertificateCheck.setIssuer(testCertificateInfo.getIssuer());
        testCertificateCheck.setSubject(testCertificateInfo.getSubject());
        testCertificateCheck.setValid(testCertificateInfo.isValid());
        testCertificateCheck.setCheckTime(LocalDateTime.now());

        testRequest = new DomainCheckRequest();
        testRequest.setDomains(Arrays.asList(FIRST_DOMAIN, SECOND_DOMAIN));
    }

    @Test
    void checkDomains_ShouldReturnResults() {
        when(sslCertificateService.checkCertificate(FIRST_DOMAIN)).thenReturn(testCertificateInfo);
        when(sslCertificateService.checkCertificate(SECOND_DOMAIN)).thenReturn(testCertificateInfo);
        when(domainRepository.findByDomainName(anyString())).thenReturn(Optional.of(testDomain));
        when(certificateCheckRepository.save(any(CertificateCheck.class))).thenReturn(testCertificateCheck);

        List<DomainCheckResponse> results = domainService.checkDomains(testRequest);

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(sslCertificateService).checkCertificate(FIRST_DOMAIN);
        verify(sslCertificateService).checkCertificate(SECOND_DOMAIN);
        verify(certificateCheckRepository, times(2)).save(any(CertificateCheck.class));
    }

    @Test
    void checkDomainsAsync_ShouldReturnResultsAsync() throws Exception {
        CompletableFuture<CertificateInfo> certInfoFuture = CompletableFuture.completedFuture(testCertificateInfo);
        when(sslCertificateService.checkCertificateAsync(FIRST_DOMAIN)).thenReturn(certInfoFuture);
        when(sslCertificateService.checkCertificateAsync(SECOND_DOMAIN)).thenReturn(certInfoFuture);
        when(domainRepository.findByDomainName(anyString())).thenReturn(Optional.of(testDomain));
        when(certificateCheckRepository.save(any(CertificateCheck.class))).thenReturn(testCertificateCheck);

        CompletableFuture<List<DomainCheckResponse>> futureResults = domainService.checkDomainsAsync(testRequest);
        List<DomainCheckResponse> results = futureResults.get();

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(sslCertificateService).checkCertificateAsync(FIRST_DOMAIN);
        verify(sslCertificateService).checkCertificateAsync(SECOND_DOMAIN);
        verify(certificateCheckRepository, times(2)).save(any(CertificateCheck.class));
    }

    @Test
    void getDomainsExpiringSoon_ShouldReturnFilteredResults() {
        int days = 30;
        List<CertificateCheck> expectedChecks = Collections.singletonList(testCertificateCheck);
        when(certificateCheckRepository.findLatestChecksExpiringBefore(any(LocalDateTime.class)))
                .thenReturn(expectedChecks);

        List<DomainCheckResponse> results = domainService.getDomainsExpiringSoon(days);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(testDomain.getDomainName(), results.getFirst().getDomain());
        verify(certificateCheckRepository).findLatestChecksExpiringBefore(any(LocalDateTime.class));
    }

    @Test
    void getDomainHistory_WhenDomainExists_ShouldReturnPagedResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<CertificateCheck> expectedPage = new PageImpl<>(Collections.singletonList(testCertificateCheck));
        when(domainRepository.findByDomainName(FIRST_DOMAIN)).thenReturn(Optional.of(testDomain));
        when(certificateCheckRepository.findByDomainOrderByCheckTimeDesc(testDomain, pageable))
                .thenReturn(expectedPage);

        Page<CertificateCheck> result = domainService.getDomainHistory(FIRST_DOMAIN, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testCertificateCheck.getId(), result.getContent().getFirst().getId());
        verify(domainRepository).findByDomainName(FIRST_DOMAIN);
        verify(certificateCheckRepository).findByDomainOrderByCheckTimeDesc(testDomain, pageable);
    }

    @Test
    void getDomainHistory_WhenDomainNotFound_ShouldThrowException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(domainRepository.findByDomainName(INVALID_DOMAIN)).thenReturn(Optional.empty());

        assertThrows(DomainNotFoundException.class, () -> {
            domainService.getDomainHistory(INVALID_DOMAIN, pageable);
        });
        verify(domainRepository).findByDomainName(INVALID_DOMAIN);
        verify(certificateCheckRepository, never()).findByDomainOrderByCheckTimeDesc(any(), any());
    }
}