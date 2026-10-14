# H2·3.1 International Payment Engineering Standard

These rules are mandatory implementation and code-review criteria for this project. The Chinese
statements below are the policy source supplied by the project owner; the English guidance makes
them actionable. Apply relevant requirements to every change and call out any requirement that
cannot be met, including its consequence.

## Policy source

### 基础编码

并发控制,一锁二判三更新,并行查询控超时,乐观悲观要合适。
幂等拦截,幂等字段要对齐,上下游要约定好,异常场景防击穿。
事务控制,悬挂监控要及时,必须防止空回滚,定要最终一致性。
SQL语句,使用要求最简化,业务逻辑归代码,索引不含隐式转换。

### 中间件使用

消息使用,一考虑重复投递,二考虑丢失延迟,三考虑消息乱序。
缓存使用,数据过期要控制,缓存击穿要兜底,存储容量要考虑。
调度任务,不重不漏不堵塞,捞取数据可配置,熔断处理必须有。

### 架构设计

防御編程,宁愿不做不错做,强弱依赖要合理,故障隔离能自救。
网络调用,交互三态要牢记,换号重发高风险,错误码映射要谨慎。
接口契约,字段约定要明确,业务含义要清晰,解耦研发独立发布。

### 质量与风险

开发自测,CR单测接口测,断言回归边界值,系统质量不降分。
资损防控,时效治理全链路,应急止血要可控,纳入架构设计中。
监控核对,发现问题为目的,阔值设置要合理,及时降噪禁麻木。
线上变更,严格执行三板斧,应急手段要完备,恢复业务为要务。

### 国际特色

多币种,注意币种有无分,金额计算要克隆,外汇买卖分方向。
多租户,入口设值不可变,数据都要租户值,隔离拆分是本质。
多时区,切点时区和日历,三者不能缺其一,时间戳存UTC。

## Implementation and review criteria

### Concurrency, idempotency, transactions, and SQL

- Database state changes follow **lock → validate → update**. Lock and re-check current state in
  the same bounded transaction before mutation. Choose optimistic or pessimistic locking based on
  contention and conflict behavior; do not add retries that can duplicate effects.
- Any parallel queries have bounded concurrency, deadlines/timeouts, and explicit failure
  behavior. Do not parallelize dependent work or share a transaction-bound persistence context
  across threads.
- Define idempotency key name, scope, retention, payload-equivalence rules, and replay response
  consistently across callers and all affected layers. Enforce durable uniqueness in storage and
  handle concurrent duplicate-key races. Ensure exceptional retries cannot bypass deduplication.
- Keep transactions short and database-focused. Specify commit/rollback behavior; do not report
  success after rollback, swallow write failures, or issue meaningless/empty rollbacks. For
  cross-system effects, use an outbox, reconciliation, or another documented eventual-consistency
  design. Monitor suspended/stuck work and define recovery behavior.
- Keep SQL minimal: filtering, joining, ordering, and aggregation only. Put business branching in
  application code. Match parameter types to column types, avoid implicit conversions/functions on
  indexed predicates, verify pagination bounds and important query plans, and index measured access
  patterns.

### Middleware: messaging, cache, and scheduled jobs

- Messaging must account for duplicate delivery, loss/delay, and out-of-order events. Define
  acknowledgement, retry/backoff, dead-letter/replay, deduplication, ordering key, and recovery
  semantics; handlers should be idempotent where delivery is at least once.
- Set cache freshness/TTL deliberately. Provide a fallback for hot-key expiration/cache stampedes,
  bound stale or missing-data behavior, and estimate capacity/eviction effects. Cache must not be
  treated as the authoritative source for critical correctness or idempotency.
- Scheduled jobs must prevent unintended duplicate work and omissions, avoid blocked worker pools,
  expose configurable batch/pickup limits, and define retry, checkpoint, and recovery behavior.
  Provide a circuit breaker or equivalent protection for failing dependencies and make job progress
  observable.

### Architecture and network boundaries

- Use defensive programming: validate untrusted input, fail safely, avoid performing an action when
  required facts are uncertain, classify dependencies as strong or optional, and isolate failures
  with bounded resources and a defined fallback/recovery path.
- Model network outcomes as success, definitive failure, or unknown/timeout. A timeout does not
  prove the remote side did not perform the operation. Do not blindly retry non-idempotent actions
  or switch identifiers/keys on retry; use the agreed idempotency contract. Map downstream errors
  deliberately without hiding actionable failure information. All calls require bounded connect
  and response timeouts; retries need limits, backoff, and transient-error criteria.
- Treat API fields, meanings, units, nullability, enum evolution, and compatibility as explicit
  contracts so producers and consumers can evolve and release independently.
- Keep external calls outside database transactions. For multi-system changes, document the
  consistency model and compensating/reconciliation path.

### Quality, financial risk, operations, and release

- Self-test changes with review, focused unit tests, API/integration tests where relevant, and
  regression assertions for boundary and failure cases. Do not reduce existing quality coverage
  without an explicit reason and replacement.
- Identify financial-loss scenarios and end-to-end time limits. Include safe stop/containment and
  recovery controls in the design, not only in operational notes.
- Add useful monitoring for outcomes and stuck work. Set thresholds from expected behavior, reduce
  noisy alerts without suppressing actionable incidents, and ensure alerts lead to an operator
  action.
- Before production changes, use the project’s three-step change/release procedure, ensure rollback
  or containment is available, and prioritize restoring correct business operation. If no such
  procedure is defined for the change, state that gap rather than claiming release readiness.

### International payment concerns

- Model currency explicitly and consistently, including whether an amount is major-unit or
  minor-unit and the currency's scale. Use exact decimal/integer arithmetic with defined rounding;
  never use binary floating point for money. For FX, record source/target currencies, direction,
  rate, and applicable timestamp/reference so a conversion is auditable.
- Establish tenant identity at a trusted, immutable ingress boundary. Propagate it through calls,
  jobs, and messages; scope reads, writes, uniqueness constraints, and cache keys to the tenant.
  Prevent cross-tenant access by default and test isolation.
- Store instants as UTC. For business-day cutoffs, use an explicit timezone and calendar (including
  daylight-saving behavior); do not infer these solely from server-local time. Keep timestamp,
  timezone, and calendar semantics distinct and explicit.

## Project-specific implementation notes

- Local runtime defaults to the h2 profile with a runtime-scoped H2 dependency. The in-memory
  database is ephemeral and is rebuilt on a fresh JVM launch from classpath:db/init_db.sql.
  SQL initialization runs before Hibernate ddl-auto=validate and fails startup on script errors;
  Hibernate must not also create the schema. The script uses H2 identity columns, entity-aligned
  book_id, exact DECIMAL(12,2) amounts, foreign keys and nonduplicated indexes. No seed data is supplied.
  Hikari connections set UTC and Hibernate JDBC timestamp binding uses UTC; business timezone and
  calendar rules remain undefined. H2 DDL is not a rollback-safe production migration mechanism.
  Tests still explicitly activate the separate test profile with Hibernate create-drop.
  MariaDB requires an explicit mariadb profile and DB_URL/DB_USER/DB_PASSWORD settings; its failures
  must not silently fall back to H2. The H2 script is not applied to MariaDB.
  Existing MariaDB ddl-auto=update remains for development bootstrap; production versioned migrations
  are still missing, so schema evolution is not production-ready. Invalid credentials/unreachable DB
  fail startup. This configuration change preserves write ordering, transaction timeouts and rollback,
  SQL/pagination, external-call behavior, and adds no retries, messaging, caches or jobs. Existing
  idempotency, order locking, currency, tenant and UTC/calendar gaps remain. Packaging is checked;
  real MariaDB behavior and tests remain unverified until connection details and test work are available.

- Use explicit local variable types rather than var. Prefer clear loops over complex stream chains;
  simple Page.map conversions are allowed to preserve pagination metadata without custom plumbing.
- Controllers document the standard header contract: Basic Authorization is required, JSON body
  endpoints consume application/json, and responses produce JSON plus a generated X-Trace-Id.
  Accept: application/json is recommended, not required; no client trace ID is required.
- Service implementations use @Validated; interface parameters declare @NotNull/@Valid and ID
  constraints. Service DTOs declare text, precision and quantity constraints. Use injected Spring
  services: direct construction and self-invocation bypass method validation. Input checks run before
  service execution; update/delete still lock and verify database state before mutation. Request
  validation is not a replacement for transactional state checks or database constraints.
- Keep one API advice with separate handlers per public error category. ConstraintViolationException
  maps to INVALID_REQUEST, with no raw violation messages exposed. Security failures retain their
  separate pre-controller writer. A generic catch-all must not erase public error distinctions.
- Input-annotation refactoring adds service-level length/decimal/positive-ID checks. Invalid input
  now fails before repository access (including when an update ID is missing). Existing transactions,
  rollback and external timeouts/no-retry behavior remain. No messaging/cache/jobs are introduced;
  currency, tenant and UTC/calendar gaps remain as documented in README. Tests are still deferred.

- Use one Maven module with packages controller, domain, services, and repository.
  Do not use application-prefixed layer directories. Dependencies flow controller -> domain ->
  services -> repository. CatalogBiz and OrderBiz orchestrate use cases in domain feature packages.
  BookServices and OrderPersistService remain service contracts.
- Place configuration in common/config. API error codes belong to controller/enums because they define the HTTP API
  contract.
  Keep feature-specific enums with their owning feature; common is not a general dumping ground.
  HTTP DTOs belong to controller/dto; service DTOs stay in services/request and services/response.
- HTTP transfer types use controller/dto/request and controller/dto/response; reserve VO for domain value objects.
  Feature-specific persistence converters belong to services because they translate entities to service
  DTOs. Outbound SPI/client implementations remain in integration, separate from controllers.
- Package restructuring preserves locking, transaction/rollback behavior, typed SQL, pagination
  bounds, external timeouts/no-retry behavior, and response/logging contracts. It introduces no
  writes, messaging, caches, jobs, currency, tenant or time/calendar logic. Review package scanning,
  logging pointcuts and exception/security wiring when moving classes. Existing README gaps remain.
  Compilation/package verification does not prove runtime behavior; tests remain deferred by user
  instruction.
- API JSON request classes extend `BaseRequest` (or `PageRequest` for pagination). Each DTO has
  its own file, multiline fields, and an inherited JSON toString that must exclude secrets. BaseRequest/BaseResponse use
  JsonLogFormatter; serialization failures produce a safe JSON marker, not an exception or internal details. DEBUG
  logging bounds
  request text and removes line breaks. Do not log authentication headers.
- API responses use `BaseResponse<T>`: string status SUCCESS/FAIL, errorCode, errorDesc, data,
  and traceId. Every handled application/MVC/security outcome uses HTTP 200 by project policy;
  security still denies execution. Success has null error fields; failure has null data.
  ErrorCodeEnum declares explicit stable code and public-safe description fields; responses use getErrorCode()/getErrorDesc(),
  not enum names. Unexpected exception details remain in server logs.
- Generate a server trace ID per request, return it in JSON/header, and clean MDC after processing.
  Log API failures separately from HTTP status. Trace IDs are not deduplication keys.
- Envelope conversion happens outside service transactions, after service completion. This change
  preserves write ordering, transaction boundaries and rollback; it introduces no retries or
  external calls. Existing idempotency/concurrency, SQL/index, currency, tenant, UTC/calendar,
  monitoring/release gaps in README remain; no messaging, cache or job subsystem is introduced.
- Put every request and response DTO in its own file and matching `request` or `response`
  package. Keep HTTP and service DTOs separate with explicit converters. Use `BookVO`
  for book service input and `BookServices1` for the book service contract. Use JUnit Jupiter.
- Service request DTOs use no-argument beans with field validation and getters/setters. API
  conversion assigns fields explicitly rather than using a positional constructor. Collection
  conversion may use a sequential stream calling the same mapper; inputs and elements must be
  non-null, and the returned list is unmodifiable. Each request must remain confined to its caller
  and must not be mutated concurrently with validation or persistence. This DTO change preserves
  full-update required fields, validation, lock/write ordering, transaction deadlines and rollback.
  It changes no SQL, external calls, retries, messaging, caches, jobs, or money/time semantics;
  existing idempotency, currency, tenant and UTC/calendar gaps remain unresolved.
- This naming/package refactor preserves validation, transactions, locks, rollback, SQL and
  network behavior. It introduces no writes, retries, messaging, caches, jobs, or currency,
  tenant/time semantics. Existing compliance gaps in README remain unresolved.

- Controller search/filter/pagination input uses validated JSON request DTOs, not individual
  query parameters. Collection searches use POST `/api/books/search` and `/api/orders/search`;
  resource IDs remain path variables. Defaults and bounds are part of the documented API contract.
- Keep feature methods readable: separate conversion, service invocation, and response construction
  when nesting obscures the flow; document method purpose and relevant failure/consistency behavior.
- This request-contract change is read-only: it adds no writes, retries, external calls, messaging,
  caches, jobs, currency conversions, tenant handling, or time/calendar behavior. Existing documented
  compliance gaps remain. Search SQL and deep-offset limitations are unchanged; callers must migrate
  from collection GET requests to JSON POST requests.

- Catalog update/delete acquire a pessimistic row lock and verify the row exists before mutation.
- Catalog updates use an explicit typed update query and require one affected row in the service.
  Missing lock results or zero updated rows report not-found; unexpected counts and database errors
  roll back the five-second transaction. Bulk updates clear the persistence context to avoid stale
  managed book data. An absent lock result skips mutation so concurrent insertion cannot bypass
  lock-before-update ordering. No automatic retries are introduced. MariaDB affected-row semantics (including unchanged
  values), locking and rollback still require database integration verification;
  existing create idempotency, currency, tenant and UTC/calendar gaps remain unresolved.
- Catalog create/update validate the command in the service layer; writes have a five-second
  transaction timeout.
- External page requests run outside database transactions. The HTTP client enforces a
  three-second connect timeout and a five-second request timeout. The seven-second timeout
  attribute on `NOT_SUPPORTED` does not enforce a service deadline because no transaction is
  started; an independent service deadline is not implemented.
- The catalog create endpoint still needs a caller-provided idempotency-key contract and durable
  deduplication before it can claim idempotent create semantics. This is intentionally explicit,
  not simulated with a process-local cache.
- Catalog search currently applies `lower()` to title/author and performs substring matching. This
  preserves case-insensitive search behavior but prevents ordinary B-tree indexes from serving
  that predicate; use a database-specific functional/full-text index or a separately designed
  search strategy before adding indexes for this query.
- Catalog search builds optional predicates with JPA Criteria in the repository fragment and binds
  search text through an explicit String parameter. Empty search omits filtering; literal substring,
  case-insensitive matching and pagination counts are preserved. Repository pagination rejects
  unpaged requests, sizes above 100 and offsets above the JPA integer limit. Content and count can
  still differ under concurrent writes at the existing database isolation level. This read-only
  change preserves transaction deadlines and adds no retries or external effects. MariaDB plans,
  substring index limitations and existing idempotency/currency/tenant/UTC gaps remain unresolved.

- Group API and persistence converters by business feature: catalog, order and external. Keep
  controller mappings under controller/<feature>/converter, service mappings under
  services/<feature>/converter, and generic HTTP page mapping under controller/support.
  Name all project-owned mapping methods lowerCamelSource2Destination, including actual type
  names (bookRequest2BookServiceRequest) and collection shape (bookRequestList2BookServiceRequestList). Multi-source
  mappings identify both source types;
  in-place mappings document target mutation. Reserve VO for genuine domain value objects.
- ConversionUtils shares sequential list iteration only; explicit field mappings remain in feature
  converters. list2List rejects null sources, mappers, elements and results, propagates mapper
  failures without a partial result, preserves order and returns an unmodifiable list. Empty input
  produces an empty list. Mappers must be pure except for explicitly documented target mutation;
  do not pass repository/network operations or share mutable requests across threads.
- Page content conversion uses Page.map to preserve pagination metadata; page2PageResponse
  wraps already converted content. Persistence mapping remains inside existing transactions and
  HTTP conversion follows service completion. Converter regrouping changes no lock/validate/update
  ordering, write counts, transaction boundaries/timeouts/rollback, SQL, retry or external-call
  behavior. No messaging/cache/jobs are added. Exact money and order price snapshots are preserved;
  existing create idempotency, order locking/timeouts, currency, tenant, UTC/calendar, MariaDB
  query-plan and operational/release gaps remain unresolved.

## BooksService complete-edit contract

- PUT /books/{id} accepts BookRequest with required title, author and price. The API converter
  maps these fields explicitly to services/request/BooksUpdateVO. Validate positive IDs, text
  lengths, positive exact-decimal price and its 12,2 precision before repository access.
  The controller method returns void; central response advice emits SUCCESS with data:null.
- BooksService.update invokes lockByBookId and, only after a row is found, updateBook in the
  same transaction. The service checks affected rows: zero is NOT_FOUND, one is success, and
  unexpected counts fail with an internal error. Never mutate the locked entity, call save,
  or use reflective copying in this update flow; doing so could cause an extra flush/write.
- The service starts a transaction with a five-second timeout and default REQUIRED propagation.
  A caller-owned transaction retains its own timeout and must also be bounded. Runtime exceptions,
  database errors and lock failures propagate for rollback; there are no automatic retries.
  The three-second JPA lock hint requires provider/database verification. Row locks serialize
  writers but do not detect stale client requests; a later full edit can overwrite an earlier edit.
- The update query binds Long/String/BigDecimal parameters and filters by the primary key without
  expressions or implicit type conversions. Stock and createdAt are not writable through this
  request. The existing CURRENT_TIMESTAMP assignment and clearAutomatically behavior remain;
  the persistence context is cleared after the bulk update while the database lock lasts until
  transaction completion. MariaDB matched-row behavior for unchanged values remains to be tested.
- This change adds no external calls, parallel application queries, messaging, caching, jobs, FX,
  or schema migration. Existing generated create/search code and the older catalog flow remain
  incomplete; missing types/repositories can prevent project compilation and runtime verification.
  The generated controller still calls the service directly instead of the business layer, and
  retains its pre-existing success-response contract. This is not full architectural/API alignment.
- UTC interpretation of database CURRENT_TIMESTAMP is unverified because database/session timezone
  is unspecified. Currency and tenant isolation remain undefined, and creation retries lack durable
  deduplication. These gaps prevent claims of international-payment compliance. Production migration,
  release/recovery and operational-monitoring gaps also remain as previously documented.
- Verification on 2026-10-14: targeted BooksServiceUpdateTest and
  BooksServiceUpdateIntegrationTest execution was attempted, but Maven failed during main-source
  compilation. Missing BooksVO/BooksQueryVO and order request types, removed BookRepository/BookDO/
  OrderRepository/BookOrderDO/ExternalServiceConverter references, and BookOrdersController's invalid
  jakarta.persistence validation imports prevent tests from running. Unit, rollback/concurrency
  and MariaDB behavior are therefore unverified; no test-pass or release-readiness claim is made.

## Central HTTP response contract

- Maven JaCoCo coverage excludes classes in vo, dto, request, response and entity packages,
  including nested classes. This is a reporting/instrumentation exclusion only: retain behavioral
  validation, mapping and persistence tests. Data carriers must not contain business logic merely
  to evade coverage. Coverage reports are generated at verify under target/site/jacoco; target/
  is already Git-ignored. These rules do not configure IntelliJ's independent coverage runner.

- Application REST controllers may return plain JSON data, strings, pages, lists, null or void.
  ApiResponseAdvice wraps these in BaseResponse.success after service completion; null/void yields
  data:null. Existing BaseResponse values pass through once. BooksController is the first migrated
  controller; other controllers may retain explicit envelopes. String results use the configured
  ObjectMapper because Spring may have selected its string converter. Binary/resource converters,
  streaming handlers and framework endpoints retain their own response contracts.
  ApiResponseMvcConfiguration adds a write-only fallback converter so MVC invokes the advice for
  void and null Object results; it uses the configured ObjectMapper and does not accept request bodies.
- ApiExceptionHandler centralizes controller/MVC failures, including BizException's explicit enum
  code; unexpected failures expose only INTERNAL_ERROR and log details internally. Do not add
  controller try/catch blocks or swallow transactional exceptions. Security failures use the same
  envelope through SecurityErrorWriter before MVC. Handled JSON outcomes remain HTTP 200.
- Response writers record request-local status, errorCode and errorDesc for HttpLoggingFilter;
  trace IDs come from the request and remain in the response header/body. No mutable per-request
  state is held in advice singletons. Serialization or transport failures after response commitment
  cannot be replaced with a JSON failure. A committed service write is not undone by serialization
  failure; clients must not assume a failure response makes retrying a write safe.
- This change adds no database writes, queries, locks, retries, external calls, messaging, caches,
  scheduled jobs or money/time calculations. Existing transaction ordering/rollback and SQL remain
  unchanged. Currency/tenant/UTC, idempotency, MariaDB verification and release/recovery gaps remain;
  this response refactor does not establish full H2·3.1 compliance or production readiness.
- Verification: six ApiResponseAdviceTest tests pass in an isolated Maven build of the response
  components using the project dependencies. Coverage includes JSON shapes, strings, null/void,
  envelope passthrough, trace/log attributes, validation/business/unexpected failures, pre-commit
  serialization failure, binary exclusion and the security writer. The full project build remains
  blocked by unrelated BookOrdersController imports/missing DTOs and ExternalController's missing
  CatalogBiz; application startup and end-to-end security-chain execution are not verified.
