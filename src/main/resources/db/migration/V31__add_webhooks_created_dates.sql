ALTER TABLE hook.webhook ADD COLUMN created_at TIMESTAMP DEFAULT NOW() AT TIME ZONE 'UTC';
ALTER TABLE hook.webhook_to_events ADD COLUMN created_at TIMESTAMP DEFAULT NOW() AT TIME ZONE 'UTC';