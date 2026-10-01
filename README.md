# StockFlow

StockFlow is a web-based inventory control system for Lanka Fresh Distributors (Pvt) Ltd.

## Confirmed Technology Stack

- Java 17
- Spring Boot
- Thymeleaf
- MySQL
- Maven
- IntelliJ IDEA
- GitHub

## Member Modules

| Member | Function |
|---|---|
| Chamidu | Product Catalogue Management |
| Tharinya | Supplier and Purchase Order Management |
| Dishan | Goods Receiving and Batch Management |
| Tesara | Warehouse and Stock Transfer Management |
| Raveesha | Stock Issue and Return Management |
| Janidu | Central Inventory Control, Auditing, Reordering and Dashboard |

## Current Build Status

The integrated build includes all six major functions, a public landing page, a secured
staff workspace, BCrypt password storage, role-based navigation and server-side access
control. User and role management is maintained as a common function by the IT and
Systems Officer. Operations and Store staff can also maintain active product categories,
brands and measurement units through the shared Reference Data area. Permanent deletion
is offered only for records that nothing else uses, so existing relationships remain valid. The secured
workspace now uses one responsive sidebar, active role-aware navigation, automatic page
context, read-only indicators, client-side table pagination, confirmation dialogs and
friendly error pages across all six modules.

## Internal Workspace UI

- One responsive navigation shell is shared by every secured page.
- Links are shown only when the signed-in role is authorized for that function.
- Read-only roles receive a clear page indicator while the server continues to enforce access.
- Tables with more than ten visible records receive browser-side pagination automatically.
- Important workflow actions receive a confirmation dialog before submission.
- The dashboard summarizes the authenticated role and the connected six-function workflow.
- The UI respects reduced-motion settings and adapts to tablet and mobile widths.

## Safe Permanent Delete

Deactivate and Delete are separate options. Deactivate hides a record from new work and
keeps it. Delete removes it permanently and is offered only when that cannot break history.

| Record | Delete allowed when |
|---|---|
| Product | Never used in a purchase order, receipt, batch, balance, movement, transfer, issue or reorder rule |
| Category, brand, unit | No product uses it |
| Supplier | It has no purchase orders (drafts included) |
| Purchase order | Status is DRAFT and no goods receipt refers to it |
| Warehouse | It has no storage locations |
| Storage location | No balance, movement, receipt, transfer, issue or count uses it |
| Recipient | It has no stock issues |
| Reorder rule | Always (it is a setting, not history) |
| Goods receipt, transfer, issue, return, count, adjustment | Status is DRAFT |

- A deleted draft's own item lines are removed with it. Linked products, batches, balances
  and issue lines are never touched.
- Stock movements, inventory balances and completed documents are never deleted.
- Foreign keys stay enabled and no cascade rules were added. No SQL migration is required.
- `src/main/resources/db/11_safe_delete_demo.sql` contains read-only queries for showing a
  deletion in MySQL Workbench.

## Authorized Roles

| Role | Main access |
|---|---|
| Operations Manager | Inventory control, approvals, audits, reorder rules and reports |
| Store Supervisor | Products, receiving, warehouses, transfers, issues and returns |
| Finance Executive | Suppliers, purchase orders and authorized inventory views |
| Customer Service Coordinator | Product availability, issues and return-status views |
| IT and Systems Officer | User accounts, password resets and role assignments |

## Local Setup

1. Start MySQL 8 and create the database if it does not already exist:

```sql
CREATE DATABASE stockflow_db;
```

2. Update the local database credentials in:

```text
src/main/resources/application.properties
```

3. For the first local run, set the local MySQL and development bootstrap passwords in PowerShell:

```powershell
$env:STOCKFLOW_DB_PASSWORD="Your-Local-MySQL-Password"
$env:STOCKFLOW_BOOTSTRAP_PASSWORD="Choose-A-Strong-Password"
```

4. Open the project folder in IntelliJ IDEA:

```text
stockflow
```

5. Reload Maven so the Spring Security dependency is downloaded, then run:

```text
StockflowApplication
```

6. Open the public website:

```text
http://localhost:8080
```

Select **Access system**, or open `http://localhost:8080/login`. On the first run,
StockFlow creates one local demonstration account for each role: `operations`, `store`,
`finance`, `service` and `itadmin`. They initially use the bootstrap password supplied
above. Use the IT account to create permanent staff accounts and reset passwords.

## GitHub Contribution Rule

Each member should work on a separate branch and commit only their assigned module.

Example:

```text
feature/product-catalogue-chamidu
feature/supplier-purchase-tharinya
feature/goods-receiving-dishan
feature/warehouse-transfer-tesara
feature/stock-issue-return-raveesha
feature/inventory-control-janidu
feature/auth-role-access-janidu
feature/reference-data-management-janidu
feature/internal-ui-phase-2-janidu
```
