package com.microvault.finance.controller;

import com.microvault.finance.dto.ReportRequest;
import com.microvault.finance.dto.ReportResponse;
import com.microvault.finance.service.ReportService;
import com.microvault.finance.service.RequestUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Period reports calculated from transactions")
public class ReportController {

    private final ReportService reportService;
    private final RequestUser requestUser;

    public ReportController(ReportService reportService, RequestUser requestUser) {
        this.reportService = reportService;
        this.requestUser = requestUser;
    }

    @GetMapping
    @Operation(summary = "List reports")
    public List<ReportResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return reportService.list(requestUser.requireUser(authorization));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate a report", description = "Totals income and expense transactions between the two dates.")
    public ResponseEntity<ReportResponse> generate(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @Valid @RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reportService.generate(requestUser.requireUser(authorization), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a report")
    public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @PathVariable UUID id) {
        reportService.softDelete(requestUser.requireUser(authorization), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Soft delete every report")
    public ResponseEntity<Void> deleteAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        reportService.softDeleteAll(requestUser.requireUser(authorization));
        return ResponseEntity.noContent().build();
    }
}
