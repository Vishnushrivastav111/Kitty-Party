package com.microvault.finance.controller;

import com.microvault.finance.dto.FinanceProfileRequest;
import com.microvault.finance.dto.FinanceProfileResponse;
import com.microvault.finance.dto.FinanceWorkspaceResponse;
import com.microvault.finance.dto.MonthlySummaryResponse;
import com.microvault.finance.exception.UnauthorizedException;
import com.microvault.finance.service.FinanceProfileService;
import com.microvault.finance.service.RequestUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance")
@Tag(name = "Finance profile", description = "Income, expenses and the member financial profile")
public class FinanceProfileController {

    private final FinanceProfileService financeProfileService;
    private final RequestUser requestUser;

    public FinanceProfileController(FinanceProfileService financeProfileService, RequestUser requestUser) {
        this.financeProfileService = financeProfileService;
        this.requestUser = requestUser;
    }

    @PutMapping
    @Operation(summary = "Save financial profile", description = "Creates the profile when the member does not have one, otherwise updates it.")
    public FinanceProfileResponse save(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody FinanceProfileRequest request) {
        return financeProfileService.saveForUser(requestUser.requireUser(authorization), request);
    }

    @PostMapping("/skip")
    @Operation(summary = "Skip financial setup", description = "Remembers that the member chose to finish setup later.")
    public ResponseEntity<Void> skip(@RequestHeader(value = "Authorization", required = false) String authorization) {
        financeProfileService.skipSetup(requestUser.requireUser(authorization));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get a financial profile")
    public FinanceProfileResponse get(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable UUID userId) {
        requireOwnAccount(authorization, userId);
        return financeProfileService.getByUserId(userId);
    }

    @GetMapping("/profile/{userId}/summary")
    @Operation(summary = "Monthly summary", description = "Income, expenses, EMI, surplus and savings rate.")
    public MonthlySummaryResponse summary(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @PathVariable UUID userId) {
        requireOwnAccount(authorization, userId);
        return financeProfileService.summary(userId);
    }

    @GetMapping("/workspace")
    @Operation(summary = "Finance workspace", description = "Profile plus transactions, goals, savings, budgets, reports and affordability checks for the dashboard.")
    public FinanceWorkspaceResponse workspace(@RequestParam UUID userId,
                                              @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        requestUser.requireInternal(token);
        return financeProfileService.workspace(userId);
    }

    @GetMapping("/profiles/active-user-ids")
    @Operation(summary = "Users who finished setup")
    public List<UUID> activeUserIds(@RequestHeader(value = "X-Internal-Token", required = false) String token) {
        requestUser.requireInternal(token);
        return financeProfileService.activeUserIds();
    }

    private void requireOwnAccount(String authorization, UUID userId) {
        UUID caller = requestUser.requireUser(authorization);
        if (!caller.equals(userId)) {
            throw new UnauthorizedException("You can only access your own account");
        }
    }
}
