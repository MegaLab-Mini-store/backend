-- Продажа может увести остаток в минус (офлайн-продажа уже прошла, товар отдан).
-- Для приёмки и корректировок запрет на минус остаётся в StockService.
ALTER TABLE products DROP CONSTRAINT IF EXISTS chk_products_stock_non_negative;
