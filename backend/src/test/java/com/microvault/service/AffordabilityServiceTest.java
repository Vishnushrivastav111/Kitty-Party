package com.microvault.service;

import com.microvault.dto.AffordabilityCheckDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.AffordabilityCheck;
import com.microvault.serviceimpl.AffordabilityCheckServiceImpl;
import com.microvault.support.InMemoryAffordabilityCheckDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffordabilityServiceTest {

    private AffordabilityCheckService affordabilityCheckService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        affordabilityCheckService = new AffordabilityCheckServiceImpl(new InMemoryAffordabilityCheckDAO());
        userId = UUID.randomUUID();
    }

    @Test
    void validAffordabilityCalculation() {
        AffordabilityCheckDTO result = affordabilityCheckService.checkAffordability(
                userId,
                "New phone",
                new BigDecimal("10000"),
                "medium",
                LocalDate.now(),
                new BigDecimal("80000"),
                new BigDecimal("40000"),
                new BigDecimal("20000")
        );

        assertNotNull(result.getId());
        assertEquals("New phone", result.getItemName());
        assertEquals(new BigDecimal("46000.00"), result.getAvailableAmount());
        assertEquals("Comfortably affordable", result.getVerdict());
        assertEquals("success", result.getLevel());
    }

    @Test
    void updateAndSoftDeleteCheck() {
        AffordabilityCheckDTO saved = affordabilityCheckService.checkAffordability(
                userId, "Headphones", new BigDecimal("3000"), "low",
                LocalDate.now(), new BigDecimal("50000"), new BigDecimal("20000"), BigDecimal.ZERO);

        AffordabilityCheck update = new AffordabilityCheck(userId, "Headphones Pro",
                new BigDecimal("3500"), saved.getAvailableAmount(), saved.getVerdict(),
                saved.getLevel(), "low", LocalDate.now());
        update.setId(saved.getId());

        assertTrue(affordabilityCheckService.updateCheck(update));
        assertEquals("Headphones Pro", affordabilityCheckService.getCheckById(saved.getId()).getItemName());
        assertTrue(affordabilityCheckService.softDeleteCheck(saved.getId()));
        assertThrows(ValidationException.class, () -> affordabilityCheckService.getCheckById(saved.getId()));
    }

    @Test
    void negativeCostIsRejected() {
        AffordabilityCheck check = validCheck();
        check.setAmount(new BigDecimal("-1"));

        assertThrows(ValidationException.class, () -> affordabilityCheckService.createCheck(check));
    }

    @Test
    void zeroCostIsRejected() {
        AffordabilityCheck check = validCheck();
        check.setAmount(BigDecimal.ZERO);

        assertThrows(ValidationException.class, () -> affordabilityCheckService.createCheck(check));
    }

    @Test
    void missingItemNameIsRejected() {
        AffordabilityCheck check = validCheck();
        check.setItemName(" ");

        assertThrows(ValidationException.class, () -> affordabilityCheckService.createCheck(check));
    }

    @Test
    void futureDateIsRejected() {
        AffordabilityCheck check = validCheck();
        check.setCheckDate(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> affordabilityCheckService.createCheck(check));
    }

    private AffordabilityCheck validCheck() {
        return new AffordabilityCheck(userId, "Item", new BigDecimal("1000"),
                new BigDecimal("5000"), "Comfortably affordable", "success", "low", LocalDate.now());
    }
}
