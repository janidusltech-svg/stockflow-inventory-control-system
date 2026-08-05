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
Systems Officer.

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
```
