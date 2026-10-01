-- StockFlow Phase 5 - Evaluation demonstration data
-- Run after database scripts 01-11 on a development/demo database only.
-- This script never deletes rows. Existing demo keys are preserved on repeat runs.

CREATE DATABASE IF NOT EXISTS stockflow_db;
USE stockflow_db;

START TRANSACTION;

-- -----------------------------------------------------------------------------
-- Function 01: Product catalogue and shared reference data
-- -----------------------------------------------------------------------------
INSERT INTO categories (code, name, description, active) VALUES
    ('FOOD', 'Food', 'Packaged and dry food products', TRUE),
    ('BEVERAGE', 'Beverages', 'Drinks and beverage products', TRUE),
    ('HOUSEHOLD', 'Household', 'Household-use products', TRUE)
ON DUPLICATE KEY UPDATE active = TRUE;

INSERT INTO brands (name, active) VALUES
    ('Lanka Fresh', TRUE),
    ('Island Choice', TRUE),
    ('Daily Home', TRUE)
ON DUPLICATE KEY UPDATE active = TRUE;

INSERT INTO units (code, name, active) VALUES
    ('PACK', 'Pack', TRUE),
    ('BAG', 'Bag', TRUE),
    ('BOTTLE', 'Bottle', TRUE),
    ('UNIT', 'Unit', TRUE)
ON DUPLICATE KEY UPDATE active = TRUE;

SET @food_category_id = (SELECT id FROM categories WHERE code = 'FOOD' LIMIT 1);
SET @beverage_category_id = (SELECT id FROM categories WHERE code = 'BEVERAGE' LIMIT 1);
SET @household_category_id = (SELECT id FROM categories WHERE code = 'HOUSEHOLD' LIMIT 1);
SET @lanka_fresh_brand_id = (SELECT id FROM brands WHERE name = 'Lanka Fresh' LIMIT 1);
SET @island_choice_brand_id = (SELECT id FROM brands WHERE name = 'Island Choice' LIMIT 1);
SET @daily_home_brand_id = (SELECT id FROM brands WHERE name = 'Daily Home' LIMIT 1);
SET @pack_unit_id = (SELECT id FROM units WHERE code = 'PACK' LIMIT 1);
SET @bag_unit_id = (SELECT id FROM units WHERE code = 'BAG' LIMIT 1);
SET @bottle_unit_id = (SELECT id FROM units WHERE code = 'BOTTLE' LIMIT 1);

INSERT INTO products
    (sku, barcode, name, category_id, brand_id, unit_id, description,
     batch_tracked, active, created_at, updated_at)
VALUES
    ('MILK-400G', '4796001000011', 'Milk Powder 400g', @food_category_id,
     @lanka_fresh_brand_id, @pack_unit_id, 'Batch-tracked milk powder for retail distribution',
     TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('RICE-5KG', '4796001000028', 'Premium Rice 5kg', @food_category_id,
     @island_choice_brand_id, @bag_unit_id, 'Five kilogram premium rice bag',
     FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DISH-500ML', '4796001000035', 'Dishwashing Liquid 500ml', @household_category_id,
     @daily_home_brand_id, @bottle_unit_id, 'Household dishwashing liquid',
     FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TEA-200G', '4796001000042', 'Ceylon Tea 200g', @beverage_category_id,
     @lanka_fresh_brand_id, @pack_unit_id, 'Ceylon black tea prepared for reorder demonstration',
     TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), description = VALUES(description), active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

SET @milk_product_id = (SELECT id FROM products WHERE sku = 'MILK-400G' LIMIT 1);
SET @rice_product_id = (SELECT id FROM products WHERE sku = 'RICE-5KG' LIMIT 1);
SET @dish_product_id = (SELECT id FROM products WHERE sku = 'DISH-500ML' LIMIT 1);
SET @tea_product_id = (SELECT id FROM products WHERE sku = 'TEA-200G' LIMIT 1);

-- -----------------------------------------------------------------------------
-- Function 02: Suppliers and purchase orders
-- -----------------------------------------------------------------------------
INSERT INTO suppliers
    (supplier_code, name, contact_person, phone, email, address, active, created_at, updated_at)
VALUES
    ('SUP-DAIRY-01', 'Central Dairy Supplies', 'Kasun Silva', '0771002003',
     'orders@centraldairy.demo', 'Colombo 10', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SUP-GROCERY-01', 'Island Grocery Wholesale', 'Nadeesha Fernando', '0772003004',
     'sales@islandgrocery.demo', 'Wattala', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE name = VALUES(name), active = TRUE, updated_at = CURRENT_TIMESTAMP;

SET @dairy_supplier_id = (SELECT id FROM suppliers WHERE supplier_code = 'SUP-DAIRY-01' LIMIT 1);
SET @grocery_supplier_id = (SELECT id FROM suppliers WHERE supplier_code = 'SUP-GROCERY-01' LIMIT 1);

INSERT INTO purchase_orders
    (order_number, supplier_id, order_date, expected_date, status, notes, created_at, updated_at)
VALUES
    ('PO-DEMO-001', @dairy_supplier_id, DATE_SUB(CURDATE(), INTERVAL 7 DAY),
     DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'PARTIALLY_RECEIVED',
     'Evaluation order with a confirmed partial delivery', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PO-DEMO-002', @grocery_supplier_id, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 5 DAY),
     'APPROVED', 'Approved order waiting for supplier delivery', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE order_number = VALUES(order_number);

SET @po_received_id = (SELECT id FROM purchase_orders WHERE order_number = 'PO-DEMO-001' LIMIT 1);
SET @po_pending_id = (SELECT id FROM purchase_orders WHERE order_number = 'PO-DEMO-002' LIMIT 1);

INSERT INTO purchase_order_items
    (purchase_order_id, product_id, quantity, received_quantity, unit_price)
VALUES
    (@po_received_id, @milk_product_id, 20, 18, 1450.00),
    (@po_received_id, @rice_product_id, 15, 15, 2350.00),
    (@po_received_id, @dish_product_id, 2, 2, 490.00),
    (@po_pending_id, @tea_product_id, 25, 0, 780.00)
ON DUPLICATE KEY UPDATE purchase_order_id = VALUES(purchase_order_id);

SET @milk_po_item_id = (SELECT id FROM purchase_order_items
                        WHERE purchase_order_id = @po_received_id AND product_id = @milk_product_id LIMIT 1);
SET @rice_po_item_id = (SELECT id FROM purchase_order_items
                        WHERE purchase_order_id = @po_received_id AND product_id = @rice_product_id LIMIT 1);
SET @dish_po_item_id = (SELECT id FROM purchase_order_items
                        WHERE purchase_order_id = @po_received_id AND product_id = @dish_product_id LIMIT 1);

-- -----------------------------------------------------------------------------
-- Functions 03 and 04: Receiving, batches, warehouses and transfers
-- -----------------------------------------------------------------------------
INSERT INTO warehouses (code, name, address, active) VALUES
    ('WH-MAIN', 'Main Warehouse', 'Lanka Fresh Distributors Main Facility', TRUE),
    ('WH-NORTH', 'North Distribution Warehouse', 'Kurunegala Distribution Centre', TRUE)
ON DUPLICATE KEY UPDATE address = VALUES(address), active = TRUE;

SET @main_warehouse_id = (SELECT id FROM warehouses WHERE code = 'WH-MAIN' LIMIT 1);
SET @north_warehouse_id = (SELECT id FROM warehouses WHERE code = 'WH-NORTH' LIMIT 1);

INSERT INTO storage_locations (warehouse_id, code, name, active) VALUES
    (@main_warehouse_id, 'RECEIVING', 'Receiving Area', TRUE),
    (@main_warehouse_id, 'RACK-A', 'Storage Rack A', TRUE),
    (@main_warehouse_id, 'RACK-B', 'Storage Rack B', TRUE),
    (@north_warehouse_id, 'NORTH-A', 'North Storage Area A', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name), active = TRUE;

SET @receiving_location_id = (SELECT id FROM storage_locations
                              WHERE warehouse_id = @main_warehouse_id AND code = 'RECEIVING' LIMIT 1);
SET @rack_a_location_id = (SELECT id FROM storage_locations
                           WHERE warehouse_id = @main_warehouse_id AND code = 'RACK-A' LIMIT 1);
SET @rack_b_location_id = (SELECT id FROM storage_locations
                           WHERE warehouse_id = @main_warehouse_id AND code = 'RACK-B' LIMIT 1);
SET @north_a_location_id = (SELECT id FROM storage_locations
                            WHERE warehouse_id = @north_warehouse_id AND code = 'NORTH-A' LIMIT 1);

INSERT INTO batches
    (product_id, batch_number, manufacture_date, expiry_date, status, created_at)
VALUES
    (@milk_product_id, 'MILK-BATCH-DEMO-01', DATE_SUB(CURDATE(), INTERVAL 25 DAY),
     DATE_ADD(CURDATE(), INTERVAL 20 DAY), 'ACTIVE', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE batch_number = VALUES(batch_number);

SET @milk_batch_id = (SELECT id FROM batches
                      WHERE product_id = @milk_product_id AND batch_number = 'MILK-BATCH-DEMO-01' LIMIT 1);

INSERT INTO goods_receipts
    (receipt_number, purchase_order_id, location_id, received_date,
     supplier_delivery_reference, notes, status, confirmed_at, created_at, updated_at)
VALUES
    ('GRN-DEMO-001', @po_received_id, @receiving_location_id, DATE_SUB(CURDATE(), INTERVAL 1 DAY),
     'DEL-DEMO-001', 'Confirmed receipt containing accepted and rejected quantities',
     'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE receipt_number = VALUES(receipt_number);

SET @receipt_id = (SELECT id FROM goods_receipts WHERE receipt_number = 'GRN-DEMO-001' LIMIT 1);

INSERT INTO goods_receipt_items
    (goods_receipt_id, purchase_order_item_id, product_id, batch_id,
     delivered_quantity, accepted_quantity, rejected_quantity, rejection_reason,
     batch_number, manufacture_date, expiry_date)
VALUES
    (@receipt_id, @milk_po_item_id, @milk_product_id, @milk_batch_id,
     20, 18, 2, 'Two packs damaged during delivery', 'MILK-BATCH-DEMO-01',
     DATE_SUB(CURDATE(), INTERVAL 25 DAY), DATE_ADD(CURDATE(), INTERVAL 20 DAY)),
    (@receipt_id, @rice_po_item_id, @rice_product_id, NULL,
     15, 15, 0, NULL, NULL, NULL, NULL),
    (@receipt_id, @dish_po_item_id, @dish_product_id, NULL,
     2, 2, 0, NULL, NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE goods_receipt_id = VALUES(goods_receipt_id);

INSERT INTO stock_transfers
    (transfer_number, source_location_id, destination_location_id, transfer_date,
     status, notes, dispatched_at, received_at, created_at, updated_at)
VALUES
    ('TRF-DEMO-001', @receiving_location_id, @rack_a_location_id,
     CURDATE(), 'RECEIVED', 'Completed placement of received milk stock',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TRF-DEMO-003', @receiving_location_id, @rack_b_location_id,
     CURDATE(), 'RECEIVED', 'Completed placement of rice and household stock',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TRF-DEMO-002', @rack_b_location_id, @north_a_location_id,
     CURDATE(), 'DRAFT', 'Draft inter-warehouse transfer for evaluation',
     NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE transfer_number = VALUES(transfer_number);

SET @completed_transfer_id = (SELECT id FROM stock_transfers WHERE transfer_number = 'TRF-DEMO-001' LIMIT 1);
SET @rack_b_transfer_id = (SELECT id FROM stock_transfers WHERE transfer_number = 'TRF-DEMO-003' LIMIT 1);
SET @draft_transfer_id = (SELECT id FROM stock_transfers WHERE transfer_number = 'TRF-DEMO-002' LIMIT 1);

INSERT INTO stock_transfer_items (stock_transfer_id, product_id, batch_id, quantity) VALUES
    (@completed_transfer_id, @milk_product_id, @milk_batch_id, 10),
    (@rack_b_transfer_id, @rice_product_id, NULL, 12),
    (@rack_b_transfer_id, @dish_product_id, NULL, 2),
    (@draft_transfer_id, @rice_product_id, NULL, 4)
ON DUPLICATE KEY UPDATE stock_transfer_id = VALUES(stock_transfer_id);

-- Current balances are inserted only when the exact product/location/batch row does not exist.
INSERT INTO inventory_balances
    (product_id, location_id, batch_id, quantity_available, version, updated_at)
SELECT @milk_product_id, @receiving_location_id, @milk_batch_id, 8, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM inventory_balances
                  WHERE product_id = @milk_product_id AND location_id = @receiving_location_id
                    AND batch_id = @milk_batch_id);

INSERT INTO inventory_balances
    (product_id, location_id, batch_id, quantity_available, version, updated_at)
SELECT @milk_product_id, @rack_a_location_id, @milk_batch_id, 7, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM inventory_balances
                  WHERE product_id = @milk_product_id AND location_id = @rack_a_location_id
                    AND batch_id = @milk_batch_id);

INSERT INTO inventory_balances
    (product_id, location_id, batch_id, quantity_available, version, updated_at)
SELECT @rice_product_id, @receiving_location_id, NULL, 3, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM inventory_balances
                  WHERE product_id = @rice_product_id AND location_id = @receiving_location_id
                    AND batch_id IS NULL);

INSERT INTO inventory_balances
    (product_id, location_id, batch_id, quantity_available, version, updated_at)
SELECT @rice_product_id, @rack_b_location_id, NULL, 9, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM inventory_balances
                  WHERE product_id = @rice_product_id AND location_id = @rack_b_location_id
                    AND batch_id IS NULL);

INSERT INTO inventory_balances
    (product_id, location_id, batch_id, quantity_available, version, updated_at)
SELECT @dish_product_id, @rack_b_location_id, NULL, 4, 0, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM inventory_balances
                  WHERE product_id = @dish_product_id AND location_id = @rack_b_location_id
                    AND batch_id IS NULL);

-- -----------------------------------------------------------------------------
-- Function 05: Stock issue and return
-- -----------------------------------------------------------------------------
INSERT INTO recipients
    (code, name, recipient_type, contact_name, phone, email, address, active, created_at, updated_at)
VALUES
    ('CUS-001', 'Green Mart Colombo', 'CUSTOMER', 'Nimal Perera', '0771234567',
     'orders@greenmart.demo', 'Colombo 05', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE name = VALUES(name), active = TRUE, updated_at = CURRENT_TIMESTAMP;

SET @recipient_id = (SELECT id FROM recipients WHERE code = 'CUS-001' LIMIT 1);

INSERT INTO stock_issues
    (issue_number, recipient_id, issue_date, external_reference, notes, status,
     confirmed_at, created_at, updated_at)
VALUES
    ('ISS-DEMO-001', @recipient_id, CURDATE(), 'ORDER-DEMO-001',
     'Confirmed customer issue with a partial return', 'PARTIALLY_RETURNED',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE issue_number = VALUES(issue_number);

SET @issue_id = (SELECT id FROM stock_issues WHERE issue_number = 'ISS-DEMO-001' LIMIT 1);

INSERT INTO stock_issue_items
    (stock_issue_id, product_id, location_id, batch_id, quantity, returned_quantity)
VALUES
    (@issue_id, @milk_product_id, @rack_a_location_id, @milk_batch_id, 4, 1),
    (@issue_id, @rice_product_id, @rack_b_location_id, NULL, 3, 0)
ON DUPLICATE KEY UPDATE stock_issue_id = VALUES(stock_issue_id);

SET @milk_issue_item_id = (SELECT id FROM stock_issue_items
                           WHERE stock_issue_id = @issue_id AND product_id = @milk_product_id LIMIT 1);

INSERT INTO stock_returns
    (return_number, stock_issue_id, return_date, notes, status, confirmed_at, created_at, updated_at)
VALUES
    ('RET-DEMO-001', @issue_id, CURDATE(), 'One unopened milk pack returned to stock',
     'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE return_number = VALUES(return_number);

SET @return_id = (SELECT id FROM stock_returns WHERE return_number = 'RET-DEMO-001' LIMIT 1);

INSERT INTO stock_return_items
    (stock_return_id, issue_item_id, returned_quantity, restock_quantity,
     rejected_quantity, return_reason, condition_note)
VALUES
    (@return_id, @milk_issue_item_id, 1, 1, 0, 'Customer order quantity corrected',
     'Unopened pack inspected and accepted')
ON DUPLICATE KEY UPDATE stock_return_id = VALUES(stock_return_id);

-- -----------------------------------------------------------------------------
-- Function 06: Reordering, physical count and adjustment
-- -----------------------------------------------------------------------------
INSERT INTO reorder_rules
    (product_id, minimum_level, maximum_level, reorder_quantity, active, created_at, updated_at)
VALUES
    (@milk_product_id, 8, 40, 25, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@rice_product_id, 10, 35, 20, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@dish_product_id, 5, 20, 16, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@tea_product_id, 5, 30, 25, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE minimum_level = VALUES(minimum_level),
    maximum_level = VALUES(maximum_level), reorder_quantity = VALUES(reorder_quantity), active = TRUE;

SET @dish_balance_id = (SELECT id FROM inventory_balances
                        WHERE product_id = @dish_product_id AND location_id = @rack_b_location_id
                          AND batch_id IS NULL LIMIT 1);

INSERT INTO stock_counts
    (count_number, location_id, count_date, notes, status,
     submitted_at, closed_at, created_at, updated_at)
VALUES
    ('COUNT-DEMO-001', @rack_b_location_id, CURDATE(),
     'Closed demonstration count that identified two extra units', 'CLOSED',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE count_number = VALUES(count_number);

SET @count_id = (SELECT id FROM stock_counts WHERE count_number = 'COUNT-DEMO-001' LIMIT 1);

INSERT INTO stock_count_items
    (stock_count_id, inventory_balance_id, system_quantity, counted_quantity, variance_note)
VALUES
    (@count_id, @dish_balance_id, 2, 4, 'Two units located during physical count')
ON DUPLICATE KEY UPDATE stock_count_id = VALUES(stock_count_id);

INSERT INTO stock_adjustments
    (adjustment_number, inventory_balance_id, stock_count_id, quantity_change,
     reason, notes, status, submitted_at, reviewed_at, created_at, updated_at)
VALUES
    ('ADJ-DEMO-001', @dish_balance_id, @count_id, 2, 'Physical count variance',
     'Approved demonstration adjustment supported by COUNT-DEMO-001', 'APPLIED',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE adjustment_number = VALUES(adjustment_number);

-- -----------------------------------------------------------------------------
-- Connected stock movement evidence. Each reference is inserted once only.
-- -----------------------------------------------------------------------------
INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'GOODS_RECEIPT', @milk_product_id, @receiving_location_id, @milk_batch_id, 18,
       'GRN-DEMO-001-MILK', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'GRN-DEMO-001-MILK');

INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'GOODS_RECEIPT', @rice_product_id, @receiving_location_id, NULL, 15,
       'GRN-DEMO-001-RICE', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'GRN-DEMO-001-RICE');

INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'GOODS_RECEIPT', @dish_product_id, @receiving_location_id, NULL, 2,
       'GRN-DEMO-001-DISH', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'GRN-DEMO-001-DISH');

INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'TRANSFER_OUT', @milk_product_id, @receiving_location_id, @milk_batch_id, -10,
       'TRF-DEMO-001-OUT', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 12 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'TRF-DEMO-001-OUT');

INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'TRANSFER_IN', @milk_product_id, @rack_a_location_id, @milk_batch_id, 10,
       'TRF-DEMO-001-IN', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 12 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'TRF-DEMO-001-IN');

INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'TRANSFER_OUT', @rice_product_id, @receiving_location_id, NULL, -12,
       'TRF-DEMO-003-RICE-OUT', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 10 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'TRF-DEMO-003-RICE-OUT');

INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'TRANSFER_IN', @rice_product_id, @rack_b_location_id, NULL, 12,
       'TRF-DEMO-003-RICE-IN', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 10 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'TRF-DEMO-003-RICE-IN');

INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'TRANSFER_OUT', @dish_product_id, @receiving_location_id, NULL, -2,
       'TRF-DEMO-003-DISH-OUT', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 10 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'TRF-DEMO-003-DISH-OUT');

INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'TRANSFER_IN', @dish_product_id, @rack_b_location_id, NULL, 2,
       'TRF-DEMO-003-DISH-IN', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 10 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'TRF-DEMO-003-DISH-IN');

INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'STOCK_ISSUE', @milk_product_id, @rack_a_location_id, @milk_batch_id, -4,
       'ISS-DEMO-001-MILK', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 6 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'ISS-DEMO-001-MILK');

INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'STOCK_ISSUE', @rice_product_id, @rack_b_location_id, NULL, -3,
       'ISS-DEMO-001-RICE', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 6 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'ISS-DEMO-001-RICE');

INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'STOCK_RETURN', @milk_product_id, @rack_a_location_id, @milk_batch_id, 1,
       'RET-DEMO-001-MILK', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'RET-DEMO-001-MILK');

INSERT INTO stock_movements
    (movement_type, product_id, location_id, batch_id, quantity_change, reference_number, created_at)
SELECT 'ADJUSTMENT', @dish_product_id, @rack_b_location_id, NULL, 2,
       'ADJ-DEMO-001-DISH', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM stock_movements WHERE reference_number = 'ADJ-DEMO-001-DISH');

COMMIT;

-- Evaluation verification: all queries should execute without an error.
SELECT 'Products' AS area, COUNT(*) AS demo_rows FROM products WHERE sku LIKE '%-DEMO-%' OR sku IN ('MILK-400G','RICE-5KG','DISH-500ML','TEA-200G')
UNION ALL SELECT 'Purchase orders', COUNT(*) FROM purchase_orders WHERE order_number LIKE '%-DEMO-%'
UNION ALL SELECT 'Goods receipts', COUNT(*) FROM goods_receipts WHERE receipt_number LIKE '%-DEMO-%'
UNION ALL SELECT 'Stock transfers', COUNT(*) FROM stock_transfers WHERE transfer_number LIKE '%-DEMO-%'
UNION ALL SELECT 'Stock issues', COUNT(*) FROM stock_issues WHERE issue_number LIKE '%-DEMO-%'
UNION ALL SELECT 'Stock returns', COUNT(*) FROM stock_returns WHERE return_number LIKE '%-DEMO-%'
UNION ALL SELECT 'Stock counts', COUNT(*) FROM stock_counts WHERE count_number LIKE '%-DEMO-%'
UNION ALL SELECT 'Stock adjustments', COUNT(*) FROM stock_adjustments WHERE adjustment_number LIKE '%-DEMO-%';

SELECT product.sku, product.name,
       COALESCE(SUM(balance.quantity_available), 0) AS current_quantity,
       rule.minimum_level, rule.maximum_level,
       CASE
           WHEN COALESCE(SUM(balance.quantity_available), 0) = 0 THEN 'OUT OF STOCK'
           WHEN COALESCE(SUM(balance.quantity_available), 0) <= rule.minimum_level THEN 'LOW STOCK'
           ELSE 'NORMAL'
       END AS evaluation_status
FROM products product
JOIN reorder_rules rule ON rule.product_id = product.id AND rule.active = TRUE
LEFT JOIN inventory_balances balance ON balance.product_id = product.id
WHERE product.sku IN ('MILK-400G','RICE-5KG','DISH-500ML','TEA-200G')
GROUP BY product.id, product.sku, product.name, rule.minimum_level, rule.maximum_level
ORDER BY product.sku;
