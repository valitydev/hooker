ALTER TABLE hook.webhook ADD COLUMN created_at TIMESTAMP;
UPDATE hook.webhook SET created_at = NOW() - INTERVAL '2 DAYS';
ALTER TABLE hook.webhook ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE hook.webhook_to_events ADD COLUMN created_at TIMESTAMP;
UPDATE hook.webhook_to_events SET created_at = NOW() - INTERVAL '2 DAYS';
ALTER TABLE hook.webhook_to_events ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;