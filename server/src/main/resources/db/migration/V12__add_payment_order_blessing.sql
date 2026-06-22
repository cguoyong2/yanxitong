ALTER TABLE payment_order
    ADD COLUMN blessing VARCHAR(500) NULL AFTER payer_open_id;
