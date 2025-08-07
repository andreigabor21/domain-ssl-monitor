package com.media.sslmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateInfo {

    private String domain;
    private boolean isValid;
    private LocalDateTime expiryDate;
    private String issuer;
    private String subject;
    private Integer daysUntilExpiry;
    private String errorMessage;
    private LocalDateTime checkTime;
}