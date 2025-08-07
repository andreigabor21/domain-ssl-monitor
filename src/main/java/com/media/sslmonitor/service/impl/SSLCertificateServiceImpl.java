package com.media.sslmonitor.service.impl;

import com.media.sslmonitor.dto.CertificateInfo;
import com.media.sslmonitor.service.SSLCertificateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.net.URI;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class SSLCertificateServiceImpl implements SSLCertificateService {

    @Override
    public CertificateInfo checkCertificate(String domain) {
        log.info("Checking SSL certificate for domain: {}", domain);

        try {
            final String cleanDomain = domain.replaceAll("^https?://", "").replaceAll("/.*$", "");

            final URI uri = URI.create("https://" + cleanDomain);
            final HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();

            final Certificate[] certificates = connection.getServerCertificates();
            final X509Certificate cert = (X509Certificate) certificates[0];

            final LocalDateTime expiryDate = LocalDateTime.ofInstant(
                    cert.getNotAfter().toInstant(),
                    ZoneId.systemDefault()
            );

            final long daysUntilExpiry = ChronoUnit.DAYS.between(
                    LocalDateTime.now(),
                    expiryDate
            );

            final CertificateInfo info = CertificateInfo.builder()
                    .domain(cleanDomain)
                    .isValid(true)
                    .expiryDate(expiryDate)
                    .issuer(cert.getIssuerX500Principal().getName())
                    .subject(cert.getSubjectX500Principal().getName())
                    .daysUntilExpiry((int) daysUntilExpiry)
                    .checkTime(LocalDateTime.now())
                    .build();

            log.info("Certificate check completed for {}: {} days until expiry",
                    cleanDomain, daysUntilExpiry);

            connection.disconnect();
            return info;

        } catch (Exception e) {
            log.error("Error checking certificate for domain {}: {}", domain, e.getMessage());

            return CertificateInfo.builder()
                    .domain(domain)
                    .isValid(false)
                    .errorMessage(e.getMessage())
                    .checkTime(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Async("sslCheckExecutor")
    public CompletableFuture<CertificateInfo> checkCertificateAsync(String domain) {
        log.info("Starting async SSL check for domain: {} on thread: {}",
                domain, Thread.currentThread().getName());

        final CertificateInfo result = checkCertificate(domain);
        return CompletableFuture.completedFuture(result);
    }
}