CREATE OR REPLACE FUNCTION prevent_inventory_movements_change()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'inventory_movements is append-only: % is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_inventory_movements_immutable
    BEFORE UPDATE OR DELETE ON inventory_movements
    FOR EACH ROW EXECUTE FUNCTION prevent_inventory_movements_change();