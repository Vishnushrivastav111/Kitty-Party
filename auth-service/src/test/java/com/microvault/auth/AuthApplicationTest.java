package com.microvault.auth;

import com.microvault.auth.entity.User;
import com.microvault.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class AuthApplicationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void startsAndSeedsDefaultAdmin() {
        assertNotNull(userRepository);
        User admin = userRepository.findByEmailIgnoreCaseAndDeletedFalse("admin@microvault.local").orElseThrow();
        assertEquals("admin", admin.getRole());
        assertTrue(admin.getPasswordHash().startsWith("pbkdf2_sha256$"));
    }
}
