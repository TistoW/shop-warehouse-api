# Shop Warehouse API

English | [Bahasa Indonesia](README.id.md)

English | [Bahasa Indonesia](README.id.md)

A small REST API for a shop warehouse: items, their variants (size, color and so on), prices and stock levels, plus orders that refuse to sell what is not on the shelf. Built with Java 17 and Spring Boot 3.5.

Live demo: https://geli-api.tisto.my.id opens Swagger UI. The data resets every night and the H2 console is disabled there.

## Background

I have solved this problem in production before, just on a different stack. Zenenta (https://zenenta.com) is a POS and back-office system for retail shops, and ZenentaShop (https://zenentashop.com) is its online storefront. Both run on the same product, variant and stock data, so a sale in the store and a sale online draw from one stock count. ChatSaya (https://chatsaya.com), an AI WhatsApp assistant I built, reads live prices and stock from Zenenta and can turn a confirmed chat order into a pending order there.

Those systems run on NestJS, Prisma and PostgreSQL. This repository is a Java/Spring Boot take on the same core ideas:

- price and stock live on the variant, not on the item
- every stock change leaves a row in a movement ledger
- a sale is rejected at the database level when stock is not enough

This is a fresh implementation written for this assessment. It is not code that runs in production.

## How to run

You need Java 17. Maven is not required, the wrapper is included.

```
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`. If port 8080 is busy, add `--server.port=9090`. The app starts on http://localhost:8080 with an embedded H2 database stored in `./data`, so there is nothing to set up and data survives restarts. The sample data is loaded on first start.

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console, JDBC URL `jdbc:h2:file:./data/warehouse`, user `sa`, empty password (stop the app first if you open the same file from another tool)

To run against PostgreSQL with Docker:

```
docker compose up --build
```

This starts Postgres 16 and the app (with the `postgres` profile) on port 8080. To point a locally started app at your own Postgres, run with `--spring.profiles.active=postgres` and set `DB_URL`, `DB_USER` and `DB_PASSWORD`.

Tests:

```
./mvnw test
```

The suite runs against in-memory H2. There is also a Postgres variant of the concurrency test using Testcontainers. It needs Docker and is skipped automatically when Docker is not available.

## Design decisions and why

**Variants carry price and stock.** An item is just the catalog entry (name, description). Anything you can actually sell, and count, is a variant with its own SKU, price and stock. An item without real variants gets one variant called "Default" (create it with just `sku` and `price`). That way there is one code path for selling, whether a product has sizes or not, and no "does this item have variants?" branching anywhere.

**Stock never changes through a plain update.** `PUT /variants/{id}` edits name, SKU and price only. Stock changes only through a stock adjustment or an order, and both write a row to `stock_movements` (positive for restock, negative for sale or correction). The current number on the variant is what you read and sell against; the ledger is how you explain it. New variants start at zero and are stocked with a RESTOCK adjustment, so there is exactly one way stock enters the system.

**Out-of-stock prevention is a conditional atomic update.** The core of the order flow is one statement per line:

```sql
UPDATE variants SET stock_quantity = stock_quantity - :qty
WHERE id = :id AND active = true AND stock_quantity >= :qty
```

If it updates zero rows, the sale is refused. I looked at three options:

- Read, check, write in Java: simple, but two requests can both read "1 left" and both sell it.
- Pessimistic lock (`SELECT ... FOR UPDATE`): correct, but it is an extra round trip and the lock is held while Java code runs.
- Optimistic locking with retry: correct, but under contention (a popular item, a flash sale) most attempts fail and have to be retried, which is wasted work.

The conditional update does the check and the write in one statement, so the database serializes competing buyers on the row and nobody oversells. No retry loop is needed. A `@Version` column on variants catches two writes to the same variant racing at the same moment. The version is not exposed to clients (no ETag or If-Match), so it does not stop someone from overwriting a change they never saw. The stock update bumps it too, so a price edit that races with a sale gets a 409 and should simply be retried.

An order is a single transaction. Duplicate variants in the request are merged first, and lines are processed in variant id order so concurrent orders always take row locks in the same order and cannot deadlock. If any line fails, the exception rolls back everything: no partial orders, no half-decremented stock, no orphan ledger rows. A `CHECK (stock_quantity >= 0)` constraint in the schema is the last line of defence if someone ever bypasses the service.

**Price snapshot on order lines.** `order_items` stores `unit_price` and `line_total` at the time of sale. Changing a variant's price later does not rewrite past orders.

**Archive instead of delete.** `DELETE` sets `active = false`. Past orders reference variants, so rows cannot just disappear. Archived items and variants are hidden from lists (use `includeArchived=true` to see them), cannot be sold and cannot be restocked. Archiving an item archives all its variants.

**Flyway owns the schema, Hibernate only validates it** (`ddl-auto=validate`). The migrations are plain SQL that runs on both H2 (in PostgreSQL mode) and real PostgreSQL, so the same files are used everywhere. Sample data is a second migration.

**H2 by default, Postgres by profile.** Zero setup for whoever runs this, while `docker compose` shows the same code against Postgres. The concurrency test exists for both.

**Package by feature** (`catalog`, `inventory`, `order`, `common`), with controller, service and repository in each. No interfaces with a single implementation, no mapping library: DTOs are records and the mapping is a few lines by hand. Errors are returned as RFC 7807 problem details.

## Assumptions

- One warehouse and one currency (prices are plain decimals, the sample data uses IDR).
- An order is a completed sale. There is no payment, reservation or cancellation flow.
- No authentication or authorization.
- SKUs are unique across the whole catalog.
- A request to sell an unknown variant is answered with 404, a variant that exists but is archived with 409.
- Order numbers come from a database sequence (`ORD-20261014-000001`). They are unique but can have gaps when an order is rolled back.

## API examples

The sample data has four items. Item 1 is a t-shirt with five variants (ids 1 to 5), variant 6 is a mug, variant 7 is a tote bag with zero stock, variant 8 is a notebook. Lists are paged (`page`, `size`, `sort`).

Items:

```
curl "localhost:8080/api/items?q=shirt&page=0&size=10"
curl localhost:8080/api/items/1

curl -i -X POST localhost:8080/api/items -H "Content-Type: application/json" -d '{
  "name": "Hoodie",
  "description": "Fleece, regular fit",
  "variants": [
    {"sku": "HOOD-GRY-M", "name": "Grey / M", "price": 349000},
    {"sku": "HOOD-GRY-L", "name": "Grey / L", "price": 349000}
  ]
}'

curl -i -X POST localhost:8080/api/items -H "Content-Type: application/json" \
  -d '{"name": "Sticker pack", "sku": "STICKER-1", "price": 15000}'

curl -X PUT localhost:8080/api/items/1 -H "Content-Type: application/json" \
  -d '{"name": "Classic Cotton T-Shirt", "description": "Heavyweight 180gsm cotton tee"}'

curl -i -X DELETE localhost:8080/api/items/4
```

Creates return `201` with a `Location` header. `DELETE` returns `204`.

Variants:

```
curl localhost:8080/api/items/1/variants
curl -i -X POST localhost:8080/api/items/1/variants -H "Content-Type: application/json" \
  -d '{"sku": "TEE-BLK-XL", "name": "Black / XL", "price": 155000}'
curl localhost:8080/api/variants/1
curl -X PUT localhost:8080/api/variants/1 -H "Content-Type: application/json" \
  -d '{"sku": "TEE-BLK-M", "name": "Black / M", "price": 155000}'
curl -X DELETE localhost:8080/api/variants/5
```

Stock:

```
curl -i -X POST localhost:8080/api/variants/4/stock-adjustments -H "Content-Type: application/json" \
  -d '{"quantity": 20, "reason": "RESTOCK", "note": "supplier delivery"}'

curl -X POST localhost:8080/api/variants/1/stock-adjustments -H "Content-Type: application/json" \
  -d '{"quantity": -2, "reason": "ADJUSTMENT", "note": "damaged in storage"}'

curl localhost:8080/api/variants/1/stock-movements
```

Orders:

```
curl -i -X POST localhost:8080/api/orders -H "Content-Type: application/json" -d '{
  "items": [
    {"variantId": 1, "quantity": 2},
    {"variantId": 6, "quantity": 1}
  ]
}'

curl "localhost:8080/api/orders?size=5"
curl localhost:8080/api/orders/1
```

Success returns `201` with the order, including the price snapshot per line:

```json
{
  "id": 1,
  "orderNumber": "ORD-20261014-000001",
  "totalAmount": 363000.00,
  "items": [
    {"variantId": 1, "sku": "TEE-BLK-M", "name": "Black / M", "quantity": 2, "unitPrice": 149000.00, "lineTotal": 298000.00},
    {"variantId": 6, "sku": "MUG-350", "name": "Default", "quantity": 1, "unitPrice": 65000.00, "lineTotal": 65000.00}
  ]
}
```

Asking for more than is in stock (variant 5 has 3 left) returns `409`, and nothing is changed, including other lines of the same order:

```
curl -i -X POST localhost:8080/api/orders -H "Content-Type: application/json" \
  -d '{"items": [{"variantId": 5, "quantity": 10}]}'
```

```json
{
  "type": "about:blank",
  "title": "Insufficient stock",
  "status": 409,
  "detail": "Not enough stock for TEE-RED-M: requested 10, available 3",
  "instance": "/api/orders",
  "sku": "TEE-RED-M",
  "requested": 10,
  "available": 3
}
```

Validation errors return `400` with the failing fields:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/items",
  "errors": [{"field": "name", "message": "must not be blank"}]
}
```

Other cases: `404` for unknown ids, `409` for a duplicate SKU, for taking stock below zero with a negative adjustment, for working with archived records, and for an edit that lost a race with another edit.

## What I would do next

- Authentication, and roles (staff who adjust stock versus staff who only sell).
- Multiple warehouses or locations: stock would move to a `(variant, location)` table.
- Order cancellation that puts stock back, written as a RESTOCK-style movement that references the order.
- An idempotency key on `POST /orders`, so a client retrying after a timeout cannot create the order twice.
- If this were split into services, the order service would publish an `OrderPlaced` event to Kafka through a transactional outbox (the event is written in the same transaction as the order and relayed afterwards), so other services such as notifications or reporting stay in sync without dual writes.
- A low-stock threshold per variant and a report on it.
