#!/usr/bin/env bash
set -Eeuo pipefail

readonly FLYWAY_IMAGE="flyway/flyway:11.14.1"
readonly MYSQL_CONNECTOR_VERSION="9.7.0"
readonly MYSQL_CONNECTOR_SHA256="0353648eaa1c91e0f4020c959abf756bc866ffd583df22ae6b6f6e0cbd43eb44"
readonly MYSQL_CONNECTOR_URL="https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/${MYSQL_CONNECTOR_VERSION}/mysql-connector-j-${MYSQL_CONNECTOR_VERSION}.jar"
readonly MYSQL_DRIVER_DIRECTORY="${MYSQL_DRIVER_DIRECTORY:-$HOME/.cache/swiftpay-flyway-drivers}"
readonly MYSQL_DRIVER_JAR="$MYSQL_DRIVER_DIRECTORY/mysql-connector-j-${MYSQL_CONNECTOR_VERSION}.jar"
readonly MYSQL_FQDN="${MYSQL_FQDN:-mysql-swiftpay-prod-am26k7p-southindia.mysql.database.azure.com}"
readonly MYSQL_DATABASE="${MYSQL_DATABASE:-swiftpay}"
readonly MIGRATION_USER="${MIGRATION_USER:-swiftpay_migrator}"

approved_commit="${1:-${APPROVED_COMMIT_SHA:-}}"
if [[ -z "$approved_commit" ]]; then
  echo "Usage: $0 <approved-production-commit-sha>" >&2
  exit 2
fi

for command_name in curl docker getent git sha256sum; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Required command is unavailable: $command_name" >&2
    exit 1
  fi
done

repository_root="$(git rev-parse --show-toplevel 2>/dev/null)" || {
  echo "Run this script from the checked-out SwiftPay repository." >&2
  exit 1
}
cd "$repository_root"

expected_commit="$(git rev-parse "${approved_commit}^{commit}")"
actual_commit="$(git rev-parse HEAD)"
if [[ "$actual_commit" != "$expected_commit" ]]; then
  echo "Checked-out commit $actual_commit does not match approved commit $expected_commit." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain --untracked-files=no)" ]]; then
  echo "Tracked repository files are modified. Use a clean approved checkout." >&2
  exit 1
fi

migration_directory="$repository_root/PaytmCloneBackend/src/main/resources/db/migration"
if [[ ! -f "$migration_directory/V1__authoritative_baseline.sql" ]] ||
   [[ ! -f "$migration_directory/V2__admin_high_value_merchant_procedures.sql" ]]; then
  echo "The approved V1 and V2 migration files are missing." >&2
  exit 1
fi

migration_files=("$migration_directory"/*.sql)
if (( ${#migration_files[@]} != 2 )); then
  echo "Expected exactly the reviewed V1 and V2 SQL migrations; found ${#migration_files[@]}." >&2
  exit 1
fi

mapfile -t mysql_addresses < <(getent ahostsv4 "$MYSQL_FQDN" | awk '{print $1}' | sort -u)
if (( ${#mysql_addresses[@]} == 0 )); then
  echo "Private DNS did not resolve $MYSQL_FQDN." >&2
  exit 1
fi

private_address_found=false
for mysql_address in "${mysql_addresses[@]}"; do
  if [[ "$mysql_address" == 10.20.* ]]; then
    private_address_found=true
  fi
done
if [[ "$private_address_found" != true ]]; then
  printf 'Expected a 10.20.0.0/16 private endpoint address; resolved: %s\n' "${mysql_addresses[*]}" >&2
  exit 1
fi

short_commit="${actual_commit:0:12}"
evidence_directory="${MIGRATION_EVIDENCE_DIR:-$HOME/swiftpay-migration-evidence-$short_commit}"
mkdir -p "$evidence_directory"

sha256sum \
  "$migration_directory/V1__authoritative_baseline.sql" \
  "$migration_directory/V2__admin_high_value_merchant_procedures.sql" \
  | tee "$evidence_directory/migration-files.sha256"
printf '%s\n' "$actual_commit" > "$evidence_directory/approved-commit.txt"
printf '%s\n' "${mysql_addresses[@]}" > "$evidence_directory/mysql-private-addresses.txt"

mkdir -p "$MYSQL_DRIVER_DIRECTORY"
chmod 0700 "$MYSQL_DRIVER_DIRECTORY"

if ! printf '%s  %s\n' "$MYSQL_CONNECTOR_SHA256" "$MYSQL_DRIVER_JAR" |
  sha256sum --check --status 2>/dev/null; then
  driver_download="$MYSQL_DRIVER_JAR.download"
  rm -f "$driver_download"
  curl --fail --location --proto '=https' --tlsv1.2 \
    --output "$driver_download" \
    "$MYSQL_CONNECTOR_URL"

  if ! printf '%s  %s\n' "$MYSQL_CONNECTOR_SHA256" "$driver_download" |
    sha256sum --check --status; then
    rm -f "$driver_download"
    echo "Downloaded MySQL Connector/J checksum did not match the approved value." >&2
    exit 1
  fi

  mv "$driver_download" "$MYSQL_DRIVER_JAR"
  chmod 0600 "$MYSQL_DRIVER_JAR"
fi

sha256sum "$MYSQL_DRIVER_JAR" | tee "$evidence_directory/mysql-connector-j.sha256"
docker image inspect "$FLYWAY_IMAGE" >/dev/null 2>&1 || docker pull "$FLYWAY_IMAGE"

export FLYWAY_URL="jdbc:mysql://${MYSQL_FQDN}:3306/${MYSQL_DATABASE}?sslMode=VERIFY_IDENTITY&serverTimezone=UTC&disableMariaDbDriver=true"
export FLYWAY_USER="$MIGRATION_USER"
export FLYWAY_LOCATIONS="filesystem:/flyway/sql"
export FLYWAY_DEFAULT_SCHEMA="$MYSQL_DATABASE"
export FLYWAY_SCHEMAS="$MYSQL_DATABASE"
export FLYWAY_CREATE_SCHEMAS="false"
export FLYWAY_BASELINE_ON_MIGRATE="false"
export FLYWAY_CLEAN_DISABLED="true"
export FLYWAY_OUT_OF_ORDER="false"
export FLYWAY_VALIDATE_MIGRATION_NAMING="true"

IFS= read -r -s -p "Password for ${MIGRATION_USER}: " FLYWAY_PASSWORD
echo
if [[ -z "$FLYWAY_PASSWORD" ]]; then
  echo "The migration password cannot be empty." >&2
  exit 1
fi
export FLYWAY_PASSWORD
trap 'unset FLYWAY_PASSWORD' EXIT

run_flyway() {
  docker run --rm \
    --env FLYWAY_URL \
    --env FLYWAY_USER \
    --env FLYWAY_PASSWORD \
    --env FLYWAY_LOCATIONS \
    --env FLYWAY_DEFAULT_SCHEMA \
    --env FLYWAY_SCHEMAS \
    --env FLYWAY_CREATE_SCHEMAS \
    --env FLYWAY_BASELINE_ON_MIGRATE \
    --env FLYWAY_CLEAN_DISABLED \
    --env FLYWAY_OUT_OF_ORDER \
    --env FLYWAY_VALIDATE_MIGRATION_NAMING \
    --volume "$MYSQL_DRIVER_JAR:/flyway/drivers/mysql-connector-j-${MYSQL_CONNECTOR_VERSION}.jar:ro" \
    --volume "$migration_directory:/flyway/sql:ro" \
    "$FLYWAY_IMAGE" "$@"
}

echo "Inspecting the target before any schema mutation..."
run_flyway info | tee "$evidence_directory/01-info-before.txt"
run_flyway -ignoreMigrationPatterns="*:pending" validate |
  tee "$evidence_directory/02-validate-before.txt"

confirmation=""
read -r -p "Type MIGRATE-$short_commit to apply V1/V2 to production: " confirmation
if [[ "$confirmation" != "MIGRATE-$short_commit" ]]; then
  echo "Migration cancelled; no Flyway migrate command was run." >&2
  exit 1
fi

run_flyway migrate | tee "$evidence_directory/03-migrate.txt"
run_flyway validate | tee "$evidence_directory/04-validate-after.txt"
run_flyway info | tee "$evidence_directory/05-info-after.txt"
run_flyway migrate | tee "$evidence_directory/06-repeat-migrate-no-op.txt"

echo
echo "Migration commands completed. Review and retain evidence in: $evidence_directory"
echo "Next: inspect flyway_schema_history, create/test swiftpayapp, and remove the temporary host."
