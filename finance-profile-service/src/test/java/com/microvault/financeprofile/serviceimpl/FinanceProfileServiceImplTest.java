package com.microvault.financeprofile.serviceimpl;

import com.microvault.financeprofile.dto.FinanceProfileRequest;
import com.microvault.financeprofile.dto.FinanceProfileResponse;
import com.microvault.financeprofile.dto.MonthlySummaryResponse;
import com.microvault.financeprofile.dto.SurplusResponse;
import com.microvault.financeprofile.exception.DuplicateProfileException;
import com.microvault.financeprofile.exception.ProfileNotFoundException;
import com.microvault.financeprofile.exception.ValidationException;
import com.microvault.financeprofile.model.FinanceProfile;
import com.microvault.financeprofile.repository.FinanceProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceProfileServiceImplTest {

    @Mock
    private FinanceProfileRepository financeProfileRepository;

    private FinanceProfileServiceImpl service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new FinanceProfileServiceImpl(financeProfileRepository);
        userId = UUID.fromString("a3333333-3333-4333-8333-333333333333");
    }

    @Test
    void createProfileStoresIncomeAndZeroForMissingAmounts() {
        when(financeProfileRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());
        when(financeProfileRepository.save(any(FinanceProfile.class)))
                .thenAnswer(call -> call.getArgument(0));

        FinanceProfileRequest request = salaryRequest();
        request.setMonthlyExpenses(null);
        request.setHasLoan(null);

        FinanceProfileResponse saved = service.createProfile(request);

        assertEquals(userId, saved.getUserId());
        assertEquals(new BigDecimal("85000.00"), saved.getMonthlyIncome());
        assertEquals(BigDecimal.ZERO, saved.getMonthlyExpenses());
        assertFalse(saved.getHasLoan());
        assertEquals("Salary", saved.getIncomeSource());
    }

    @Test
    void createProfileRejectsASecondActiveProfile() {
        when(financeProfileRepository.findByUserIdAndIsDeletedFalse(userId))
                .thenReturn(Optional.of(existingProfile()));

        assertThrows(DuplicateProfileException.class, () -> service.createProfile(salaryRequest()));
    }

    @Test
    void createProfileRejectsNegativeIncome() {
        FinanceProfileRequest request = salaryRequest();
        request.setMonthlyIncome(new BigDecimal("-1"));

        ValidationException error = assertThrows(ValidationException.class, () -> service.createProfile(request));
        assertEquals("Monthly income cannot be negative", error.getMessage());
    }

    @Test
    void createProfileRejectsAFutureSetupDate() {
        FinanceProfileRequest request = salaryRequest();
        request.setSetupDate(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> service.createProfile(request));
    }

    @Test
    void monthlySurplusIsZeroWhenExpensesAreHigherThanIncome() {
        FinanceProfile profile = existingProfile();
        profile.setMonthlyIncome(new BigDecimal("20000"));
        profile.setMonthlyExpenses(new BigDecimal("25000"));
        when(financeProfileRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(profile));

        SurplusResponse surplus = service.getMonthlySurplus(userId);

        assertEquals(BigDecimal.ZERO, surplus.getMonthlySurplus());
    }

    @Test
    void summaryKeepsTheShortfallAfterEmi() {
        FinanceProfile profile = existingProfile();
        profile.setMonthlyIncome(new BigDecimal("50000"));
        profile.setMonthlyExpenses(new BigDecimal("30000"));
        profile.setMonthlyEmi(new BigDecimal("8000"));
        when(financeProfileRepository.findByUserIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(profile));

        MonthlySummaryResponse summary = service.getMonthlySummary(userId);

        assertEquals(new BigDecimal("20000"), summary.getMonthlySurplus());
        assertEquals(new BigDecimal("12000"), summary.getLeftAfterEmi());
        assertEquals(new BigDecimal("40.00"), summary.getSavingsRatePercent());
    }

    @Test
    void updateDoesNotChangeTheUser() {
        FinanceProfile profile = existingProfile();
        when(financeProfileRepository.findByIdAndIsDeletedFalse(profile.getId())).thenReturn(Optional.of(profile));

        FinanceProfileRequest request = salaryRequest();
        request.setUserId(UUID.randomUUID());
        request.setMonthlyIncome(new BigDecimal("90000"));

        assertThrows(ValidationException.class, () -> service.updateProfile(profile.getId(), request));
    }

    @Test
    void missingProfileReturnsNotFound() {
        UUID id = UUID.randomUUID();
        when(financeProfileRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> service.getProfileById(id));
    }

    @Test
    void softDeleteMarksTheRowInsteadOfRemovingIt() {
        FinanceProfile profile = existingProfile();
        when(financeProfileRepository.findByIdAndIsDeletedFalse(profile.getId())).thenReturn(Optional.of(profile));
        when(financeProfileRepository.save(any(FinanceProfile.class)))
                .thenAnswer(call -> call.getArgument(0));

        service.softDeleteProfile(profile.getId());

        ArgumentCaptor<FinanceProfile> saved = ArgumentCaptor.forClass(FinanceProfile.class);
        verify(financeProfileRepository).save(saved.capture());
        assertTrue(saved.getValue().getIsDeleted());
    }

    @Test
    void getAllSkipsNothingWhenTheRepositoryAlreadyFiltersDeletedRows() {
        when(financeProfileRepository.findByIsDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(existingProfile()));

        assertEquals(1, service.getAllProfiles().size());
    }

    private FinanceProfileRequest salaryRequest() {
        FinanceProfileRequest request = new FinanceProfileRequest();
        request.setUserId(userId);
        request.setIncomeSource(" Salary ");
        request.setMonthlyIncome(new BigDecimal("85000.00"));
        request.setPayCycle("Monthly");
        request.setMonthlyExpenses(new BigDecimal("42000"));
        request.setExpenseCategories("Food,Travel,Bills");
        request.setHasLoan(Boolean.FALSE);
        request.setCurrentSavings(new BigDecimal("150000"));
        request.setMonthlyBudget(new BigDecimal("40000"));
        request.setBudgetStyle("50/30/20");
        request.setSetupDate(LocalDate.of(2026, 1, 15));
        return request;
    }

    private FinanceProfile existingProfile() {
        FinanceProfile profile = new FinanceProfile();
        profile.setId(UUID.fromString("b1111111-1111-4111-8111-111111111111"));
        profile.setUserId(userId);
        profile.setIncomeSource("Salary");
        profile.setMonthlyIncome(new BigDecimal("85000"));
        profile.setMonthlyExpenses(new BigDecimal("42000"));
        profile.setMonthlyEmi(BigDecimal.ZERO);
        profile.setLoanAmount(BigDecimal.ZERO);
        profile.setCurrentSavings(new BigDecimal("150000"));
        profile.setInvestments(BigDecimal.ZERO);
        profile.setMonthlyBudget(new BigDecimal("40000"));
        profile.setHasLoan(Boolean.FALSE);
        profile.setIsDeleted(Boolean.FALSE);
        return profile;
    }
}
