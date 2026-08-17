# Production database migration gate

This is a mandatory gate before the first SwiftPay production backend
deployment. The infrastructure can be created now, but the backend must not be
sent real traffic until every exit criterion in this document is satisfied.

## Why deployment remains gated

There is no legacy MySQL schema or application data to preserve. The current JPA
entity model is therefore the source for the initial schema. The repository now
contains two Flyway migrations:

- `V1__authoritative_baseline.sql` creates the ten entity tables, unique
  constraints, foreign keys, and deterministic `utf8mb4_unicode_ci` table
  collation;
- `V2__admin_high_value_merchant_procedures.sql` creates the three stored
  procedures used by the administrative merchant reports.

The baseline was generated from Hibernate's MySQL dialect and normalized for
review. V1 and V2 have passed on disposable MySQL 8.0.21, including Hibernate
`validate`, procedure contract checks, and a repeat no-op migration. Production
remains gated until the migrations receive independent review, are applied using
the migration identity, and the least-privilege runtime login is verified.

Hibernate now defaults to `JPA_DDL_AUTO=validate`. Flyway is enabled for the
default dev profile but the production profile defaults `FLYWAY_ENABLED=false`.
Do not change Hibernate to `update`, `create`, or `create-drop`, and do not enable
Flyway during ordinary App Service startup.

## Required access and separation of duties

Use three different database identities:

| Identity | Purpose | Retention in App Service |
| --- | --- | --- |
| MySQL administrator | Initial server/user administration and emergency recovery | Never |
| Migration identity | Reviewed DDL and routine deployment | Never |
| `swiftpayapp` runtime login | Backend DML and routine execution | Key Vault reference only |

Terraform accepts two different password inputs:

- `TF_VAR_database_admin_password` configures only the Flexible Server
  administrator.
- `TF_VAR_database_application_password` becomes the `DB-PASSWORD` Key Vault
  secret consumed by App Service.

Terraform intentionally does not connect to MySQL, create database users, or
run migrations. The server has private VNet access, so all SQL work must run
from an approved, ephemeral administration host or self-hosted runner inside
the production VNet (or through approved VPN/ExpressRoute connectivity). Do not
temporarily enable public database access.

## Phase 1: review the entity-derived baseline

1. Freeze entity mapping changes while the baseline is reviewed and record the
   exact application commit, Hibernate version, target MySQL version, character
   set, and collation.
2. Compare every `@Entity`, `@Table`, `@Column`, relationship, enum, uniqueness
   rule, and optimistic-lock field with V1. V1 must represent all ten entities.
3. Confirm V1 contains no database creation/use statement, environment-specific
   schema name, `DEFINER`, account, password, or data row.
4. Review the explicit InnoDB, `utf8mb4`, and `utf8mb4_unicode_ci` settings
   against the Terraform database configuration.
5. Review V2 against the Java callers and expected result columns for:

   - `sp_admin_high_value_merchants_summary`;
   - `sp_admin_high_value_merchants_count`;
   - `sp_admin_merchant_high_value_txns_by_merchant`.

6. If an existing database with data is introduced before this baseline is
   approved, stop and replace this entity-derived process with a schema-only
   dump and object-by-object comparison. Never baseline an unknown populated
   schema automatically.

## Phase 2: approve the versioned migrations

Perform this work in a separate pull request.

1. Review the Spring Boot 4 Flyway starter and Flyway MySQL module in the backend
   dependency tree. Their versions must remain managed by Spring Boot.
2. Review the migrations in execution order:

   ```text
   PaytmCloneBackend/src/main/resources/db/migration/V1__authoritative_baseline.sql
   PaytmCloneBackend/src/main/resources/db/migration/V2__admin_high_value_merchant_procedures.sql
   ```

3. Treat V1 and V2 as immutable after their first application. Put every later
   change in a new `V3__...sql`, `V4__...sql`, and so on.
4. Configure production migration execution as a separate, approved step using
   migration-only credentials. Keep `FLYWAY_ENABLED=false` during ordinary App
   Service startup so staging health probes cannot change the shared production
   schema.
5. Do not enable `baselineOnMigrate`. Every approved environment starts with an
   empty database and executes V1 and V2 normally. If a populated database is
   introduced later, stop and design a separately reviewed adoption procedure.

The migration pull request must include entity-to-DDL review evidence, forward
test results, restore/rollback evidence, reviewer sign-off, and the application
commit against which Hibernate validation passed.

## Phase 3: test on disposable MySQL 8.0.21

The Terraform default is MySQL 8.0.21. Pin that version until a separate 8.4
compatibility exercise is complete.

1. Provision a disposable empty MySQL 8.0.21 database using `utf8mb4` and
   `utf8mb4_unicode_ci`.
2. Apply all migrations using the migration identity and confirm Flyway records
   V1 followed by V2.
3. Run Flyway `validate`, then run `migrate` again to prove an idempotent no-op.
4. Start the backend with the production profile and
   `JPA_DDL_AUTO=validate`. Startup and `/actuator/health` must succeed without
   any DDL.
5. Run all Maven tests plus MySQL integration tests for registration/login,
   wallet operations, KYC, transfers, split payments, idempotency, and all three
   stored-procedure-backed admin reports. H2-only tests are not sufficient.
6. Inspect `information_schema` and `SHOW CREATE PROCEDURE` output. Compare all
   tables, constraints, collations, and routines with V1 and V2 and investigate
   every difference.
7. Recreate another empty database from zero and repeat migration and validation
   before approving the migration set. A destructive down migration is not the
   default rollback; use an application-compatible forward repair or restore
   once production contains data.

### Current MySQL verification evidence

On 2026-08-14, the migrations were tested with the official `mysql:8.0.21`
container on a new `utf8mb4`/`utf8mb4_unicode_ci` database:

- Flyway validated and applied V1 followed by V2 successfully.
- The resulting schema contained ten application tables and three procedures.
- Every application table used `utf8mb4_unicode_ci`.
- Spring Boot started with Hibernate `validate` and reported MySQL 8.0.21.
- Procedure fixtures produced one matching merchant, two high-value
  transactions totaling `40000.00`, and two distinct payers.
- The transaction-detail procedure returned both qualifying transactions in
  descending creation order.
- A second Flyway-enabled startup reported schema version 2 and no migration
  necessary.
- The disposable container and its database were removed after verification.

This test used the local MySQL root account. It does not replace testing the
migration-only and runtime identities, Azure private connectivity/TLS, backup
recovery, or the independent review required by the exit criteria.

## Phase 4: prepare the production database and runtime login

Run this only after Terraform has created the private server and after the V1/V2
migration pull request is approved.

For the concrete temporary-host, GitHub variable, SSH, Flyway, verification,
and cleanup commands, follow
[`production-database-migration-execution.md`](production-database-migration-execution.md).

1. Start an approved VNet-connected migration host. Confirm the production FQDN
   resolves to a private address and connect with
   `--ssl-mode=VERIFY_IDENTITY`.
2. Apply and validate the reviewed migrations with the migration identity.
3. Create the runtime login using the exact username from
   `terraform output mysql_application_username` and the application password
   from the approved secret manager. The values below are placeholders—bind or
   substitute secrets without logging them:

   ```sql
   CREATE USER 'swiftpayapp'@'%' IDENTIFIED BY '<application-password>' REQUIRE SSL;
   GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE
     ON `swiftpay`.* TO 'swiftpayapp'@'%';
   ```

4. Do not grant `CREATE`, `ALTER`, `DROP`, `INDEX`, `TRIGGER`, `CREATE ROUTINE`,
   `ALTER ROUTINE`, `GRANT OPTION`, or server-administration privileges to the
   runtime login.
5. From the same private host, authenticate as `swiftpayapp` and prove:

   - TLS is active.
   - Required table reads/writes work in a disposable transaction.
   - All three procedures can execute and return the expected column contracts.
   - A harmless DDL statement is denied.

6. Remove the migration host or return the self-hosted runner to its locked-down
   state. Revoke temporary role assignments and erase local credential material.

## Phase 5: release and verify

1. Confirm the production Key Vault `DB-USERNAME` value is `swiftpayapp` (or the
   reviewed override), never the administrator username.
2. Confirm the `DB-PASSWORD` secret version corresponds to the password used in
   `CREATE USER`.
3. Deploy the backend to the stopped staging slot through
   `prod-backend.yml`. The slot has scheduling disabled and uses
   `JPA_DDL_AUTO=validate`.
4. Require staging health and smoke tests before swap. After swap, require
   production health and inspect MySQL audit/connection logs for the runtime
   username.
5. Store migration evidence with the release record: migration versions and
   checksums, operator/job identity, timestamps, reviewed plan, test results,
   and approvers. Never store passwords.

## Exit criteria

Production backend deployment is unblocked only when all are true:

- The entity-derived V1 baseline is committed and independently reviewed.
- The three stored procedures are present in V2 and contract-tested.
- A clean MySQL 8.0.21 database migrates successfully through V1 and V2.
- Flyway validation and Hibernate `validate` both pass on MySQL.
- No populated database is adopted through `baselineOnMigrate`.
- The private production database has the expected V1/V2 migration history.
- The runtime login exists with DML plus `EXECUTE` only, and DDL denial is tested.
- Backup restore and application rollback compatibility are demonstrated.
- Migration evidence and approvals are retained.

## Official references

- [Azure MySQL TLS and private-connectivity requirements](https://learn.microsoft.com/en-us/azure/mysql/flexible-server/security-tls-how-to-connect)
- [Azure MySQL private networking](https://learn.microsoft.com/en-us/azure/mysql/flexible-server/concepts-networking-vnet)
- [Flyway commands, including baseline, migrate, and validate](https://documentation.red-gate.com/flyway/reference/commands)
- [Flyway baseline migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/baseline-migrations)

