ALTER TABLE hook.webhook ADD COLUMN created_at TIMESTAMP DEFAULT NOW();
UPDATE hook.webhook SET created_at = NOW() - INTERVAL '2 DAYS';

ALTER TABLE hook.webhook_to_events ADD COLUMN created_at TIMESTAMP DEFAULT NOW();
UPDATE hook.webhook_to_events SET created_at = NOW() - INTERVAL '2 DAYS';