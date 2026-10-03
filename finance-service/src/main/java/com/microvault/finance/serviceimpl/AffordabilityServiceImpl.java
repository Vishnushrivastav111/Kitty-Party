package com.microvault.finance.serviceimpl;

import com.microvault.finance.dto.AffordabilityRequest;
import com.microvault.finance.dto.AffordabilityResponse;
import com.microvault.finance.entity.AffordabilityCheck;
import com.microvault.finance.entity.FinanceProfile;
import com.microvault.finance.entity.SavingsEntry;
import com.microvault.finance.exception.ResourceNotFoundException;
import com.microvault.finance.exception.ValidationException;
import com.microvault.finance.repository.AffordabilityRepository;
import com.microvault.finance.repository.FinanceProfileRepository;
import com.microvault.finance.repository.SavingsRepository;
import com.microvault.finance.service.AffordabilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AffordabilityServiceImpl implements AffordabilityService {

    private static final BigDecimal SAVINGS_SHARE = new BigDecimal("0.30");

    private final AffordabilityRepository affordabilityRepository;
    private final FinanceProfileRepository financeProfileRepository;
    private final SavingsRepository savingsRepository;

    public AffordabilityServiceImpl(AffordabilityRepository affordabilityRepository,
                                    FinanceProfileRepository financeProfileRepository,
                                    SavingsRepository savingsRepository) {
        this.affordabilityRepository = affordabilityRepository;
        this.financeProfileRepository = financeProfileRepository;
        this.savingsRepository = savingsRepository;
    }

    @Override
    public AffordabilityResponse check(UUID userId, AffordabilityRequest request) {
        if (request.getCheckDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Check date cannot be in the future");
        }

        FinanceProfile profile = financeProfileRepository.findByUserIdAndDeletedFalse(userId).orElse(null);
        BigDecimal income = profile == null ? BigDecimal.ZERO : money(profile.getMonthlyIncome());
        BigDecimal expenses = profile == null ? BigDecimal.ZERO : money(profile.getMonthlyExpenses());
        BigDecimal totalSavings = totalSavings(userId);

        BigDecimal surplus = income.subtract(expenses);
        if (surplus.compareTo(BigDecimal.ZERO) < 0) {
            surplus = BigDecimal.ZERO;
        }
        BigDecimal available = surplus.add(totalSavings.multiply(SAVINGS_SHARE)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal ratio;
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            ratio = BigDecimal.ONE;
        } else {
            ratio = request.getAmount().divide(available, 4, RoundingMode.HALF_UP);
        }

        String verdict;
        String level;
        String suggestion;
        if (ratio.compareTo(new BigDecimal("0.30")) <= 0) {
            verdict = "Comfortably affordable";
            level = "success";
            suggestion = "Buy now. This uses 30% or less of your available capacity. Keep the rest for regular bills.";
        } else if (ratio.compareTo(new BigDecimal("0.60")) <= 0) {
            verdict = "Affordable with caution";
            level = "warning";
            suggestion = "You can buy this, but it uses a large share of this month. Wait for the next pay cycle if it is not urgent.";
        } else if (ratio.compareTo(BigDecimal.ONE) <= 0) {
            verdict = "Tight — consider delaying";
            level = "warning";
            suggestion = "Delay this purchase. Put the monthly surplus into savings, then check again.";
        } else {
            verdict = "Not recommended";
            level = "danger";
            BigDecimal shortfall = request.getAmount().subtract(available).setScale(2, RoundingMode.HALF_UP);
            suggestion = "Do not buy this now. You are short by " + shortfall + ". Save that amount before you purchase.";
        }

        AffordabilityCheck check = new AffordabilityCheck();
        check.setId(UUID.randomUUID());
        check.setUserId(userId);
        check.setItemName(request.getItemName().trim());
        check.setAmount(request.getAmount());
        check.setAvailableAmount(available);
        check.setVerdict(verdict);
        check.setLevel(level);
        check.setPriority(request.getPriority() == null ? "Want" : request.getPriority());
        check.setCheckDate(request.getCheckDate());
        LocalDateTime now = LocalDateTime.now();
        check.setCreatedAt(now);
        check.setUpdatedAt(now);
        check.setDeleted(false);

        AffordabilityResponse response = toResponse(affordabilityRepository.save(check));
        response.setSuggestion(suggestion);
        response.setPlan(buildPlan(income, expenses, totalSavings, available, request.getAmount(), suggestion));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AffordabilityResponse> list(UUID userId) {
        List<AffordabilityResponse> rows = new ArrayList<>();
        for (AffordabilityCheck check : affordabilityRepository
                .findByUserIdAndDeletedFalseOrderByCheckDateDescCreatedAtDesc(userId)) {
            rows.add(toResponse(check));
        }
        return rows;
    }

    @Override
    public void softDelete(UUID userId, UUID id) {
        AffordabilityCheck check = affordabilityRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Affordability check not found"));
        LocalDateTime now = LocalDateTime.now();
        check.setDeleted(true);
        check.setDeletedAt(now);
        check.setUpdatedAt(now);
        affordabilityRepository.save(check);
    }

    @Override
    public void softDeleteAll(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        for (AffordabilityCheck check : affordabilityRepository
                .findByUserIdAndDeletedFalseOrderByCheckDateDescCreatedAtDesc(userId)) {
            check.setDeleted(true);
            check.setDeletedAt(now);
            check.setUpdatedAt(now);
            affordabilityRepository.save(check);
        }
    }

    /**
     * Available capacity = max(0, monthly income - monthly expenses) + 30% of savings entries.
     * The purchase is then compared with that capacity to choose a verdict and a short plan.
     */
    private List<String> buildPlan(BigDecimal income, BigDecimal expenses, BigDecimal savings,
                                   BigDecimal available, BigDecimal amount, String suggestion) {
        List<String> plan = new ArrayList<>();
        plan.add("Monthly surplus = income " + income + " minus expenses " + expenses + ". A negative surplus counts as zero.");
        plan.add("Add 30% of savings (" + savings + ") to that surplus. Available capacity is " + available + ".");
        plan.add("The purchase amount is " + amount + ".");
        plan.add(suggestion);
        return plan;
    }

    private BigDecimal totalSavings(UUID userId) {
        BigDecimal total = BigDecimal.ZERO;
        for (SavingsEntry entry : savingsRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)) {
            if (entry.getAmount() != null) {
                total = total.add(entry.getAmount());
            }
        }
        return total;
    }

    private AffordabilityResponse toResponse(AffordabilityCheck check) {
        AffordabilityResponse response = new AffordabilityResponse();
        response.setId(check.getId());
        response.setItemName(check.getItemName());
        response.setAmount(check.getAmount());
        response.setAvailable(check.getAvailableAmount());
        response.setAvailableAmount(check.getAvailableAmount());
        response.setVerdict(check.getVerdict());
        response.setLevel(check.getLevel());
        response.setPriority(check.getPriority());
        response.setDate(check.getCheckDate());
        return response;
    }

    private BigDecimal money(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
