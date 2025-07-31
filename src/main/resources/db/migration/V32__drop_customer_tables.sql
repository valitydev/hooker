DROP TABLE IF EXISTS hook.customer_message;
DROP INDEX IF EXISTS customer_message_customer_id_idx, customer_message_uniq_idx, customer_message_event_id_idx, customer_queue_customer_id_id, customer_queue_wtime_idx;
DROP TYPE IF EXISTS hook.customer_message_type, hook.customer_status, hook.customer_binding_status, hook.payment_payer_type, hook.payment_tool_details_type;

-- rename the old enum
ALTER TYPE hook.EventType rename TO OldEventType;
-- create the new enum
CREATE TYPE hook.EventType AS ENUM (
    'INVOICE_CREATED',
    'INVOICE_STATUS_CHANGED',
    'INVOICE_PAYMENT_STARTED',
    'INVOICE_PAYMENT_STATUS_CHANGED',
    'INVOICE_PAYMENT_CASH_CHANGED',
    'INVOICE_PAYMENT_CASH_FLOW_CHANGED',
    'INVOICE_PAYMENT_REFUND_STARTED',
    'INVOICE_PAYMENT_REFUND_STATUS_CHANGED',
    'INVOICE_PAYMENT_USER_INTERACTION_CHANGE_REQUESTED',
    'INVOICE_PAYMENT_USER_INTERACTION_CHANGE_COMPLETED'

    );

ALTER TABLE hook.webhook_to_events
ALTER COLUMN event_type TYPE hook.EventType USING event_type::text::hook.EventType;

ALTER TABLE hook.message
ALTER COLUMN event_type TYPE hook.EventType USING event_type::text::hook.EventType;

-- drop the old enum
DROP TYPE hook.OldEventType;