# Book Order System

A Java 17, Spring Boot, Maven REST application. Local runs default to in-memory H2;
automated tests use a separate in-memory H2 database. MariaDB remains an optional profile.

## Project layout

One Maven module, with packages under `src/main/java/com/example/bookorder`:

```text
controller/       HTTP endpoints, advice, API conversion and response helpers
domain/           catalog/order business orchestration and pagination rules
services/         contracts, implementations and persistence conversion
repository/       catalog/order repositories and persistence entities
controller/dto/request/      JSON request DTOs and BaseRequest
controller/dto/response/     JSON response DTOs and BaseResponse
integration/      outbound SPI and HTTP client
common/           request logging and JSON formatting
common/config/    security configuration
controller/enums/     API error codes
```

Dependencies flow `controller -> domain -> services -> repository`.
External calls flow from services through the integration SPI to its HTTP client.
Persistence conversion belongs to services; lower layers do not depend on HTTP DTOs.
“Report” is interpreted as repository/database access; no reporting feature is introduced.
Resources are in `src/main/resources`; existing tests are in `src/test/java`.
Payment, inventory and discount features remain unimplemented.

## Run

```powershell
# Default: H2 in memory, no external database required.
.\mvnw.cmd -Dmaven.test.skip=true clean package
java -jar target/book-order-system-1.0.0.jar
```

Unix/macOS: `./mvnw -Dmaven.test.skip=true clean package`, then the same `java -jar` command.

The default `h2` profile runs `src/main/resources/db/init_db.sql` automatically before
Hibernate validates the schema. Edit that file for local schema changes. It creates empty
tables; add seed INSERT statements after the tables if needed. Data is lost when the JVM
exits and the schema is recreated on the next launch. Initialization errors stop startup.
An explicitly selected profile overrides the default; remove an old `mariadb` run-profile
setting in the IDE or select `h2` explicitly. No H2 web console is enabled.

For persistent MariaDB, set DB_URL, DB_USER and DB_PASSWORD, then run:
`java -jar target/book-order-system-1.0.0.jar --spring.profiles.active=mariadb`.
The H2 initialization script is configured only in the H2 profile, so it does not run on MariaDB.

The API is at `http://localhost:8080`. HTTP Basic demo credentials are `reader` / `change-me`.
Override them with `APP_USER` and `APP_PASSWORD`; use HTTPS and non-demo credentials outside local
development.

## API

- `POST /api/books`, `GET /api/books/{id}`, `PUT /api/books/{id}`, `DELETE /api/books/{id}`
- `POST /api/books/search` with `{"query":"java","page":0,"size":10}` — case-insensitive
  title/author search using a JSON request DTO.
- `POST /api/orders` with `{"bookId": 1, "quantity": 2}`
- `POST /api/orders/search` with `{"page":0,"size":20}`
- `GET /api/external/google` — calls the configured external page endpoint

All endpoints require HTTP Basic authentication. Application responses use HTTP 200 with a JSON envelope. A
referenced-book deletion returns API code `CONFLICT`; missing resources use `NOT_FOUND`; invalid input uses
`INVALID_REQUEST`. Orders snapshot the book price at creation.

Search endpoints require a JSON object body; `{}` is valid. Omitted or null page/size values
default to page 0 and size 20. Page must be non-negative and size must be 1–100. Missing or
null book search text means all books; surrounding whitespace is trimmed. Empty bodies,
malformed JSON, and invalid bounds return `status: FAIL` with `errorCode: INVALID_REQUEST`. Collection
GET/query-parameter calls must migrate
to these POST endpoints. Resource IDs remain path variables for get, update, and delete.

## Standard JSON envelope

HTTP request DTOs extend `BaseRequest`; search DTOs inherit page/size from `PageRequest`.
Jackson binds JSON to JavaBean fields, Bean Validation validates them, and Lombok provides
getters/setters. The inherited `toString()` uses Jackson to produce a JSON string, including inherited fields.
`BaseResponse.toString()` also returns JSON; `JsonLogFormatter.toJson()` accepts objects or maps and returns a safe JSON
marker on serialization failure. Service DTOs remain independent.

Every handled application response uses `BaseResponse<T>`. `status` is a string (`SUCCESS` or
`FAIL`), not a boolean. Consumers must check it even when HTTP is 200. Success returns `data`
with null error fields; failure returns null `data`. Successful deletion also has null `data`.
The server creates a UUID trace ID and returns it in both the envelope and `X-Trace-Id` header.
It is a debugging correlation ID, not an idempotency key.

```json
{
  "status": "SUCCESS",
  "errorCode": null,
  "errorDesc": null,
  "data": {"id": 1, "title": "Java", "author": "Author", "price": 12.50},
  "traceId": "server-generated-uuid"
}
```

```json
{
  "status": "FAIL",
  "errorCode": "INTERNAL_ERROR",
  "errorDesc": "An internal error occurred. Contact support with the trace ID.",
  "data": null,
  "traceId": "server-generated-uuid"
}
```

`ErrorCodeEnum` has explicit `code` and `description` fields (Lombok getters); responses use these values, not the enum
constant name. It defines `INVALID_REQUEST`, `NOT_FOUND`, `CONFLICT`, `EXTERNAL_UNAVAILABLE`,
`UNAUTHORIZED`, `FORBIDDEN`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`, `NOT_ACCEPTABLE`,
and `INTERNAL_ERROR`. Public descriptions never copy raw exception messages or SQL details.
Controller advice handles application/MVC failures; security handlers handle authentication and
access failures before controller invocation. Both return the same JSON contract with HTTP 200.
Authentication is still enforced, so callers must send Basic credentials preemptively.
Failures before application handling (for example proxy/connection errors) can still have other
transport outcomes; clients must handle these separately.

This changes the response contract: business data is now under `data`, create/delete no longer
return 201/204, and API failures no longer use HTTP 4xx/5xx. API failure monitoring must use
`apiStatus` and `errorCode`, not only HTTP status. Do not blindly retry writes after an internal
error: an uncertain response does not establish whether an operation committed.

## Database

Normal startup uses the default `mariadb` profile. Create a database on your MariaDB server and
supply its connection settings through environment variables:

```powershell
$env:DB_URL = "jdbc:mariadb://localhost:3306/bookstore"
$env:DB_USER = "bookstore"
$env:DB_PASSWORD = "<your-database-password>"
java -jar target/book-order-system-1.0.0.jar
```

Use your actual host, port, database, username and password. The database must already exist and the
user needs access. Do not commit credentials. An unreachable server or incorrect credentials fail
startup; the application does not fall back to H2. An explicitly selected profile replaces the default;
include `mariadb` when selecting other application profiles that need this connection configuration.

The existing MariaDB `ddl-auto=update` setting creates/updates entity tables for development and
preserves rows between restarts. It does not create the database itself. Production schema migrations
remain unimplemented; do not treat automatic schema updates as a production migration strategy.

The repository's `save` writes through JPA to the configured database. Create/update/delete retain
the existing service transactions and rollback behavior. Changing the database does not add durable
idempotency or resolve order concurrency gaps; MariaDB locking and SQL behavior still need validation.

H2 has Maven `test` scope and is excluded from the application jar. The existing Spring integration
test activates the `test` profile and reads `src/test/resources/application-test.properties`, which
uses an in-memory H2 database with `create-drop`. Test configuration is not packaged in the application.
No tests are being run during this change, and no connection to a real database has been verified.

## ExternalApiClient

`ExternalApiClient` is an outbound interface (a port) that declares `fetch()`. It lives in the
application's `integration/spi` package. `GoogleClient` implements the HTTP details and timeout
behavior. `GoogleService` depends on the interface, so another provider or a test double can
replace the client without changing service logic. `PageMetadata` is the external HTTP metadata;
its status is separate from the application's SUCCESS/FAIL response envelope.

The flow is `ExternalController -> GoogleService -> ExternalApiClient -> GoogleClient`.
Calls remain outside database transactions.

## External API and logs

The external URL defaults to `https://www.google.com`; set `EXTERNAL_PAGE_URL` to override it. The
client enforces a 3-second connect timeout and a 5-second request timeout. External calls run outside
database transactions, are not automatically retried, and return API code `EXTERNAL_UNAVAILABLE` when unavailable (HTTP
200).

The web filter logs HTTP status, API status/error code, trace ID, and duration. It does not log authentication headers.
Controller DEBUG logs include bounded, single-line request DTO `toString()` output (up to 2048 characters). Only
approved non-sensitive fields should be included; exclude secrets from future DTO toString implementations. Unexpected
exception details are logged server-side, never returned to clients.

## Tests

The full 29-test JUnit Jupiter suite passes after converter regrouping and correction of JSON logging assertions. Run
Maven test (using mvn test, or the wrapper when its bootstrap is available). Tests cover business pagination/delegation,
service-level order price snapshots, the external client against a local HTTP server, and API
CRUD/search/order/security behavior using H2 and MockMvc.

## Request and response structure

Each HTTP DTO has its own file under `controller/dto/request` or `controller/dto/response`. Service DTOs are
under `services/request` and `services/response`. `BookVO` supplies title, author
and price for creating/updating a book; `BookOrderVO` supplies book ID and quantity.
Feature-specific CatalogApiConverter, OrderApiConverter and ExternalApiConverter map HTTP and service DTOs. Methods
identify both types, for example bookRequest2BookServiceRequest.
The book service is `BookServices1`, implemented by `BookServicesImpl` under `services/catalog/impl`.

Tests use JUnit Jupiter (JUnit 5), with JUnit assertions. Mockito provides dependency mocks;
MockMvc tests the HTTP contracts with H2.

## Converter and IDE troubleshooting

Open the root `pom.xml` as the Maven project. Catalog persistence mapping lives in
`book`.
Catalog and order entity types are in `repository.catalog.entity` and `repository.order.entity`; service request beans
and response
records live in `services/request` and `services/response`, respectively. Older snapshots using
`com.example.demo.core.converter` are not part of the current build.

The POM configures Lombok as an annotation processor. VS Code workspace settings enable
Lombok support and automatic Maven build-configuration updates. If the current source still
shows missing getter/setter or old-package errors after a successful `mvnw.cmd clean verify`,
run **Java: Clean Java Language Server Workspace** from the command palette and reload.
See the [Java extension settings](https://github.com/redhat-developer/vscode-java/blob/main/README.md).

### Engineering review scope

The converter/IDE diagnosis changes no application behavior, database writes, SQL, network
calls, or monetary calculations. No messaging, caching, scheduled jobs, or parallel queries
are introduced. The application is not fully H2·3.1 compliant:

- Book and order creation have no durable idempotency-key contract; retries can create duplicates.
- Order creation does not lock the book or declare a transaction timeout; concurrent book
  updates/deletion are not serialized with the order's price snapshot.
- External calls have HTTP timeouts and no automatic retries, but expose failure and unknown
  outcomes through the same exception. `NOT_SUPPORTED` suspends a transaction; its `timeout`
  attribute does not impose a seven-second service deadline.
- Substring search scans normalized text and cannot use ordinary B-tree indexes. Page size is
  bounded, but deep offsets and service-level pagination are not independently bounded.
- Money uses exact decimals, but currency is unspecified; tenant isolation, UTC audit timestamps,
  and business timezone/calendar semantics are not implemented. Do not use this demo as a
  multi-tenant payment system.
- Production schema migrations, actionable monitoring/recovery thresholds, and a three-step
  release/rollback procedure remain undefined. MariaDB behavior needs separate validation.

### Workspace dependency resolution

Maven CLI reads `.mvn/maven.config` and uses the project-local `.maven/repository` cache.
VS Code Java reads `.mvn/settings.xml` through the workspace Maven user-settings setting,
so Jakarta, Spring and Lombok imports resolve from the same cache. The IDE settings use this
workspace's absolute path; update both paths if the project moves. No offline mode is forced.
If stale errors remain after the layout change, run **Java: Clean Java Language Server Workspace**
and allow the root Maven project to reload.

This import/configuration repair changes no business logic, validation rules, database writes,
locks, transactions, SQL, network calls, retries, or money/tenant/time handling. Existing engineering
gaps above remain; no new messaging, caching or scheduled work is introduced. Tests remain deferred.

### Controller headers and service input validation

All controllers require Basic Authorization through Spring Security. JSON POST/PUT requests require
Content-Type: application/json. Accept: application/json is recommended; omitting Accept is allowed.
Responses use JSON and include the server-generated X-Trace-Id. No custom request headers are required.

Service implementations use @Validated and their interfaces declare @NotNull, @Valid and positive
ID constraints. BookServiceRequest validates required text, maximum lengths, and decimal precision;
OrderServiceRequest validates a positive book ID and quantity 1..1000. Calls must go through injected
Spring beans for method validation to execute. Direct construction/self-invocation bypasses it.
Invalid input maps to INVALID_REQUEST without exposing internal violation details. Input validation
precedes row lookup; update/delete still lock and check the current database row before mutation.

ApiExceptionHandler is one advice class with methods for distinct API error categories. Keeping these
methods avoids a long conditional dispatcher and preserves NOT_FOUND, CONFLICT and other stable codes.
SecurityErrorWriter remains separate because authentication failures occur before controller execution.

Existing tests were only changed to use explicit local types. They were not executed: direct-construction
service tests need to be adapted for proxy validation and the JSON toString assertions remain stale.
Compilation alone does not verify runtime validation or header/error behavior. Existing idempotency,
order concurrency, currency/tenant/time, SQL and operational limitations remain unresolved.
Conversion classes are grouped by business feature under controller/<feature>/converter and
services/<feature>/converter. Mapping names follow source2Destination, including collection shape.
Common ConversionUtils.list2List handles ordered, sequential list conversion with explicit mappers;
it rejects nulls and returns an unmodifiable list. PageResponseConverter.page2PageResponse only wraps
already mapped page content. Service request DTOs use setters with field validation; keep each mutable
request confined to its caller. Field copying remains explicit; no reflection-based copying is used.
