package com.microvault.admin.repository;

import com.microvault.admin.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NewsRepository extends JpaRepository<News, UUID> {
    List<News> findByDeletedFalseOrderByCreatedAtDesc();
    List<News> findByStatusIgnoreCaseAndDeletedFalseOrderByCreatedAtDesc(String status);
    Optional<News> findByIdAndDeletedFalse(UUID id);
}
