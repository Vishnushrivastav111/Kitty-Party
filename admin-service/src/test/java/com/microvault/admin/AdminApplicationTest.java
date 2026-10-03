package com.microvault.admin;

import com.microvault.admin.repository.FeedbackRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AdminApplicationTest {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    void startsWithDatabase() {
        assertNotNull(feedbackRepository);
    }
}
