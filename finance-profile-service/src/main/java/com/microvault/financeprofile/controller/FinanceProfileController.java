package com.microvault.financeprofile.controller;

import com.microvault.financeprofile.dto.FinanceProfileRequest;
import com.microvault.financeprofile.dto.FinanceProfileResponse;
import com.microvault.financeprofile.dto.MonthlySummaryResponse;
import com.microvault.financeprofile.dto.SurplusResponse;
import com.microvault.financeprofile.service.FinanceProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance-profiles")
public class FinanceProfileController {

    private final FinanceProfileService financeProfileService;

    public FinanceProfileController(FinanceProfileService financeProfileService) {
        this.financeProfileService = financeProfileService;
    }

    @PostMapping
    public ResponseEntity<FinanceProfileResponse> createProfile(@RequestBody FinanceProfileRequest request) {
        FinanceProfileResponse saved = financeProfileService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<FinanceProfileResponse> getAllProfiles() {
        return financeProfileService.getAllProfiles();
    }

    @GetMapping("/{id}")
    public FinanceProfileResponse getProfileById(@PathVariable UUID id) {
        return financeProfileService.getProfileById(id);
    }

    @GetMapping("/user/{userId}")
    public FinanceProfileResponse getProfileByUserId(@PathVariable UUID userId) {
        return financeProfileService.getProfileByUserId(userId);
    }

    @GetMapping("/user/{userId}/surplus")
    public SurplusResponse getMonthlySurplus(@PathVariable UUID userId) {
        return financeProfileService.getMonthlySurplus(userId);
    }

    @GetMapping("/user/{userId}/summary")
    public MonthlySummaryResponse getMonthlySummary(@PathVariable UUID userId) {
        return financeProfileService.getMonthlySummary(userId);
    }

    @PutMapping("/{id}")
    public FinanceProfileResponse updateProfile(@PathVariable UUID id,
                                                 @RequestBody FinanceProfileRequest request) {
        return financeProfileService.updateProfile(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable UUID id) {
        financeProfileService.softDeleteProfile(id);
        return ResponseEntity.noContent().build();
    }
}
