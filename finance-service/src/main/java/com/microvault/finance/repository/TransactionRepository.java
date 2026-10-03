package com.microvault.finance.repository;

import com.microvault.finance.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserIdAndDeletedFalseOrderByDateDescCreatedAtDesc(UUID userId);
    Optional<Transaction> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
}
