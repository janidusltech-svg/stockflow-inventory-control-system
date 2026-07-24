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

The first project foundation includes:

- Spring Boot application setup
- MySQL connection configuration
- Shared dashboard UI
- Product Catalogue CRUD module
- Product validation
- Duplicate SKU protection
- Search and filtering
- Product deactivate function

## Local Setup

1. Create a MySQL database:

```sql
CREATE DATABASE stockflow_db;
```

2. Update database credentials in:

```text
src/main/resources/application.properties
```

3. Open the project folder in IntelliJ IDEA:

```text
stockflow
```

4. Run:

```text
StockflowApplication
```

5. Open:

```text
http://localhost:8080
```

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
```
