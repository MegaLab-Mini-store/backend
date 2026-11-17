ALTER TABLE deliveries
    ADD CONSTRAINT chk_deliveries_total_amount CHECK (total_amount >= 0);

ALTER TABLE delivery_items
    ADD CONSTRAINT chk_delivery_items_quantity CHECK (quantity > 0),
    ADD CONSTRAINT chk_delivery_items_purchase_price CHECK (purchase_price >= 0);
