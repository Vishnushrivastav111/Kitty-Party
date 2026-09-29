package com.microvault.dashboard.repository;

import com.microvault.dashboard.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByIdAndDeletedFalse(UUID id);

    Optional<Member> findByEmailIgnoreCaseAndDeletedFalse(String email);
}
