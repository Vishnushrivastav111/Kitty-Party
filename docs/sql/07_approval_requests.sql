-- Approval requests submitted by users and reviewed by administrators.
CREATE TABLE IF NOT EXISTS approval_requests (
    id           UUID PRIMARY KEY,
    requester_id UUID NOT NULL,
    request_type VARCHAR(50) NOT NULL,
    resource_id  UUID NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'pending',
    reason       VARCHAR(500),
    reviewer_id  UUID,
    reviewed_at TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted   BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_approval_requests_status_created
    ON approval_requests(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_approval_requests_requester
    ON approval_requests(requester_id, created_at DESC);
