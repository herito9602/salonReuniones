-- Optimistic locking: concurrent updates of the same reservation (e.g. two cancels) are detected.
ALTER TABLE reservations ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
