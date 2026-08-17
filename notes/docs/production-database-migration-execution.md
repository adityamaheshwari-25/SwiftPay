# First production database migration: operator steps

This procedure creates a short-lived migration VM, applies the reviewed V1/V2
migrations over the MySQL private endpoint, creates the least-privilege runtime
login, captures evidence, and removes the VM. It does not enable public MySQL
access and it does not run Flyway during App Service startup.

## 1. Generate a temporary SSH key

Run in Windows CMD:

```cmd
ssh-keygen -t ed25519 -a 100 -f "%USERPROFILE%\.ssh\swiftpay-prod-migration" -C "swiftpay-prod-migration"
type "%USERPROFILE%\.ssh\swiftpay-prod-migration.pub"
```

Protect the private key. Put only the single-line `.pub` value in GitHub.

## 2. Identify the approved operator address

Use the stable public egress IPv4 address of the approved corporate/VPN
connection and append `/32`, for example `203.0.113.10/32`. Do not use a broad
home/office CIDR and do not use `0.0.0.0/0`.

## 3. Configure the protected GitHub environment

In repository **Settings > Environments > production-infrastructure >
Environment variables**, add or update:

| Variable | Value |
| --- | --- |
| `TF_VAR_enable_migration_host` | `true` |
| `TF_VAR_migration_operator_cidr` | The approved IPv4 address with `/32` |
| `TF_VAR_migration_host_ssh_public_key` | Contents of the `.pub` file |
| `TF_VAR_migration_host_vm_size` | Optional VM-size override; omit to use `Standard_D2as_v5` |

These are variables, not secrets. Never put a private SSH key or a database
password in these values.

## 4. Create and review the migration host

Run **Prod infrastructure** with operation `apply`. The plan should add only the
temporary migration subnet, NSG and associations, public IP, NIC, and Linux VM.
It must not replace or destroy MySQL, Key Vault, Storage, the VNet, or App
Service. Approve only after checking that condition.

Record the `migration_host` Terraform output or read the public IP of
`vm-swiftpay-prod-am26k7p-db-migrate` in Azure Portal.

## 5. Connect and wait for provisioning

Run in Windows CMD, replacing the address:

```cmd
ssh -i "%USERPROFILE%\.ssh\swiftpay-prod-migration" azureadmin@<MIGRATION_VM_PUBLIC_IP>
```

On the VM:

```bash
cloud-init status --wait
docker image inspect flyway/flyway:11.14.1 >/dev/null
mysql --version
```

If Docker reports a socket permission error during the first SSH session, sign
out and reconnect so the new `docker` group membership is applied.

## 6. Clone the exact approved revision

Use the commit SHA shown by the successful reviewed production run:

```bash
git clone https://github.com/adityamaheshwari-25/SwiftPay.git
cd SwiftPay
git checkout <APPROVED_COMMIT_SHA>
git status --porcelain
git rev-parse HEAD
```

The status command must print nothing and `HEAD` must equal the approved SHA.

## 7. Verify private DNS and an empty target

```bash
getent hosts mysql-swiftpay-prod-am26k7p-southindia.mysql.database.azure.com
export MYSQL_HISTFILE=/dev/null
mysql \
  --host=mysql-swiftpay-prod-am26k7p-southindia.mysql.database.azure.com \
  --user=swiftpayadmin \
  --password \
  --ssl-mode=VERIFY_IDENTITY \
  --ssl-ca=/etc/ssl/certs/ca-certificates.crt
```

At the MySQL prompt:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'swiftpay'
ORDER BY table_name;
```

Stop if application tables or unknown migration history already exist.

## 8. Create the migration identity

Generate and retain a unique migration password in the approved secret manager.
As `swiftpayadmin`, run:

```sql
CREATE USER 'swiftpay_migrator'@'%'
IDENTIFIED BY '<MIGRATION_PASSWORD>'
REQUIRE SSL;

GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, ALTER, DROP, INDEX, REFERENCES,
      CREATE ROUTINE, ALTER ROUTINE, EXECUTE
ON `swiftpay`.* TO 'swiftpay_migrator'@'%';

SHOW GRANTS FOR 'swiftpay_migrator'@'%';
```

The migration identity must not receive `CREATE USER`, `GRANT OPTION`, or
server-wide privileges.

## 9. Inspect and apply V1/V2

From the cloned repository, run:

```bash
bash ./scripts/production-db-migrate.sh <APPROVED_COMMIT_SHA>
```

The script first runs `info` and `validate`. Review that output. It then requires
an exact commit-specific confirmation before it runs `migrate`. After migration
it runs `validate`, `info`, and a second no-op `migrate`, and stores non-secret
evidence under the VM user's home directory.

The script also downloads the pinned official MySQL Connector/J, verifies its
SHA-256 checksum, mounts it read-only into the Flyway container, and disables
Flyway's MariaDB JDBC fallback. This is required for the production MySQL 8
authentication configuration.

Do not run `flyway clean`, `baseline`, or `repair`.

## 10. Verify database objects

Reconnect as the administrator and run:

```sql
SELECT installed_rank, version, description, script, checksum,
       installed_by, installed_on, execution_time, success
FROM swiftpay.flyway_schema_history
ORDER BY installed_rank;

SELECT routine_name
FROM information_schema.routines
WHERE routine_schema = 'swiftpay'
ORDER BY routine_name;
```

V1 and V2 must both be successful, and all three `sp_admin_*` procedures must be
present.

## 11. Create and test the runtime identity

Use the exact application password previously supplied to Terraform as
`TF_VAR_database_application_password`. As administrator:

```sql
CREATE USER IF NOT EXISTS 'swiftpayapp'@'%' REQUIRE SSL;
ALTER USER 'swiftpayapp'@'%'
  IDENTIFIED BY '<APPLICATION_PASSWORD>' REQUIRE SSL;
GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE
  ON `swiftpay`.* TO 'swiftpayapp'@'%';
SHOW GRANTS FOR 'swiftpayapp'@'%';
```

Reconnect as `swiftpayapp` with `--ssl-mode=VERIFY_IDENTITY` and verify:

```sql
SHOW SESSION STATUS LIKE 'Ssl_cipher';
CALL sp_admin_high_value_merchants_summary(10000, NULL, 10, 0);
CALL sp_admin_high_value_merchants_count(10000, NULL);
CALL sp_admin_merchant_high_value_txns_by_merchant(0, 10000, 10, 0);
CREATE TABLE runtime_create_should_fail (id INT);
```

TLS must be active, the procedure calls must execute, and `CREATE TABLE` must be
denied.

## 12. Retain evidence and remove the host

Copy the non-secret evidence directory to the approved release record. Do not
copy passwords, shell history, client configuration files, or private keys.

Set `TF_VAR_enable_migration_host` to `false` in the protected GitHub
environment, remove the operator CIDR and public-key variables, and run a new
**Prod infrastructure** `apply`. Review that it destroys only the temporary
migration VM resources. Do not approve any MySQL or application replacement.

After the destroy succeeds, delete the temporary private key from the operator
workstation if organizational policy does not require retaining it.
