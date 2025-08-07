package com.media.sslmonitor.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "certificate_checks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Domain domain;

    @Column(name = "check_time")
    private LocalDateTime checkTime;

    @Column(name = "is_valid")
    private boolean isValid;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(length = 500)
    private String issuer;

    @Column(length = 500)
    private String subject;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        checkTime = LocalDateTime.now();
    }

    @Transient
    public Integer getDaysUntilExpiry() {
        if (expiryDate == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);
        return (int) days;
    }

    @Transient
    public String getAlertLevel() {
        final Integer days = getDaysUntilExpiry();
        if (days == null) return "ERROR";
        if (days <= 7) return "CRITICAL";
        if (days <= 30) return "WARNING";
        if (days <= 90) return "INFO";
        return "OK";
    }
}