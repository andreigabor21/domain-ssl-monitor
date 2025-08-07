package com.media.sslmonitor.repository;

import com.media.sslmonitor.entity.CertificateCheck;
import com.media.sslmonitor.entity.Domain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CertificateCheckRepository extends JpaRepository<CertificateCheck, Long> {

    Page<CertificateCheck> findByDomainOrderByCheckTimeDesc(Domain domain, Pageable pageable);

    @Query("""
        SELECT c FROM CertificateCheck c
        WHERE c.id IN (
            SELECT MAX(cc.id) FROM CertificateCheck cc
            GROUP BY cc.domain.id
        )
        AND c.isValid = true
        AND c.expiryDate <= :expiryDate
        ORDER BY c.expiryDate ASC
    """)
    List<CertificateCheck> findLatestChecksExpiringBefore(@Param("expiryDate") LocalDateTime expiryDate);
}