ALTER TABLE notification_deliveries
    ADD COLUMN claimed_at TIMESTAMP(6) WITHOUT TIME ZONE;

ALTER TABLE notification_deliveries
    ADD COLUMN sent_at TIMESTAMP(6) WITHOUT TIME ZONE;
