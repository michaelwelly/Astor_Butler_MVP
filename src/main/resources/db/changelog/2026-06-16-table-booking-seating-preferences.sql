ALTER TABLE table_reservation_orders
    ADD COLUMN IF NOT EXISTS preferred_zone VARCHAR(40),
    ADD COLUMN IF NOT EXISTS seating_preference TEXT;

CREATE INDEX IF NOT EXISTS idx_table_reservation_orders_preferred_zone
    ON table_reservation_orders(preferred_zone, requested_start_at)
    WHERE preferred_zone IS NOT NULL;
