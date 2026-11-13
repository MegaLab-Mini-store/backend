ALTER TABLE inventory_movements
    ADD COLUMN IF NOT EXISTS quantity_after integer;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_products_stock_non_negative') THEN
        ALTER TABLE products
            ADD CONSTRAINT chk_products_stock_non_negative CHECK (stock_quantity >= 0);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_inventory_movements_operation
    ON inventory_movements (operation_type, reference_id, product_id)
    WHERE reference_id IS NOT NULL;