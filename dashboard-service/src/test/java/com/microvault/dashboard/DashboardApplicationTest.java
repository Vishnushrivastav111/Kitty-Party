package com.microvault.dashboard;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class DashboardApplicationTest {

    @Test
    void mainStartsTheSpringApplicationWithTheGivenArguments() {
        String[] args = {"--server.port=0"};
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            spring.when(() -> SpringApplication.run(any(Class.class), any(String[].class))).thenReturn(null);

            DashboardApplication.main(args);

            spring.verify(() -> SpringApplication.run(DashboardApplication.class, args));
        }
    }

    @Test
    void applicationIsAConfiguredSpringBootApplication() {
        assertNotNull(new DashboardApplication());
        assertNotNull(DashboardApplication.class.getAnnotation(SpringBootApplication.class));
    }
}
