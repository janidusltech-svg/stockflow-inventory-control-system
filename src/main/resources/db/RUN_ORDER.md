# StockFlow MySQL script order

Use a development database and run these files in MySQL Workbench in this order:

1. `schema.sql`
2. `tharinya_purchasing.sql`
3. `04_goods_receiving_dishan.sql`
4. `05_warehouse_transfer_tesara.sql`
5. `06_stock_issue_return_raveesha.sql`
6. `07_inventory_control_janidu.sql`
7. `08_auth_role_access_janidu.sql`
8. `09_reference_data_management_janidu.sql`
9. `10_phase3_shared_features_janidu.sql`
10. `11_phase4_audit_trail_janidu.sql`
11. `12_phase5_evaluation_demo_data.sql` — optional evaluation data

`05_stock_issue_return_raveesha.sql` is an older retained copy. Do not run it when
`06_stock_issue_return_raveesha.sql` has been used.

Back up `stockflow_db` before running demonstration data on an existing database. The
Phase 5 script does not delete rows, but demonstration transactions should remain separate
from data you want to preserve as a clean submission database.
