ALTER TABLE payment_order
    ADD UNIQUE KEY uk_payment_provider_trade_no (provider, provider_trade_no);

ALTER TABLE gift_record
    ADD UNIQUE KEY uk_gift_payment_order_once (payment_order_id);

ALTER TABLE favor_entry
    ADD UNIQUE KEY uk_favor_gift_record_once (gift_record_id);
