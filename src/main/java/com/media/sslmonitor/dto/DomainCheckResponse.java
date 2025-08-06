package com.media.sslmonitor.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DomainCheckResponse {

    private String domain;
    private boolean isValid;
    private LocalDateTime expiryDate;
    private Integer daysUntilExpiry;
    private String alertLevel;
    private LocalDateTime lastChecked;
    private String error;

    public static DomainCheckResponse fromCertificateInfo(CertificateInfo info) {
        return DomainCheckResponse.builder()
                .domain(info.getDomain())
                .isValid(info.isValid())
                .expiryDate(info.getExpiryDate())
                .daysUntilExpiry(info.getDaysUntilExpiry())
                .alertLevel(getAlertLevel(info.getDaysUntilExpiry()))
                .lastChecked(info.getCheckTime())
                .error(info.getErrorMessage())
                .build();
    }

    private static String getAlertLevel(Integer daysUntilExpiry) {
        if (daysUntilExpiry == null) return "ERROR";
        if (daysUntilExpiry <= 7) return "CRITICAL";
        if (daysUntilExpiry <= 30) return "WARNING";
        if (daysUntilExpiry <= 90) return "INFO";
        return "OK";
    }
}