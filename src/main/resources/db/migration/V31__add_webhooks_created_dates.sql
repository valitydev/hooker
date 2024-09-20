ALTER TABLE hook.webhook
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT clock_timestamp();
-- UPDATE hook.webhook
-- SET created_at = NOW() - INTERVAL '2 DAYS';

ALTER TABLE hook.webhook_to_events
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT clock_timestamp();
-- UPDATE hook.webhook_to_events
-- SET created_at = NOW() - INTERVAL '2 DAYS';