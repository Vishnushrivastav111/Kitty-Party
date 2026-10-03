package com.microvault.finance.repository;

import com.microvault.finance.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId);
    Optional<Report> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
}
