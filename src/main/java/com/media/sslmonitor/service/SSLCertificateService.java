package com.media.sslmonitor.service;

import com.media.sslmonitor.dto.CertificateInfo;
import java.util.concurrent.CompletableFuture;

public interface SSLCertificateService {

    CertificateInfo checkCertificate(String domain);
    CompletableFuture<CertificateInfo> checkCertificateAsync(String domain);
}