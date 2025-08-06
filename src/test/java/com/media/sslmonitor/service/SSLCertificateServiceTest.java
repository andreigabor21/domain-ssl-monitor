package com.media.sslmonitor.service;

import com.media.sslmonitor.dto.CertificateInfo;
import com.media.sslmonitor.service.impl.SSLCertificateServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SSLCertificateServiceTest {

    private static final String VALID_DOMAIN = "google.com";
    private static final String INVALID_DOMAIN = "this-domain-does-not-exist.com";

    @InjectMocks
    private SSLCertificateServiceImpl sslCertificateService;

    @Test
    void checkCertificate_WithValidDomain_ShouldReturnValidCertificateInfo() {
        CertificateInfo result = sslCertificateService.checkCertificate(VALID_DOMAIN);

        assertNotNull(result);
        assertEquals(VALID_DOMAIN, result.getDomain());
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    void checkCertificate_WithInvalidDomain_ShouldReturnInvalidCertificateInfo() {
        CertificateInfo result = sslCertificateService.checkCertificate(INVALID_DOMAIN);

        assertNotNull(result);
        assertEquals(INVALID_DOMAIN, result.getDomain());
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void checkCertificate_WithDomainContainingProtocol_ShouldCleanDomainName() {
        String domainWithProtocol = "https://" + VALID_DOMAIN + "/path";
        CertificateInfo result = sslCertificateService.checkCertificate(domainWithProtocol);

        assertNotNull(result);
        assertEquals(VALID_DOMAIN, result.getDomain()); // Should be cleaned
        assertTrue(result.isValid());
    }

    @Test
    void checkCertificateAsync_WithValidDomain_shouldReturnValidCertificateInfo() throws Exception {
        CompletableFuture<CertificateInfo> futureResult = sslCertificateService.checkCertificateAsync(VALID_DOMAIN);
        CertificateInfo result = futureResult.get();

        assertNotNull(futureResult);
        assertTrue(futureResult.isDone());
        assertNotNull(result);
        assertEquals(VALID_DOMAIN, result.getDomain());
        assertTrue(result.isValid());
    }

    @Test
    void checkCertificateAsync_WithInvalidDomain_ShouldReturnInvalidCertificateInfo() throws Exception {
        CompletableFuture<CertificateInfo> futureResult = sslCertificateService.checkCertificateAsync(INVALID_DOMAIN);
        CertificateInfo result = futureResult.get();

        assertNotNull(futureResult);
        assertTrue(futureResult.isDone());
        assertNotNull(result);
        assertEquals(INVALID_DOMAIN, result.getDomain());
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }
}