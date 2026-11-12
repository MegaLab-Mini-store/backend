ALTER TABLE users              ALTER COLUMN id TYPE BIGINT;
ALTER TABLE products           ALTER COLUMN id TYPE BIGINT;
ALTER TABLE deliveries         ALTER COLUMN id TYPE BIGINT;
ALTER TABLE delivery_items     ALTER COLUMN id TYPE BIGINT;
ALTER TABLE loyalty_levels     ALTER COLUMN id TYPE BIGINT;
ALTER TABLE clients            ALTER COLUMN id TYPE BIGINT;
ALTER TABLE sales              ALTER COLUMN id TYPE BIGINT;
ALTER TABLE sale_items         ALTER COLUMN id TYPE BIGINT;
ALTER TABLE inventory_movements ALTER COLUMN id TYPE BIGINT;
ALTER TABLE debts              ALTER COLUMN id TYPE BIGINT;
ALTER TABLE debt_payments      ALTER COLUMN id TYPE BIGINT;
ALTER TABLE bonus_transactions ALTER COLUMN id TYPE BIGINT;
ALTER TABLE daily_summaries    ALTER COLUMN id TYPE BIGINT;
ALTER TABLE refresh_tokens     ALTER COLUMN id TYPE BIGINT;
ALTER TABLE loyalty_settings   ALTER COLUMN id TYPE BIGINT;
ALTER TABLE debt_reminders     ALTER COLUMN id TYPE BIGINT;
ALTER TABLE corrections        ALTER COLUMN id TYPE BIGINT;
ALTER TABLE sync_operations    ALTER COLUMN id TYPE BIGINT;
ALTER TABLE cash_day_closures  ALTER COLUMN id TYPE BIGINT;

ALTER TABLE refresh_tokens      ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE deliveries          ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE delivery_items      ALTER COLUMN delivery_id TYPE BIGINT,
ALTER COLUMN product_id TYPE BIGINT;
ALTER TABLE clients             ALTER COLUMN loyalty_level_id TYPE BIGINT;
ALTER TABLE sales               ALTER COLUMN user_id TYPE BIGINT,
ALTER COLUMN client_id TYPE BIGINT;
ALTER TABLE sale_items          ALTER COLUMN sale_id TYPE BIGINT,
ALTER COLUMN product_id TYPE BIGINT;
ALTER TABLE inventory_movements ALTER COLUMN product_id TYPE BIGINT,
ALTER COLUMN reference_id TYPE BIGINT;
ALTER TABLE debts               ALTER COLUMN client_id TYPE BIGINT,
ALTER COLUMN sale_id TYPE BIGINT;
ALTER TABLE debt_payments       ALTER COLUMN debt_id TYPE BIGINT;
ALTER TABLE debt_reminders      ALTER COLUMN debt_id TYPE BIGINT;
ALTER TABLE bonus_transactions  ALTER COLUMN client_id TYPE BIGINT,
ALTER COLUMN sale_id TYPE BIGINT;
ALTER TABLE corrections         ALTER COLUMN user_id TYPE BIGINT,
ALTER COLUMN sale_id TYPE BIGINT,
                                ALTER COLUMN product_id TYPE BIGINT;
ALTER TABLE sync_operations     ALTER COLUMN server_record_id TYPE BIGINT,
ALTER COLUMN resolved_by TYPE BIGINT;
ALTER TABLE cash_day_closures   ALTER COLUMN daily_summary_id TYPE BIGINT,
ALTER COLUMN closed_by TYPE BIGINT;

CREATE INDEX idx_refresh_tokens_user_id          ON refresh_tokens (user_id);
CREATE INDEX idx_deliveries_user_id              ON deliveries (user_id);
CREATE INDEX idx_delivery_items_delivery_id      ON delivery_items (delivery_id);
CREATE INDEX idx_delivery_items_product_id       ON delivery_items (product_id);
CREATE INDEX idx_clients_loyalty_level_id        ON clients (loyalty_level_id);
CREATE INDEX idx_sales_user_id                   ON sales (user_id);
CREATE INDEX idx_sales_client_id                 ON sales (client_id);
CREATE INDEX idx_sale_items_sale_id              ON sale_items (sale_id);
CREATE INDEX idx_sale_items_product_id           ON sale_items (product_id);
CREATE INDEX idx_inventory_movements_product_id  ON inventory_movements (product_id);
CREATE INDEX idx_debts_client_id                 ON debts (client_id);
CREATE INDEX idx_debt_payments_debt_id           ON debt_payments (debt_id);
CREATE INDEX idx_debt_reminders_debt_id          ON debt_reminders (debt_id);
CREATE INDEX idx_bonus_transactions_client_id    ON bonus_transactions (client_id);
CREATE INDEX idx_bonus_transactions_sale_id      ON bonus_transactions (sale_id);
CREATE INDEX idx_corrections_user_id             ON corrections (user_id);
CREATE INDEX idx_corrections_sale_id             ON corrections (sale_id);
CREATE INDEX idx_corrections_product_id          ON corrections (product_id);
CREATE INDEX idx_sync_operations_resolved_by     ON sync_operations (resolved_by);
CREATE INDEX idx_cash_day_closures_closed_by     ON cash_day_closures (closed_by);

CREATE INDEX idx_sales_occurred_at               ON sales (occurred_at);

ALTER TABLE sales           ALTER COLUMN client_uuid TYPE VARCHAR(64),
ALTER COLUMN replaces_client_uuid TYPE VARCHAR(64);
ALTER TABLE deliveries      ALTER COLUMN client_uuid TYPE VARCHAR(64);
ALTER TABLE debt_payments   ALTER COLUMN client_uuid TYPE VARCHAR(64);
ALTER TABLE corrections     ALTER COLUMN client_uuid TYPE VARCHAR(64);
ALTER TABLE sync_operations ALTER COLUMN client_uuid TYPE VARCHAR(64),
ALTER COLUMN replacement_client_uuid TYPE VARCHAR(64),
                            ALTER COLUMN payload_hash TYPE VARCHAR(64);

ALTER TABLE products
    ADD CONSTRAINT chk_products_purchase_price CHECK (purchase_price >= 0),
    ADD CONSTRAINT chk_products_sale_price     CHECK (sale_price >= 0);

ALTER TABLE sales
    ADD CONSTRAINT chk_sales_total_amount   CHECK (total_amount >= 0),
    ADD CONSTRAINT chk_sales_bonus_spent    CHECK (bonus_spent >= 0),
    ADD CONSTRAINT chk_sales_paid_amount    CHECK (paid_amount >= 0),
    ADD CONSTRAINT chk_sales_earned_bonuses CHECK (earned_bonuses >= 0),
    ADD CONSTRAINT chk_sales_payment_method CHECK (payment_method IN ('CASH', 'CARD', 'DEBT')),
    ADD CONSTRAINT chk_sales_status         CHECK (status IN ('COMPLETED', 'VOIDED')),
    ADD CONSTRAINT chk_sales_client_uuid    CHECK (char_length(client_uuid) >= 8);

ALTER TABLE sale_items
    ADD CONSTRAINT chk_sale_items_quantity   CHECK (quantity > 0),
    ADD CONSTRAINT chk_sale_items_unit_price CHECK (unit_price >= 0),
    ADD CONSTRAINT chk_sale_items_subtotal   CHECK (subtotal = quantity * unit_price);

ALTER TABLE inventory_movements
    ADD CONSTRAINT chk_inventory_movements_quantity_change CHECK (quantity_change <> 0);

ALTER TABLE sync_operations
    ADD CONSTRAINT chk_sync_operations_client_uuid CHECK (char_length(client_uuid) >= 8);

ALTER TABLE sales ALTER COLUMN status SET DEFAULT 'COMPLETED';
