# Coupon Service

REST API for creating and redeeming discount coupons.

Recruitment task implemented with Java, Spring Boot and PostgreSQL.

## Running locally

Requirements:

- Java 25
- Docker

Start PostgreSQL:

```bash
docker compose up -d
```

Start the application:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The API is available at:

```text
http://localhost:8080
```

## Tests

Integration and concurrency tests use PostgreSQL Testcontainers and WireMock.

Docker must be running.

```bash
./mvnw clean verify
```

## API

### Redeem coupon

```http
POST /api/v1/coupons/{code}/redeem

{
  "userId": "user-123"
}
```

The client's country is determined from its IP address.

Possible responses:

- `200` — coupon redeemed
- `403` — coupon cannot be used in the detected country
- `404` — coupon not found
- `409` — coupon exhausted or already used by the user

### Management API

Coupon management operations are separated from the customer-facing redemption API.

```text
POST   /api/v1/management/coupons
GET    /api/v1/management/coupons
GET    /api/v1/management/coupons/{code}?countryCode=PL
PATCH  /api/v1/management/coupons/{code}?countryCode=PL
DELETE /api/v1/management/coupons/{code}?countryCode=PL
```

Example:

```http
POST /api/v1/management/coupons
Content-Type: application/json

{
  "code": "SUMMER25",
  "countryCode": "PL",
  "maxUsages": 100
}
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Design Decisions

### Concurrency

Coupon usage limits are enforced atomically by PostgreSQL:

```sql
UPDATE coupons
SET current_usages = current_usages + 1
WHERE id = :couponId
  AND current_usages < max_usages;
```

The number of affected rows determines the result:

- `1` — redemption succeeded
- `0` — coupon is exhausted

The check and increment happen in a single database statement, preventing concurrent requests from exceeding `max_usages`.

This works across multiple application instances and does not require application-level or distributed locks.

### One redemption per user

PostgreSQL additionally enforces:

```text
UNIQUE(coupon_id, user_id)
```

The counter increment and usage insert run in the same transaction.

If two concurrent requests attempt to redeem the same coupon for the same user, only one usage record can be created. The failed transaction is rolled back together with its counter increment.

### Coupon uniqueness

Coupon codes are case-insensitive and unique within a country:

```text
UNIQUE(code, country_code)
```

Codes and country codes are normalized using `Locale.ROOT` before persistence and lookup.

This allows the same promotional code to be configured independently for different markets.

This is an explicit domain assumption. If global coupon-code uniqueness is required, the database constraint can be changed to `UNIQUE(code)`.

PostgreSQL automatically creates indexes for UNIQUE constraints, so no
redundant indexes are created for the same columns.

An additional index on `country_code` is created to support coupon listing
and pagination by country.

### Geolocation

The customer's country is resolved from the request IP using an external geolocation provider.

The integration uses:

- Resilience4j retry for transient failures
- Caffeine caching for IP-to-country results
- WireMock in integration tests

The external HTTP call happens before the database transaction starts, avoiding holding a database connection while waiting for the external service.

In production, forwarded IP headers should only be trusted when requests pass through a controlled reverse proxy or load balancer.

## Testing Strategy

The most important database behavior is tested against a real PostgreSQL instance using Testcontainers.

Concurrency tests verify that:

- concurrent requests never exceed `maxUsages`
- concurrent requests from the same user result in exactly one successful redemption

Repository behavior is intentionally tested with PostgreSQL rather than mocked because correctness depends on transactions, constraints and database locking semantics.

## Tech Stack

- Java 25
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Liquibase
- Resilience4j
- Caffeine
- Testcontainers
- WireMock
- Maven