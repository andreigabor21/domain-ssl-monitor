package com.media.sslmonitor.repository;

import com.media.sslmonitor.entity.CertificateCheck;
import com.media.sslmonitor.entity.Domain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CertificateCheckRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CertificateCheckRepository certificateCheckRepository;

    private Domain domain1;
    private CertificateCheck check1;
    private CertificateCheck check2;

    @BeforeEach
    void setUp() {
        domain1 = new Domain();
        domain1.setDomainName("example.com");
        entityManager.persistAndFlush(domain1);

        Domain domain2 = new Domain();
        domain2.setDomainName("test.com");
        entityManager.persistAndFlush(domain2);

        LocalDateTime now = LocalDateTime.now();

        check1 = new CertificateCheck();
        check1.setDomain(domain1);
        check1.setValid(true);
        check1.setExpiryDate(now.plusDays(15));
        check1.setCheckTime(now.minusHours(2));
        check1.setIssuer("Test CA");
        check1.setSubject("CN=example.com");
        entityManager.persistAndFlush(check1);

        check2 = new CertificateCheck();
        check2.setDomain(domain1);
        check2.setValid(true);
        check2.setExpiryDate(now.plusDays(20));
        check2.setCheckTime(now.minusHours(1));
        check2.setIssuer("Test CA");
        check2.setSubject("CN=example.com");
        entityManager.persistAndFlush(check2);

        CertificateCheck check3 = new CertificateCheck();
        check3.setDomain(domain2);
        check3.setValid(true);
        check3.setExpiryDate(now.plusDays(60));
        check3.setCheckTime(now.minusMinutes(30));
        check3.setIssuer("Another CA");
        check3.setSubject("CN=test.com");
        entityManager.persistAndFlush(check3);
    }

    @Test
    void findByDomainOrderByCheckTimeDesc_ShouldReturnChecksOrderedByTime() {
        Page<CertificateCheck> result = certificateCheckRepository
                .findByDomainOrderByCheckTimeDesc(domain1, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        List<CertificateCheck> checks = result.getContent();

        assertEquals(check2.getId(), checks.get(0).getId());
        assertEquals(check1.getId(), checks.get(1).getId());
        assertTrue(checks.get(0).getCheckTime().isAfter(checks.get(1).getCheckTime()));
    }

    @Test
    void findLatestChecksExpiringBefore_ShouldReturnOnlyLatestValidChecksExpiringSoon() {
        LocalDateTime threshold = LocalDateTime.now().plusDays(25);

        List<CertificateCheck> result = certificateCheckRepository
                .findLatestChecksExpiringBefore(threshold);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(check2.getId(), result.getFirst().getId()); // Should return latest check for domain1
        assertEquals(domain1.getDomainName(), result.getFirst().getDomain().getDomainName());
        assertTrue(result.getFirst().getExpiryDate().isBefore(threshold));
    }

    @Test
    void findLatestChecksExpiringBefore_ShouldReturnEmptyWhenNoMatchingChecks() {
        LocalDateTime threshold = LocalDateTime.now().plusDays(5);

        List<CertificateCheck> result = certificateCheckRepository
                .findLatestChecksExpiringBefore(threshold);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}