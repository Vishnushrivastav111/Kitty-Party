package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.AffordabilityRequest;
import com.microvault.finance.dto.AffordabilityResponse;
import com.microvault.finance.entity.AffordabilityCheck;
import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.entity.SavingsEntry;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.AffordabilityRepository;
import com.microvault.finance.repository.FinanceProfileRepository;
import com.microvault.finance.repository.SavingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AffordabilityServiceImplTest {

    @Mock
    private AffordabilityRepository affordabilityRepository;

    @Mock
    private FinanceProfileRepository financeProfileRepository;

    @Mock
    private SavingsRepository savingsRepository;

    @InjectMocks
    private AffordabilityServiceImpl affordabilityService;

    private final UUID userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void shouldMarkSmallPurchaseAsAffordable() {
        FinanceProfile profile = new FinanceProfile();
        profile.setMonthlyIncome(new BigDecimal("50000"));
        profile.setMonthlyExpenses(new BigDecimal("20000"));
        SavingsEntry entry = new SavingsEntry();
        entry.setAmount(new BigDecimal("10000"));

        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
        when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)).thenReturn(List.of(entry));
        when(affordabilityRepository.save(any(AffordabilityCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AffordabilityRequest request = new AffordabilityRequest();
        request.setItemName("Headphones");
        request.setAmount(new BigDecimal("3000"));
        request.setCheckDate(LocalDate.now());
        request.setPriority("Want");

        AffordabilityResponse response = affordabilityService.check(userId, request);

        assertEquals("Comfortably affordable", response.getVerdict());
        assertEquals("success", response.getLevel());
        assertEquals(new BigDecimal("33000.00"), response.getAvailable());
        assertFalse(response.getPlan().isEmpty());
    }

    @Test
    void shouldRejectAPurchaseThatDoesNotFit() {
        FinanceProfile profile = new FinanceProfile();
        profile.setMonthlyIncome(new BigDecimal("20000"));
        profile.setMonthlyExpenses(new BigDecimal("18000"));
        when(financeProfileRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(profile));
        when(savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)).thenReturn(List.of());
        when(affordabilityRepository.save(any(AffordabilityCheck.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AffordabilityRequest request = new AffordabilityRequest();
        request.setItemName("Laptop");
        request.setAmount(new BigDecimal("80000"));
        request.setCheckDate(LocalDate.now());

        AffordabilityResponse response = affordabilityService.check(userId, request);

        assertEquals("Not recommended", response.getVerdict());
        assertEquals("danger", response.getLevel());
    }

    @Test
    void shouldRejectAFutureCheckDate() {
        AffordabilityRequest request = new AffordabilityRequest();
        request.setItemName("Trip");
        request.setAmount(new BigDecimal("1000"));
        request.setCheckDate(LocalDate.now().plusDays(2));

        assertThrows(ValidationException.class, () -> affordabilityService.check(userId, request));
    }
}
