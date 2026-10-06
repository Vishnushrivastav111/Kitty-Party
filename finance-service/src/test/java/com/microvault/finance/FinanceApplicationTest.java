package com.microvault.finance;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class FinanceApplicationTest {

    @Test
    void mainStartsTheSpringApplicationWithTheGivenArguments() {
        String[] args = {"--server.port=0"};
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            spring.when(() -> SpringApplication.run(any(Class.class), any(String[].class))).thenReturn(null);

            FinanceApplication.main(args);

            spring.verify(() -> SpringApplication.run(FinanceApplication.class, args));
        }
    }

    @Test
    void applicationIsAConfiguredSpringBootApplication() {
        assertNotNull(new FinanceApplication());
        assertNotNull(FinanceApplication.class.getAnnotation(SpringBootApplication.class));
    }
}
