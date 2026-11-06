
-- V7: Notificaciones e hitos de financiación (HU-15)

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    campaign_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE campaign_milestones (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL,
    percentage INTEGER NOT NULL,
    reached_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_campaign_milestones_campaign_percentage
        UNIQUE (campaign_id, percentage)
);

CREATE INDEX idx_notifications_user_created
    ON notifications (user_id, created_at DESC);

CREATE INDEX idx_campaign_milestones_campaign
    ON campaign_milestones (campaign_id);
