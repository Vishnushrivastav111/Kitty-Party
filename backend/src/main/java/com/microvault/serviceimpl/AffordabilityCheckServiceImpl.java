package com.microvault.serviceimpl;

import com.microvault.business.AffordabilityBO;
import com.microvault.dao.AffordabilityCheckDAO;
import com.microvault.dto.AffordabilityCheckDTO;
import com.microvault.exception.ValidationException;
import com.microvault.model.AffordabilityCheck;
import com.microvault.service.AffordabilityCheckService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Business rules for affordability checks. The DAO is received through the
 * constructor, the maths itself lives in {@link AffordabilityBO}.
 */
public class AffordabilityCheckServiceImpl implements AffordabilityCheckService {

    private final AffordabilityCheckDAO affordabilityCheckDAO;
    private final AffordabilityBO affordabilityBO = new AffordabilityBO();

    public AffordabilityCheckServiceImpl(AffordabilityCheckDAO affordabilityCheckDAO) {
        this.affordabilityCheckDAO = affordabilityCheckDAO;
    }

    @Override
    public AffordabilityCheckDTO checkAffordability(UUID userId, String itemName, BigDecimal amount, String priority,
                                                    LocalDate checkDate, BigDecimal monthlyIncome,
                                                    BigDecimal monthlyExpenses, BigDecimal totalSavings) {

        AffordabilityCheck affordabilityCheck = new AffordabilityCheck();
        affordabilityCheck.setUserId(userId);
        affordabilityCheck.setItemName(itemName);
        affordabilityCheck.setAmount(amount);
        affordabilityCheck.setPriority(priority);
        affordabilityCheck.setCheckDate(checkDate);

        BigDecimal availableAmount = affordabilityBO.calculateAvailableCapacity(
                monthlyIncome, monthlyExpenses, totalSavings);

        affordabilityCheck.setAvailableAmount(availableAmount);
        affordabilityCheck.setVerdict(affordabilityBO.decideVerdict(amount, availableAmount));
        affordabilityCheck.setLevel(affordabilityBO.decideLevel(amount, availableAmount));

        validateAffordabilityCheck(affordabilityCheck);

        AffordabilityCheck savedCheck = affordabilityCheckDAO.create(affordabilityCheck);
        return toDTO(savedCheck);
    }

    @Override
    public AffordabilityCheckDTO createCheck(AffordabilityCheck affordabilityCheck) {

        validateAffordabilityCheck(affordabilityCheck);

        if (affordabilityCheck.getAvailableAmount() == null) {
            affordabilityCheck.setAvailableAmount(BigDecimal.ZERO);
        }

        AffordabilityCheck savedCheck = affordabilityCheckDAO.create(affordabilityCheck);
        return toDTO(savedCheck);
    }

    @Override
    public AffordabilityCheckDTO getCheckById(UUID id) {
        if (id == null) {
            throw new ValidationException("Affordability check id is required");
        }
        AffordabilityCheck affordabilityCheck = affordabilityCheckDAO.findById(id);
        if (affordabilityCheck == null) {
            throw new ValidationException("No active affordability check found with id " + id);
        }
        return toDTO(affordabilityCheck);
    }

    @Override
    public List<AffordabilityCheckDTO> getAllChecks() {
        return toDTOList(affordabilityCheckDAO.findAll());
    }

    @Override
    public List<AffordabilityCheckDTO> getChecksByUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User id is required");
        }
        return toDTOList(affordabilityCheckDAO.findByUserId(userId));
    }

    @Override
    public boolean updateCheck(AffordabilityCheck affordabilityCheck) {

        if (affordabilityCheck == null || affordabilityCheck.getId() == null) {
            throw new ValidationException("Affordability check id is required for an update");
        }
        validateAffordabilityCheck(affordabilityCheck);

        AffordabilityCheck existingCheck = affordabilityCheckDAO.findById(affordabilityCheck.getId());
        if (existingCheck == null) {
            throw new ValidationException(
                    "No active affordability check found with id " + affordabilityCheck.getId());
        }

        return affordabilityCheckDAO.update(affordabilityCheck);
    }

    @Override
    public boolean softDeleteCheck(UUID id) {
        if (id == null) {
            throw new ValidationException("Affordability check id is required");
        }
        if (affordabilityCheckDAO.findById(id) == null) {
            throw new ValidationException("No active affordability check found with id " + id);
        }
        return affordabilityCheckDAO.softDelete(id);
    }

    /* ---------------- validation ---------------- */

    private void validateAffordabilityCheck(AffordabilityCheck affordabilityCheck) {

        if (affordabilityCheck == null) {
            throw new ValidationException("Affordability check is required");
        }
        if (affordabilityCheck.getUserId() == null) {
            throw new ValidationException("User id is required");
        }
        if (affordabilityCheck.getItemName() == null || affordabilityCheck.getItemName().trim().isEmpty()) {
            throw new ValidationException("Item name is required");
        }
        if (affordabilityCheck.getAmount() == null) {
            throw new ValidationException("Amount is required");
        }
        if (affordabilityCheck.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be greater than zero");
        }
        if (affordabilityCheck.getCheckDate() == null) {
            throw new ValidationException("Check date is required");
        }
        if (affordabilityCheck.getCheckDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Check date cannot be in the future");
        }
    }

    /* ---------------- model to DTO ---------------- */

    private AffordabilityCheckDTO toDTO(AffordabilityCheck affordabilityCheck) {

        AffordabilityCheckDTO affordabilityCheckDTO = new AffordabilityCheckDTO();
        affordabilityCheckDTO.setId(affordabilityCheck.getId());
        affordabilityCheckDTO.setUserId(affordabilityCheck.getUserId());
        affordabilityCheckDTO.setItemName(affordabilityCheck.getItemName());
        affordabilityCheckDTO.setAmount(affordabilityCheck.getAmount());
        affordabilityCheckDTO.setAvailableAmount(affordabilityCheck.getAvailableAmount());
        affordabilityCheckDTO.setVerdict(affordabilityCheck.getVerdict());
        affordabilityCheckDTO.setLevel(affordabilityCheck.getLevel());
        affordabilityCheckDTO.setPriority(affordabilityCheck.getPriority());
        affordabilityCheckDTO.setCheckDate(affordabilityCheck.getCheckDate());
        affordabilityCheckDTO.setCreatedAt(affordabilityCheck.getCreatedAt());
        return affordabilityCheckDTO;
    }

    private List<AffordabilityCheckDTO> toDTOList(List<AffordabilityCheck> affordabilityChecks) {
        List<AffordabilityCheckDTO> affordabilityCheckDTOs = new ArrayList<>();
        for (AffordabilityCheck affordabilityCheck : affordabilityChecks) {
            affordabilityCheckDTOs.add(toDTO(affordabilityCheck));
        }
        return affordabilityCheckDTOs;
    }
}
