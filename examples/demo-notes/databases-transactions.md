# Database Systems — Transactions

A transaction groups multiple database operations into one logical unit of work.

## ACID properties

Transactions are commonly described using **ACID**:

1. **Atomicity** — either all operations succeed or none take effect.
2. **Consistency** — constraints remain satisfied.
3. **Isolation** — concurrent transactions should not interfere incorrectly.
4. **Durability** — committed changes survive failures.

## Isolation

Concurrent execution can produce anomalies such as dirty reads, non-repeatable reads, and phantom reads.

Database systems use locking or multi-version concurrency control to provide appropriate isolation.
