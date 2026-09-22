package com.microvault.support;

import com.microvault.dao.AffordabilityCheckDAO;
import com.microvault.model.AffordabilityCheck;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InMemoryAffordabilityCheckDAO implements AffordabilityCheckDAO {

    private final List<AffordabilityCheck> checks = new ArrayList<>();

    @Override
    public AffordabilityCheck create(AffordabilityCheck affordabilityCheck) {
        if (affordabilityCheck.getId() == null) {
            affordabilityCheck.setId(UUID.randomUUID());
        }
        affordabilityCheck.setCreatedAt(LocalDateTime.now());
        affordabilityCheck.setUpdatedAt(LocalDateTime.now());
        affordabilityCheck.setIsDeleted(Boolean.FALSE);
        checks.add(copy(affordabilityCheck));
        return copy(affordabilityCheck);
    }

    @Override
    public AffordabilityCheck findById(UUID id) {
        for (AffordabilityCheck check : checks) {
            if (id.equals(check.getId()) && !Boolean.TRUE.equals(check.getIsDeleted())) {
                return copy(check);
            }
        }
        return null;
    }

    @Override
    public List<AffordabilityCheck> findAll() {
        List<AffordabilityCheck> result = new ArrayList<>();
        for (AffordabilityCheck check : checks) {
            if (!Boolean.TRUE.equals(check.getIsDeleted())) {
                result.add(copy(check));
            }
        }
        return result;
    }

    @Override
    public List<AffordabilityCheck> findByUserId(UUID userId) {
        List<AffordabilityCheck> result = new ArrayList<>();
        for (AffordabilityCheck check : checks) {
            if (!Boolean.TRUE.equals(check.getIsDeleted()) && userId.equals(check.getUserId())) {
                result.add(copy(check));
            }
        }
        return result;
    }

    @Override
    public boolean update(AffordabilityCheck affordabilityCheck) {
        AffordabilityCheck existing = findStored(affordabilityCheck.getId());
        if (existing == null) {
            return false;
        }
        existing.setItemName(affordabilityCheck.getItemName());
        existing.setAmount(affordabilityCheck.getAmount());
        existing.setAvailableAmount(affordabilityCheck.getAvailableAmount());
        existing.setVerdict(affordabilityCheck.getVerdict());
        existing.setLevel(affordabilityCheck.getLevel());
        existing.setPriority(affordabilityCheck.getPriority());
        existing.setCheckDate(affordabilityCheck.getCheckDate());
        existing.setUpdatedAt(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean softDelete(UUID id) {
        AffordabilityCheck existing = findStored(id);
        if (existing == null) {
            return false;
        }
        existing.setIsDeleted(Boolean.TRUE);
        existing.setDeletedAt(LocalDateTime.now());
        return true;
    }

    private AffordabilityCheck findStored(UUID id) {
        for (AffordabilityCheck check : checks) {
            if (id.equals(check.getId()) && !Boolean.TRUE.equals(check.getIsDeleted())) {
                return check;
            }
        }
        return null;
    }

    private AffordabilityCheck copy(AffordabilityCheck source) {
        AffordabilityCheck check = new AffordabilityCheck();
        check.setId(source.getId());
        check.setUserId(source.getUserId());
        check.setItemName(source.getItemName());
        check.setAmount(source.getAmount());
        check.setAvailableAmount(source.getAvailableAmount());
        check.setVerdict(source.getVerdict());
        check.setLevel(source.getLevel());
        check.setPriority(source.getPriority());
        check.setCheckDate(source.getCheckDate());
        check.setCreatedAt(source.getCreatedAt());
        check.setUpdatedAt(source.getUpdatedAt());
        check.setIsDeleted(source.getIsDeleted());
        check.setDeletedAt(source.getDeletedAt());
        return check;
    }
}
