package com.microvault.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class ApprovalRequestCreate {
    @NotBlank
    private String requestType;
    @NotNull
    private UUID resourceId;
    private String reason;

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
