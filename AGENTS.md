# Codex development instructions

Before changing application code, read and follow [ENGINEERING_STANDARDS.md](./ENGINEERING_STANDARDS.md).
Treat the complete H2·3.1 policy and its implementation/review criteria as mandatory acceptance
criteria, including concurrency, idempotency, transactions and consistency, SQL/indexes, messaging,
caching, scheduled jobs, architecture/network boundaries, quality/risk/operations, and
internationalization (currency, tenant isolation, and UTC/timezone/calendar handling).

For every relevant change, explicitly assess failure paths and edge cases. For database writes,
check lock → validate → update ordering, transaction boundaries/timeouts, rollback, and concurrent
retries. For external calls, check three-state outcomes (success/failure/unknown), idempotency,
bounded timeouts, and retry limits; keep calls outside database transactions. For SQL, check typed
parameters, index usability, pagination bounds, and that business branching remains in application
code. For messaging/cache/jobs, address duplicate/lost/delayed/ordered delivery, expiry/stampede and
capacity, and duplicate/omitted/stalled work. For payment-domain changes, verify money precision,
currency and FX direction, tenant propagation/isolation, and UTC timestamps with explicit business
timezone/calendar semantics.

If a requirement cannot be implemented within the requested change, state the specific missing
behavior and its consequence in the handoff; do not describe the work as fully compliant. Update
the engineering standard when the project adopts a new policy, and keep this file as its Codex entry
point.
