# Book Order System

Java 17, Spring Boot 3.5.16 and Maven application for books, orders with line items,
and an external HTTP fetch example. It uses Spring MVC, Security, Bean Validation,
Spring Data JPA and Lombok. Local startup defaults to in-memory H2; MariaDB is optional.

## Run locally

Install a JDK compatible with Java 17 and configure `JAVA_HOME`, then run:

```powershell
.\mvnw.cmd clean package
java -jar target/book-order-system-1.0.0.jar
```

On Unix/macOS use `./mvnw clean package`. The wrapper may need network access to
bootstrap Maven and resolve dependencies. To package without compiling or running tests,
use `-Dmaven.test.skip=true`; this does not verify application behavior.

- Base URL: `http://localhost:8080`
- HTTP Basic defaults: `book` / `book`; override with `APP_USER` and `APP_PASSWORD`.
- Public Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Public OpenAPI document: `http://localhost:8080/v3/api-docs`

Business endpoints require Basic authentication. Send credentials preemptively and use
`Content-Type: application/json` for JSON bodies. Use HTTPS and override demo credentials
for deployments outside local development.

## Database profiles

The default `h2` profile uses the in-memory `TESTDB` database. Startup executes
[`db/init_db.sql`](src/main/resources/db/init_db.sql), then Hibernate validates the schema
with `ddl-auto=validate`. The script creates `books`, `book_orders` and `book_order_items`,
with no seed rows. Initialization errors stop startup. Data disappears when the JVM exits.
H2 is a runtime dependency and is included in the application package; no H2 web console
is enabled. H2 connections and Hibernate JDBC timestamp binding use UTC.

For persistent MariaDB, create the database and explicitly select the profile:

```powershell
$env:DB_URL = "jdbc:mariadb://localhost:3306/bookstore"
$env:DB_USER = "bookstore"
$env:DB_PASSWORD = "<your-database-password>"
java -jar target/book-order-system-1.0.0.jar --spring.profiles.active=mariadb
```

Supply all three values rather than relying on the development defaults in
`application-mariadb.properties`. MariaDB uses `ddl-auto=update`; it does not run the H2
script or create the database itself. Connection failures do not fall back to H2.
Versioned production migrations are not implemented.

`src/test/resources/application-test.properties` provides a separate H2 database with
`create-drop` for tests that explicitly activate the `test` profile. The current test
classes use Mockito and do not start that database.

## API

The current routes have no `/api` prefix. The table describes the value inside the
standard response envelope's `data` field.

| Method | Path | Request | Successful data |
| --- | --- | --- | --- |
| POST | `/books/save` | Book object | Created ID as a string, e.g. `"1"` |
| POST | `/books/batchSave` | Array of book objects | ID list as a string, e.g. `"[1, 2]"` |
| GET | `/books/{id}` | Path ID | List containing one book, or `[]` if absent |
| PUT | `/books/{id}` | Complete book object | `null` |
| DELETE | `/books/{id}` | Path ID | `null` |
| POST | `/books/search` | Book search object | Spring Data page of `BookVO` values |
| POST | `/bookOrders` | Order object with line items | Created ID as a string |
| GET | `/bookOrders/{id}` | Path ID | List containing one order, or `[]` if absent |
| GET | `/bookOrders` | **JSON request body** with order search fields | Spring Data page of order responses |
| DELETE | `/bookOrders/{id}` | Path ID | `null` |
| GET | `/external/google` | None | External HTTP status, content type and content |

Order search currently requires a body on GET; it is not a query-parameter endpoint.
Some clients and intermediaries do not support GET bodies. There is no
`POST /bookOrders/search` mapping.

### Books

Create and update use `BookRequest`. All four fields are required:

```json
{
  "title": "Java",
  "author": "Author",
  "price": 12.50,
  "unitsInStock": 10
}
```

Title and author must be nonblank, with maximum lengths of 200 and 150. Price must be
at least 0.01, with at most ten integer digits and two fractional digits. Stock must
be a nonnegative integer. PUT replaces these editable fields; it is not a partial update.

Book search example:

```json
{"title":"Java","author":"Author","page":0,"pageSize":20}
```

Search accepts `title`, `author`, `minPrice` and `maxPrice`. It uses SQL `LIKE` substring
predicates without trimming, lowercasing or escaping wildcard characters; case sensitivity
depends on database collation. **The current price-range condition is reversed:** it adds
`BETWEEN` only when `minPrice > maxPrice`. Normal ranges and one-sided bounds are ignored.

Search pagination uses `page` and `pageSize`, not `size`. Missing or null values default
to 0 and 20; page must be nonnegative and pageSize must be 1–100. An empty object `{}` is
valid. Book search additionally rejects unpaged service requests and offsets above the
JPA integer limit. Book lookup responses use `id`; book search content uses `bookId` and
also includes audit fields from `BookVO`.

### Orders

Order creation binds directly to `BookOrderVO`:

```json
{
  "userId": "reader-1",
  "orderType": "ONLINE",
  "bookOrderDetailVOs": [
    {"bookId": 1, "quantity": 2}
  ]
}
```

Create the referenced books first. The service computes `totalAmount` from current book
prices and quantities, ignoring a supplied total. It saves the order header followed by
its items in one transaction. Items persist their unit price and item total as snapshots.
Reads return order totals and detail entries containing book IDs and quantities; item
price snapshots are not exposed by the current response DTO. Stock is stored on books,
but placing an order does not check or decrement it. Payment and discount processing are
not implemented.

Order search accepts `orderId`, `bookId`, `userId`, `orderType`, `startDate`, `endDate`,
`page` and `pageSize`. Zero or omitted ID filters mean no ID filter. User ID and order type
use substring matching. When both dates are present, both `createdAt` and `updatedAt`
must fall within the interval; a single date bound is ignored.

The detail list must be non-null, and quantities must be positive. Validation remains
incomplete: an empty detail list is accepted, book IDs are not required by the DTO, and
user ID/order type have length constraints but no required-field validation. Do not assume
the unused `BookOrderRequest` class describes this endpoint or imposes its quantity cap.

## Response and error contract

`ResponseAdvice` wraps application controller results in `BaseResponse<T>`; existing
envelopes pass through once. Null/void results have `data: null`. Handled application,
MVC and security outcomes use HTTP 200, so callers must inspect `status` and `errorCode`.
Authentication failures still prevent controller execution.

Example response for a book creation:

```json
{
  "status": "SUCCESS",
  "errorCode": null,
  "errorDesc": null,
  "data": "1",
  "traceId": "server-generated-uuid"
}
```

Failures use `status: "FAIL"`, null data and a public error description. Codes are defined
in `common/exception/enums/ErrorCodeEnum.java`, including `INVALID_REQUEST`, `PARAM_INVALID`,
`BOOK_NOT_FOUND`, `NOT_FOUND`, `CONFLICT`, `SYSTEM_BUSY`, `EXTERNAL_UNAVAILABLE`,
`UNAUTHORIZED`, `FORBIDDEN`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`, `NOT_ACCEPTABLE`
and `INTERNAL_ERROR`. Missing book updates and missing books during order creation use
`BOOK_NOT_FOUND`; missing GET-by-ID results are successful empty lists.

The server supplies `X-Trace-Id` and the matching JSON trace ID. This is a correlation ID,
not an idempotency key. Transport or serialization failures may prevent delivery of an
envelope even after a write commits; blindly retrying a write can duplicate it.

## Project layout

One Maven module, rooted at `src/main/java/com/example/bookorder`:

```text
controller/          BooksController, BookOrdersController, ExternalController
  advice/            ResponseAdvice and ExceptionHandler
  converter/         HTTP/service mappings
  dto/               Request and response types; request/base holds shared inputs
  support/           Response, pagination and security helpers
services/            BooksService, BookOrdersService, ExternalFetchService
  vo/                Book/order service values and search criteria
  response/          ExternalServiceResponse
repository/          Spring Data JPA repositories
  entity/            Books, BookOrders, BookOrderItems
integration/         ExternalApiClient SPI/registry and GoogleClient
common/              Configuration, exceptions, filters, logging and constants
```

Book/order controllers call services directly, then repositories; there is no active
`domain` orchestration layer. The external flow is
`ExternalController -> ExternalFetchService -> ExternalApiClient -> GoogleClient`.
The client registers under the key `google`; a single injected client is also supported
as a fallback. Some service code currently depends on HTTP response types.

## External calls and logging

`EXTERNAL_PAGE_URL` overrides the default `https://www.google.com`. `GoogleClient` uses a
three-second connect timeout and five-second request timeout, with no automatic retries.
Client exceptions, null responses and upstream HTTP statuses of 400 or higher map to
`EXTERNAL_UNAVAILABLE`. The external controller flow starts no database transaction;
there is no transaction-suspension annotation or independent service deadline.

Logs go to the console and `logs/book-order.log`. `LOG_DIR` and `LOG_FILE` customize the
file destination; `HTTP_LOG_LEVEL=DEBUG` enables controller diagnostics. Rolling files
use 10 MB segments, 14-day history and a 500 MB archive cap. HTTP logs include trace IDs,
duration and API outcome as well as HTTP status; monitor API failures rather than HTTP
status alone. Request logging must exclude secrets.

## Build and tests

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

The current JUnit Jupiter/Mockito test classes are `BooksServiceTest`,
`BookOrdersServiceTest` and `BookOrdersControllerTest`. They exercise services and direct
controller calls with mocked dependencies. They do not establish live HTTP security,
Spring method validation, transaction rollback, locking or database correctness.
No test-pass count is asserted here; this README update was checked against source and
configuration without executing the suite or starting the application.

`verify` generates JaCoCo coverage under `target/site/jacoco`. Packages named `vo`, `dto`,
`request`, `response` and `entity` are excluded from coverage, not from compilation or tests.

Import the root `pom.xml` into the IDE and enable Lombok annotation processing.
`.mvn/maven.config` selects `.maven/repository`; `.mvn/settings.xml` contains the absolute
workspace cache path and needs updating if the checkout moves.

## Known engineering gaps

[ENGINEERING_STANDARDS.md](ENGINEERING_STANDARDS.md) defines H2·3.1 requirements. Some local
notes describe earlier implementations: the current book update mutates the locked entity
(including stock), rather than issuing an explicit update query, and current controllers
call services directly. This README documents current source behavior, not full compliance.

- Creates have no durable idempotency contract, so retries can create duplicates.
- Book updates use a pessimistic lock and a five-second transaction; order creation/deletion
  also have five-second transactions. Order creation does not lock books, and deletion
  paths do not implement the required lock/validate/update sequence. Concurrent price,
  deletion and order operations need database-level verification.
- The H2 initialization script declares no foreign keys. Referenced-book deletion is not
  guaranteed to return `CONFLICT`, and orphaned references are possible.
- Price filtering and order validation have the limitations described above. Substring
  searches and deep pagination need query-plan validation; order service pagination has
  no independent bounds beyond the controller DTO.
- Exact decimal money is used, but currency, tenant isolation and business timezone/calendar
  rules are absent. H2 UTC settings do not verify MariaDB timestamp behavior.
- Production migrations, actionable alert thresholds, recovery procedures and a three-step
  release/rollback process remain undefined. Live MariaDB, concurrency and end-to-end
  verification are required before claiming production readiness.
