-- StockFlow safe permanent Delete - demonstration queries
-- READ-ONLY: every statement below is a SELECT. Nothing is changed.
-- No migration is needed for the Delete feature: no tables, columns,
-- foreign keys or cascade rules were added or changed.
--
-- How to use in MySQL Workbench:
--   1. Run STEP 1 to see which rows are eligible for Delete.
--   2. In StockFlow, create a new test record (or pick an eligible one).
--   3. Change the value in STEP 2 and run STEP 2 + STEP 3 BEFORE deleting.
--   4. Click Delete in StockFlow and confirm the dialog.
--   5. Run STEP 2 + STEP 3 again: the row is gone, history counts are unchanged.

USE stockflow_db;

-- =====================================================================
-- STEP 1 - Rows that StockFlow will offer to delete
-- =====================================================================

-- Products never used by any other record
SELECT p.id, p.sku, p.name, p.active
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM purchase_order_items x WHERE x.product_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM goods_receipt_items x WHERE x.product_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM batches x WHERE x.product_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM inventory_balances x WHERE x.product_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM stock_movements x WHERE x.product_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM stock_transfer_items x WHERE x.product_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM stock_issue_items x WHERE x.product_id = p.id)
  AND NOT EXISTS (SELECT 1 FROM reorder_rules x WHERE x.product_id = p.id);

-- Categories, brands and units used by no product
SELECT 'category' AS kind, c.id, c.name FROM categories c
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.category_id = c.id)
UNION ALL
SELECT 'brand', b.id, b.name FROM brands b
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.brand_id = b.id)
UNION ALL
SELECT 'unit', u.id, u.name FROM units u
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.unit_id = u.id);

-- Suppliers with no purchase orders
SELECT s.id, s.supplier_code, s.name, s.active
FROM suppliers s
WHERE NOT EXISTS (SELECT 1 FROM purchase_orders po WHERE po.supplier_id = s.id);

-- Draft purchase orders with no goods receipt, and how many item lines each has
SELECT po.id, po.order_number, po.status, COUNT(poi.id) AS item_lines
FROM purchase_orders po
LEFT JOIN purchase_order_items poi ON poi.purchase_order_id = po.id
WHERE po.status = 'DRAFT'
  AND NOT EXISTS (SELECT 1 FROM goods_receipts gr WHERE gr.purchase_order_id = po.id)
GROUP BY po.id, po.order_number, po.status;

-- Warehouses with no storage locations
SELECT w.id, w.code, w.name
FROM warehouses w
WHERE NOT EXISTS (SELECT 1 FROM storage_locations l WHERE l.warehouse_id = w.id);

-- Storage locations never used by stock or a document
SELECT l.id, l.code, l.name
FROM storage_locations l
WHERE NOT EXISTS (SELECT 1 FROM inventory_balances x WHERE x.location_id = l.id)
  AND NOT EXISTS (SELECT 1 FROM stock_movements x WHERE x.location_id = l.id)
  AND NOT EXISTS (SELECT 1 FROM goods_receipts x WHERE x.location_id = l.id)
  AND NOT EXISTS (SELECT 1 FROM stock_transfers x
                  WHERE x.source_location_id = l.id OR x.destination_location_id = l.id)
  AND NOT EXISTS (SELECT 1 FROM stock_issue_items x WHERE x.location_id = l.id)
  AND NOT EXISTS (SELECT 1 FROM stock_counts x WHERE x.location_id = l.id);

-- Recipients with no stock issues
SELECT r.id, r.code, r.name, r.active
FROM recipients r
WHERE NOT EXISTS (SELECT 1 FROM stock_issues i WHERE i.recipient_id = r.id);

-- Draft documents (only drafts can be deleted)
SELECT 'goods receipt' AS document, receipt_number AS number, status FROM goods_receipts WHERE status = 'DRAFT'
UNION ALL SELECT 'stock transfer', transfer_number, status FROM stock_transfers WHERE status = 'DRAFT'
UNION ALL SELECT 'stock issue', issue_number, status FROM stock_issues WHERE status = 'DRAFT'
UNION ALL SELECT 'stock return', return_number, status FROM stock_returns WHERE status = 'DRAFT'
UNION ALL SELECT 'stock count', count_number, status FROM stock_counts WHERE status = 'DRAFT'
UNION ALL SELECT 'stock adjustment', adjustment_number, status FROM stock_adjustments WHERE status = 'DRAFT';

-- =====================================================================
-- STEP 2 - Watch one record (example: a draft purchase order)
-- Replace PO-2026-0007 with the order number you will delete.
-- =====================================================================

SET @order_number = 'PO-2026-0007';

SELECT id, order_number, status
FROM purchase_orders
WHERE order_number = @order_number;

SELECT poi.id, poi.purchase_order_id, poi.product_id, poi.quantity
FROM purchase_order_items poi
JOIN purchase_orders po ON po.id = poi.purchase_order_id
WHERE po.order_number = @order_number;

-- Before Delete: one order row and its item rows are shown.
-- After Delete:  both queries return 0 rows.

-- =====================================================================
-- STEP 3 - History is never removed
-- Run before and after the delete; the numbers must be identical.
-- =====================================================================

SELECT
    (SELECT COUNT(*) FROM stock_movements)    AS stock_movements,
    (SELECT COUNT(*) FROM inventory_balances) AS inventory_balances,
    (SELECT COUNT(*) FROM products)           AS products,
    (SELECT COUNT(*) FROM suppliers)          AS suppliers;
