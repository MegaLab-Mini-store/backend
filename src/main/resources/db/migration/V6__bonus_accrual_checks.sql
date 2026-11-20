ALTER TABLE loyalty_levels
    ADD CONSTRAINT chk_loyalty_levels_cashback_percentage CHECK (cashback_percentage BETWEEN 0 AND 10000);

ALTER TABLE clients
    ADD CONSTRAINT chk_clients_bonus_balance CHECK (bonus_balance >= 0);

ALTER TABLE bonus_transactions
    ADD CONSTRAINT chk_bonus_transactions_amount         CHECK (amount <> 0),
    ADD CONSTRAINT chk_bonus_transactions_operation_type CHECK (operation_type IN ('ACCRUAL')),
    ADD CONSTRAINT chk_bonus_transactions_percent        CHECK (percent BETWEEN 0 AND 10000),
    ADD CONSTRAINT chk_bonus_transactions_base_amount    CHECK (base_amount >= 0);

CREATE UNIQUE INDEX uq_bonus_transactions_sale_operation
    ON bonus_transactions (sale_id, operation_type);
