CREATE INDEX idx_notifications_recipient_id_id
    ON notifications (recipient_id, id DESC);
