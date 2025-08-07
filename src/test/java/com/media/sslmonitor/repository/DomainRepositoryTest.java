package com.media.sslmonitor.repository;

import com.media.sslmonitor.entity.Domain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class DomainRepositoryTest {

    private static final String FIRST_DOMAIN = "example.com";
    private static final String SECOND_DOMAIN = "test.com";
    private static final String INVALID_DOMAIN = "nonexistent.com";

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DomainRepository domainRepository;

    @BeforeEach
    void setUp() {
        Domain testDomain1 = new Domain();
        testDomain1.setDomainName(FIRST_DOMAIN);

        Domain testDomain2 = new Domain();
        testDomain2.setDomainName(SECOND_DOMAIN);

        entityManager.persistAndFlush(testDomain1);
        entityManager.persistAndFlush(testDomain2);
    }

    @Test
    void findByDomainName_WhenDomainExists_ShouldReturnDomain() {
        Optional<Domain> result = domainRepository.findByDomainName(FIRST_DOMAIN);

        assertTrue(result.isPresent());
        assertEquals(FIRST_DOMAIN, result.get().getDomainName());
        assertNotNull(result.get().getId());
        assertNotNull(result.get().getCreatedAt());
        assertNotNull(result.get().getUpdatedAt());
    }

    @Test
    void findByDomainName_WhenDomainDoesNotExist_ShouldReturnEmpty() {
        Optional<Domain> result = domainRepository.findByDomainName(INVALID_DOMAIN);

        assertFalse(result.isPresent());
    }

    @Test
    void save_ShouldPersistDomainWithTimestamps() {
        Domain newDomain = new Domain();
        newDomain.setDomainName("newdomain.com");

        Domain savedDomain = domainRepository.save(newDomain);
        entityManager.flush();

        assertNotNull(savedDomain.getId());
        assertEquals("newdomain.com", savedDomain.getDomainName());
        assertNotNull(savedDomain.getCreatedAt());
        assertNotNull(savedDomain.getUpdatedAt());
        assertEquals(savedDomain.getCreatedAt(), savedDomain.getUpdatedAt());
    }
}
