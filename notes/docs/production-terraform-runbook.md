# SwiftPay production on Azure with Terraform and GitHub Actions

This runbook provisions the production Azure platform, configures passwordless
GitHub-to-Azure authentication, and deploys the frontend and backend. It is the
companion to the Terraform under `infra/` and the production workflows under
`.github/workflows/`.

The code in this repository has not been applied to an Azure subscription by
this change. Review the plan, expected cost, region capabilities, and security
requirements before approving an apply.

## Architecture

```text
                           GitHub Actions
                  OIDC / no Azure client secrets
                    |                       |
                    | infra identity        | backend deploy identity
                    v                       v
             Terraform state          App Service staging slot
             in private container            |
                                               | health check + swap
User browser                                    v
   |---- HTTPS ----> Static Web Apps      App Service production
   |                                           |
   `------------- HTTPS API + SSE -------------'
                                               |
                                      outbound VNet integration
                                               |
                         +---------------------+--------------------+
                         |                     |                    |
                         v                     v                    v
                MySQL delegated subnet  Key Vault PE/DNS     Blob PE/DNS
                no public access         no public access     no public access
                                               |
                                      Log Analytics + App Insights
```

### Why the backend has public ingress

The React application is a static SPA. Its API and Server-Sent Events (SSE)
requests originate in the user's browser, not from an Azure Static Web Apps
server inside the VNet. App Service VNet integration is outbound-only.

Therefore, this baseline keeps the App Service HTTPS endpoint public, restricts
browser origins with exact CORS values, and makes the data services private. Do
not set App Service public access to disabled unless you also add a supported
public gateway and update the frontend URL. CORS is a browser policy, not a
network firewall.

Azure Static Web Apps' linked-backend feature is not used because its request
duration and network-isolation model do not fit the application's long-lived
SSE endpoint. Azure Front Door is also not a transparent SSE solution. If the
API needs private ingress, evaluate Application Gateway WAF_v2 with response
buffering disabled, a custom domain/certificate, private App Service ingress,
and a VNet-capable deployment runner as a separate architecture change.

## What Terraform creates

### Bootstrap stack

`infra/bootstrap` creates long-lived prerequisites:

- Production resource group
- Terraform-state resource group, Storage account, and private state container
- Blob versioning and soft-delete protection for state
- A user-assigned identity for production Terraform CI
- A separate user-assigned identity for backend deployments
- GitHub environment-scoped federated identity credentials
- Least-scope role assignments for state, production infrastructure, and RBAC

Bootstrap has its own state because the remote state storage cannot be its own
backend until after the first apply.

### Production stack

`infra/environments/prod` creates:

- VNet and dedicated App Service, MySQL, and private-endpoint subnets
- NAT gateway and stable outbound public IP for App Service egress
- Private DNS and networking for MySQL, Key Vault, and Blob Storage
- MySQL Flexible Server and the `swiftpay` database
- Key Vault, caller-supplied application secrets written through ephemeral/
  write-only Terraform fields, private endpoint, RBAC, soft delete, and purge
  protection
- Storage account, private `kyc-documents` container, private endpoint, managed
  identity RBAC, versioning, and delete retention
- Linux App Service Plan, Java 17 App Service, and `staging` deployment slot
- Azure Static Web Apps Standard frontend
- Log Analytics, workspace-based Application Insights, diagnostic settings,
  alerts, and optional autoscaling controls
- A production delete lock when enabled

The application and staging slot share a dedicated user-assigned runtime
identity. Terraform grants it only Key Vault secret-read and KYC container Blob
write access before either app is created. It is intentionally different from
both GitHub identities.

## Repository layout

```text
infra/
|-- bootstrap/                  one-time state, identities, OIDC, and prod RG
`-- environments/prod/          production network, data, apps, and monitoring

.github/workflows/
|-- prod-infrastructure.yml     validate, plan, and explicitly approved apply
|-- prod-backend.yml            Maven test, staging deploy, health, swap, rollback
`-- prod-frontend.yml           npm install, lint, build, Static Web Apps deploy

notes/docs/
|-- production-terraform-runbook.md
|-- dev-vnet-portal-guide.md
`-- database-migration-production-gate.md
```

Never commit `terraform.tfvars`, `*.tfstate`, plan files, `.terraform/`, Azure
credentials, deployment tokens, or generated backend configuration containing
real environment values.

## 1. Prerequisites

You need:

- An Azure subscription and permission to create resource groups, identities,
  role assignments, and the listed services
- Azure CLI authenticated to the correct tenant and subscription
- Terraform 1.15.8 (the exact CI version and a match for `versions.tf`)
- A GitHub repository where you can create Actions environments, variables,
  secrets, and protection rules
- An Azure region that supports the selected App Service and MySQL SKUs and,
  if enabled, zone-redundant MySQL
- Quota for the selected App Service Plan and MySQL compute
- A globally unique lowercase suffix for Storage, Key Vault, App Service, MySQL,
  and Static Web Apps names
- An email address for Azure Monitor notifications

Confirm the target subscription before every local apply:

```powershell
az login
az account set --subscription "<subscription-id-or-name>"
az account show --query "{name:name,id:id,tenantId:tenantId}" --output table
```

Because bootstrap creates resource groups and registers the required Azure
resource providers, its human operator normally needs **Contributor** at the
subscription and **Role Based Access Control Administrator** (or equivalent
narrow delegation) at a scope containing the two new resource groups. An
organization can pre-register providers and pre-create/delegate the resource
groups to reduce this one-time scope. The GitHub identities do not need
subscription-wide Owner.

## 2. Make the production choices first

Review `infra/environments/prod/terraform.tfvars.example` and decide:

- Primary Azure region
- Static Web Apps control-plane region
- Unique name suffix
- VNet CIDR ranges that do not overlap corporate/VPN networks
- App Service SKU and instance count
- MySQL SKU, storage, backup retention, and HA mode
- Storage replication mode
- Alert email and thresholds
- Resource-lock policy

Do not guess at zone redundancy. Confirm the chosen region and subscription can
create the requested MySQL HA mode and App Service SKU. A failed availability
check is a reason to change the reviewed variable, not to silently weaken it.
Static Web Apps is not available in Central India; the checked-in default is
East Asia, while the application/data resources default to Central India.

### Cost warning

Premium App Service, two or more workers, MySQL General Purpose with HA, NAT
Gateway, private endpoints, Static Web Apps Standard, and retained logs all have
ongoing cost. Use Azure Pricing Calculator and set a subscription/resource-group
budget before approval. Do not shrink production SKUs merely to make the first
apply cheaper without documenting the availability and performance impact.

## 3. Create the two GitHub environments

In GitHub, open **Settings > Environments** and create:

1. `production-infrastructure`
2. `production`

For both environments:

- Add required reviewers who are not the workflow author where possible.
- Prevent administrators from bypassing protection for normal releases.
- Restrict `production-infrastructure` to `main`. For `production`, allow
  `main` for deliberate manual deployments and stable release tags such as
  `v*` created from `main`.
- Keep environment approvers separate from day-to-day commit authors for real
  financial or identity data.

The infrastructure workflow has two sequential jobs referencing
`production-infrastructure`. Required-reviewer rules therefore create one gate
before the plan and a second gate before apply. Do not approve the second job
until its preceding plan log has been reviewed. If your GitHub plan does not
support required reviewers for this repository, use an equivalent protected
runner/change-management gate or Terraform Cloud rather than removing approval.

The names are security-sensitive: they are embedded in the OIDC subject claims.
If you rename an environment, update and apply the bootstrap federated
credentials before expecting login to work.

## 4. Bootstrap state, identities, and OIDC

From the repository root:

```powershell
Set-Location infra/bootstrap
Copy-Item terraform.tfvars.example terraform.tfvars
```

Edit the untracked `terraform.tfvars` with the subscription ID, region, unique
suffix, GitHub organization, repository, and the exact environment names. The
owner and repository are case-sensitive OIDC claim inputs; verify they match
`git remote get-url origin` (`adityamaheshwari-25/SwiftPay` in this checkout).
Then:

```powershell
terraform fmt -check -recursive
terraform init -backend=false
terraform validate
terraform plan -out bootstrap.tfplan
terraform apply bootstrap.tfplan
terraform output
```

Read the plan before applying. Expected sensitive operations are identity and
RBAC creation; there should be no unrelated existing-resource changes.

The bootstrap outputs provide:

- Remote-state resource group, account, container, and recommended state keys
- Azure tenant and subscription IDs
- Terraform CI identity client/principal IDs
- Backend deploy identity client/principal IDs
- Production resource group name

### Migrate bootstrap state to Azure Storage

State Blob access is a data-plane permission: Azure Contributor alone is not
enough. Temporarily grant the signed-in bootstrap operator **Storage Blob Data
Contributor**, copy both backend examples, and migrate with Entra authentication:

```powershell
$operatorObjectId = az ad signed-in-user show --query id --output tsv
$stateResourceGroup = terraform output -raw state_resource_group_name
$stateAccount = terraform output -raw state_storage_account_name
$stateAccountId = az storage account show `
  --resource-group $stateResourceGroup `
  --name $stateAccount `
  --query id `
  --output tsv

$temporaryRoleAssignmentId = az role assignment create `
  --assignee-object-id $operatorObjectId `
  --assignee-principal-type User `
  --role "Storage Blob Data Contributor" `
  --scope $stateAccountId `
  --query id `
  --output tsv

terraform output bootstrap_backend_config_local
Copy-Item backend.tf.example backend.tf
Copy-Item backend.hcl.example backend.hcl
# Replace backend.hcl placeholders with bootstrap_backend_config_local values.
terraform init -migrate-state -backend-config=backend.hcl
terraform state list

# Remove the temporary human data-plane grant after remote state is verified.
az role assignment delete --ids $temporaryRoleAssignmentId
```

RBAC propagation can take several minutes; if migration initially returns 403,
confirm the assignment and retry rather than enabling account keys. Answer the
migration prompt only after confirming the account and the distinct
`swiftpay/bootstrap.tfstate` key. Verify the remote blob/versioning, then move
any leftover local-state backup to an approved encrypted secret store. Do not
delete it until the remote state can be read successfully.

The state endpoint remains reachable by GitHub-hosted runners, but access is
authorized through Entra ID and limited to the Terraform CI identity. The
database/JWT/superadmin inputs are ephemeral and use write-only provider fields,
so their values are not persisted in state or saved plans. State is still
sensitive: the Static Web Apps resource exposes its deployment key to the
provider, and state contains detailed production metadata. It is encrypted at
rest, versioned, soft-delete protected, and protected by a delete lock.

## 5. Configure the `production-infrastructure` environment

Add the exact values emitted by bootstrap. Keep the names used by
`prod-infrastructure.yml`; do not invent aliases.
`terraform output production_infrastructure_github_environment_variables`
prints the non-secret keys already aligned to this workflow.

Add these environment **secrets**:

| GitHub value | Source |
| --- | --- |
| `AZURE_CLIENT_ID` | Terraform CI user-assigned identity client ID |
| `AZURE_TENANT_ID` | Bootstrap tenant ID output |
| `AZURE_SUBSCRIPTION_ID` | Target subscription ID |
| `TF_VAR_database_admin_password` | Strong MySQL admin password from the approved secret manager |
| `TF_VAR_database_application_password` | Different password for the least-privilege `swiftpayapp` runtime login; at least 16 characters |
| `TF_VAR_jwt_secret` | At least 32 high-entropy characters from the approved secret manager |
| `TF_VAR_superadmin_password` | Strong initial superadmin password from the approved secret manager |
| `TF_PLAN_ENCRYPTION_PASSPHRASE` | At least 32 random characters used only to encrypt the saved plan handoff |

Add these environment **variables**:

| GitHub value | Source/example |
| --- | --- |
| `TF_STATE_RESOURCE_GROUP` | Bootstrap state resource group output |
| `TF_STATE_STORAGE_ACCOUNT` | Bootstrap state account output |
| `TF_STATE_CONTAINER` | Bootstrap container output |
| `TF_STATE_KEY` | `swiftpay/prod.tfstate` |
| `TF_VAR_resource_group_name` | Bootstrap production resource-group output |
| `TF_VAR_backend_deploy_principal_object_id` | Backend deploy identity **principal/object** ID output |
| `TF_VAR_unique_suffix` | Same 3-8 character suffix used by bootstrap |
| `TF_VAR_superadmin_email` | Initial production superadmin email |
| `TF_VAR_alert_email_receivers` | JSON map, for example `{"platform":"ops@example.com"}` |
| `TF_VAR_static_web_app_location` | Optional; defaults to `East Asia` |

The workflow rejects missing values, malformed UUIDs, an empty alert map, weak
minimum secret lengths, and an unsupported Static Web Apps region before Azure
login. Keep all four ephemeral secret values present for both plan and apply:
because they are ephemeral, Terraform deliberately does not recover them from a
plan. Keep the plan-encryption passphrase stable for the duration of an apply
run and rotate it through normal secret-management procedures.

Do not configure an Azure client secret. `id-token: write` plus the federated
credential is the authentication mechanism.

## 6. Validate and apply production infrastructure

For a local Azure CLI-authenticated review, create untracked `backend.hcl` and
`terraform.tfvars` files from their examples. `backend.hcl.example` is for a
human logged in with `az login`; `backend.oidc.hcl.example` is for CI. The human
also needs a temporary **Storage Blob Data Contributor** grant on the state
account while running local state operations; remove it afterward. The protected
workflow is the preferred routine path.

```powershell
Set-Location ../environments/prod
Copy-Item backend.hcl.example backend.hcl
Copy-Item terraform.tfvars.example terraform.tfvars
terraform init -backend-config=backend.hcl
terraform fmt -check -recursive
terraform validate

# Populate these only in the current process from your approved secret manager.
$env:TF_VAR_database_admin_password = "<secret-manager-value>"
$env:TF_VAR_database_application_password = "<different-secret-manager-value>"
$env:TF_VAR_jwt_secret = "<secret-manager-value>"
$env:TF_VAR_superadmin_password = "<secret-manager-value>"

terraform plan -out prod.tfplan
terraform apply prod.tfplan
```

The four ephemeral variables must still be present when applying a saved plan.
Clear the process environment after the session and never place real values in
`terraform.tfvars`, command arguments, source control, or a shared transcript.

Check the plan for:

- The exact subscription and production resource group
- No overlap with dev resources or CIDRs
- Public access disabled on MySQL, Key Vault, and KYC Storage
- App Service connected only to the App Service subnet
- MySQL alone in its delegated subnet
- Private endpoints only in the private-endpoint subnet
- The shared runtime identity has both Key Vault and Blob roles and is attached
  to production and staging
- The backend deploy identity scoped to the web app, not the subscription
- The intended SKU, instance count, backup, diagnostics, alert, and lock values

The supported automated path is:

1. Push the Terraform change through a reviewed pull request. The infrastructure
   workflow runs formatting and validation without production credentials.
2. For an advisory review, run **Prod infrastructure** with operation `plan`,
   approve its plan job, and inspect the log.
3. When ready, dispatch operation `apply` from the approved `main` revision.
4. Approve the first `production-infrastructure` gate. The plan job creates and
   logs a saved plan, encrypts it with GnuPG/AES-256, uploads only the encrypted
   plan plus checksum, and retains that artifact for one day.
5. Review that exact plan in the completed plan-job log.
6. Approve the now-pending second environment gate. The apply job downloads,
   decrypts, checksum-verifies, displays, and applies that identical saved plan.

The raw plan is never uploaded. Terraform also rejects the saved plan if the
remote state changed after planning. Do not rotate the ephemeral input secrets
or plan-encryption passphrase between the two jobs.

An apply is intentionally not triggered by every push to `main`. Production
mutation requires an explicit dispatch and environment approval.

## 7. Record Terraform outputs

After apply, read only the required non-secret outputs from the workflow or
local CLI:

```powershell
terraform output resource_group_name
terraform output backend_web_app_name
terraform output mysql_application_username
terraform output github_environment_variables
terraform output frontend_repository_variables
terraform output nat_gateway_public_ip
```

Do not use bulk `terraform output -json`: Terraform's JSON output includes
sensitive output values in plaintext. Retrieve/reset the Static Web Apps token
through its controlled Azure Portal screen and place it directly into the
GitHub environment secret.

You need at least:

- Production resource group
- Backend App Service name and URL
- Staging slot hostname
- Static Web App name, hostname, and API base URL
- Key Vault, MySQL, Storage, and monitoring resource names
- NAT egress IP

Do not paste application passwords/keys or the Static Web Apps deployment token into
issues, workflow logs, chat, or documentation.

## 8. Configure the `production` deployment environment

Under GitHub **Settings > Environments > production**, add:
`terraform output production_github_environment_variables` from bootstrap maps
the non-secret Azure values to the exact backend workflow keys.

### Variables

| Variable | Value |
| --- | --- |
| `AZURE_RESOURCE_GROUP` | Terraform production resource-group output |
| `AZURE_WEBAPP_NAME` | Terraform backend app-name output |

### Secrets

| Secret | Value |
| --- | --- |
| `AZURE_CLIENT_ID` | Backend deploy identity client ID from bootstrap |
| `AZURE_TENANT_ID` | Azure tenant ID |
| `AZURE_SUBSCRIPTION_ID` | Azure subscription ID |
| `AZURE_STATIC_WEB_APPS_API_TOKEN` | Production Static Web App deployment token |

Retrieve/reset the Static Web Apps token from **Static Web App > Overview >
Manage deployment token**. It is the only deployment secret required by the
frontend action. Rotate it immediately if it appears in logs or screenshots.

The backend identity uses OIDC and receives only the web-app scope required for
deployment and slot swap. It must not reuse the highly privileged Terraform
identity.

The frontend build job deliberately does not enter the protected environment,
so add both values from `frontend_repository_variables` as **repository or
organization Actions variables**:

| Variable | Required form |
| --- | --- |
| `PRODUCTION_API_BASE_URL` | `https://<backend-host>/api/v1`, no trailing slash |
| `PRODUCTION_FRONTEND_URL` | Exact Static Web Apps HTTPS origin, no trailing slash |

## 9. First deployment

### Backend

Open **Actions > Production backend CI/CD > Run workflow** on the reviewed
release commit.

The workflow:

1. Runs all Maven tests on Java 17.
2. Selects exactly one executable Spring Boot JAR.
3. Passes that immutable artifact to the protected deploy job.
4. Authenticates to Azure with OIDC.
5. Starts and deploys to the normally stopped `staging` App Service slot.
6. Keeps scheduled jobs disabled in staging through the sticky
   `SCHEDULING_ENABLED=false` slot setting.
7. Requires `/actuator/health` to report `UP`.
8. Swaps staging into production.
9. Rechecks production and swaps the old build back if the post-swap check
   fails.
10. Stops staging in an `always()` cleanup, including after staging/deploy/
    rollback failures.

Production Terraform defaults to `JPA_DDL_AUTO=validate`. The first backend
deployment will intentionally fail until the reviewed V1/V2 Flyway migrations
and the least-privilege runtime login have been applied from inside the VNet.
Flyway remains disabled during ordinary App Service startup. Terraform
deliberately does not create MySQL users or execute application SQL.
Do not switch staging to `update`: staging uses the production database, so its
health check could mutate schema before approval or swap. Complete
[`database-migration-production-gate.md`](database-migration-production-gate.md)
before running this workflow.

### Frontend

Open **Actions > Prod frontend CI/CD > Run workflow** on the same reviewed
release commit.

The workflow validates the HTTPS API URL, installs exactly the locked npm
dependencies, runs ESLint, builds Vite, restricts production `connect-src` to
the exact API origin, and uploads the prebuilt `dist` folder. It stamps the
artifact with the commit SHA and verifies that the expected Static Web App—not
merely whichever app owns the supplied token—serves that SHA.

Vite variables are compiled into the browser bundle. Changing a Static Web Apps
runtime setting alone does not change `VITE_API_BASE_URL`; rebuild and redeploy.

### Normal releases

Publish an immutable, non-prerelease `vMAJOR.MINOR.PATCH` GitHub release/tag from
a reviewed commit on `main`.
Both application workflows respond to the published release, independently wait
for the `production` approval, and deploy that tagged revision. Prereleases are
not deployed. Manual production dispatches accept only `main`. Keep dev
deployment on `main`; do not introduce a long-lived production branch that can
drift.

## 10. End-to-end verification

Perform all checks after infrastructure and both apps are deployed:

1. `https://<backend-host>/actuator/health` returns HTTP 200 and only an `UP`
   status. `/actuator/env` and other actuator endpoints must not be available.
2. The production frontend loads directly and through a deep SPA route.
3. Browser developer tools show API requests going only to the exact production
   HTTPS API URL.
4. Registration/login, wallet read, a reversible test transaction, KYC upload
   and retrieval, and an SSE notification work.
5. App Service environment-variable diagnostics show all Key Vault references
   resolved for both production and staging slots.
6. From App Service, MySQL, Vault, and Blob hostnames resolve to private
   addresses; the data services reject public data-plane access.
7. The App Service identity, not a key/connection string, authorizes Blob access.
8. Application Insights receives requests, dependency calls, exceptions, and
   availability/health information.
9. Azure Monitor alerts reach the configured action-group email through a
   controlled test.
10. The NAT output IP matches observed backend internet egress.
11. A production database restore is tested into a separate server; a backup is
    not proven until a restore succeeds.
12. Resource and state locks/retention behave as documented.

## 11. Deployment rollback

### Backend

The workflow automatically swaps the old slot back if the production health
check fails. If a later functional defect is found, rerun the workflow from the
last known-good release tag. Do not redeploy an unreviewed local JAR.

A slot rollback cannot reverse an incompatible database change. Database schema
changes must be backward-compatible across the old and new application during
the deployment window.

### Frontend

Rerun `prod-frontend.yml` from the last known-good release. Static Web Apps
deployment is immutable at the build-output level, but the repository release
tag is the authoritative rollback source.

### Infrastructure

Revert the Terraform code in a pull request, run a new plan, and apply the
reviewed reversal. Never use `terraform destroy` as incident rollback. Do not
remove the production delete lock merely to force through an unexplained plan.

## 12. Secret rotation

- **Static Web Apps token:** reset it in Azure, update the GitHub environment
  secret, and run a frontend deployment.
- **MySQL administrator password:** rotate through a planned Terraform change by
  incrementing `database_admin_password_version`. It is migration/operator-only
  and is never written to the application's Key Vault secrets.
- **MySQL application password:** coordinate the database `ALTER USER` and the
  new `TF_VAR_database_application_password` during a maintenance window,
  increment `database_application_password_version`, apply Terraform, restart
  both slots, verify connectivity, and remove the old credential if a dual-
  password rotation was used. Never grant the runtime login DDL/admin rights.
- **JWT signing key:** rotate during a maintenance window; existing tokens become
  invalid. Restart both slots and test login.
- **Superadmin bootstrap password:** the current bootstrap component uses it only
  when creating the first superadmin row. Changing the Key Vault secret later
  does not automatically update the existing database password.
- **OIDC:** there is no client secret to rotate. Review federated subjects,
  identity owners, role assignments, and unused credentials periodically.

Use Key Vault version history and audit logs during rotation. The four
secret inputs use ephemeral/write-only fields, but the Terraform state is
still sensitive because the Static Web Apps provider state includes its
deployment key and detailed infrastructure metadata.

## 13. Backup, recovery, and operations

- Keep MySQL point-in-time backups for the reviewed retention period and test
  restores quarterly or after material schema changes.
- Enable geo-redundant database backups only where the selected region, RTO/RPO,
  and data-residency policy justify them.
- Blob versioning and soft delete protect accidental changes; they are not a
  substitute for a tested cross-region recovery policy.
- Key Vault purge protection is intentionally difficult to reverse.
- Terraform state versioning helps recover accidental state writes. Restrict
  state readers because the state contains sensitive deployment data.
- Review Application Insights sampling/retention, Log Analytics cost, alerts,
  App Service capacity, MySQL CPU/storage/connections, and Blob growth.
- Patch application dependencies through normal pull requests and rebuilds; do
  not modify generated artifacts on App Service.
- Run an access review for the bootstrap operator, Terraform identity, deploy
  identity, App Service identities, and GitHub environment reviewers.

## 14. Application readiness gates before real production data

Terraform can make the platform private, observable, and recoverable; it cannot
make application behavior horizontally safe. The current code has four explicit
gates:

1. Three Spring `@Scheduled` jobs run in every backend instance. Before setting
   App Service minimum instances above one, add a distributed scheduler lock
   such as a database-backed ShedLock or move jobs to a singleton job service.
2. SSE emitters and Caffeine caches are in process memory. Multiple instances can
   miss cross-instance notifications and hold different short-lived cache
   entries. Add a shared event broker/backplane and distributed cache, or accept
   and test the documented behavior before scaling out.
3. Hibernate schema mutation is not a controlled production migration strategy.
   Production is locked to `JPA_DDL_AUTO=validate`. Complete the separate
   [database migration production gate](database-migration-production-gate.md),
   including MySQL validation of V1 and V2 and the least-privilege runtime login,
   before the first deployment.
4. The SSE client sends its JWT in the query string because browser EventSource
   cannot set an Authorization header. URLs can be captured in access logs and
   telemetry. Replace this with a short-lived, one-purpose stream ticket or a
   reviewed cookie-based design before processing sensitive production traffic.

For safety, keep the backend at one active production instance until items 1 and
2 are addressed. This is an application availability tradeoff, not a Terraform
limitation. Do not claim multi-instance HA merely because the App Service Plan
can autoscale.

Also complete threat modeling, penetration testing, privacy/data-retention
review, fraud controls, rate limiting, dependency/SAST/secret scanning, and an
incident-response exercise before treating a payment-like demo as a regulated
payment system.

## 15. Custom domain and edge security

No domain name or DNS zone was supplied, so Terraform does not fabricate a
production hostname or certificate. The default Azure hostnames are sufficient
for technical deployment, not a polished public launch.

Before launch, decide and separately review:

- Custom frontend and API domains
- DNS ownership and certificate lifecycle
- WAF/rate-limiting architecture that is compatible with SSE
- Whether App Service ingress should become private behind Application Gateway
- A self-hosted/VNet-connected runner or another deployment path if SCM is made
  private

These choices materially affect DNS, certificates, ingress, cost, and CI/CD and
should not be guessed in reusable Terraform.

## 16. Safe teardown

Production teardown is intentionally difficult.

1. Export/verify required financial, audit, KYC, and database records under the
   applicable retention policy.
2. Obtain explicit approval for the exact production resource group.
3. Disable/remove the production delete lock through a reviewed Terraform
   change.
4. Run and save a destroy plan; inspect every target.
5. Understand that Key Vault purge protection retains the deleted vault and
   prevents immediate name reuse.
6. Destroy the production stack before bootstrap. Verify its remote state no
   longer manages live resources.
7. Before destroying bootstrap, temporarily regain Storage Blob Data Contributor
   as in section 4, move bootstrap state back to a protected local backend, and
   verify it:

   ```powershell
   Set-Location infra/bootstrap
   Move-Item backend.tf backend.tf.remote
   terraform init -migrate-state
   terraform state list
   ```

8. Only after the local state is safely readable, plan/apply bootstrap destroy
   from that local backend. Terraform removes the state-account lock before the
   account. Remove the temporary human role assignment after migration.

Never destroy a storage account while Terraform is still using it as its own
backend, never delete the state account first, and never use ad-hoc portal
deletion to work around a Terraform error.

## Official references

- [Authenticate GitHub Actions to Azure with OIDC](https://learn.microsoft.com/azure/developer/github/connect-from-azure-openid-connect)
- [Terraform `azurerm` backend](https://developer.hashicorp.com/terraform/language/backend/azurerm)
- [App Service VNet integration](https://learn.microsoft.com/azure/app-service/overview-vnet-integration)
- [App Service deployment slots](https://learn.microsoft.com/azure/app-service/deploy-staging-slots)
- [App Service Key Vault references](https://learn.microsoft.com/azure/app-service/app-service-key-vault-references)
- [MySQL Flexible Server private networking](https://learn.microsoft.com/azure/mysql/flexible-server/concepts-networking-vnet)
- [Storage private endpoints](https://learn.microsoft.com/azure/storage/common/storage-private-endpoints)
- [Key Vault private link](https://learn.microsoft.com/azure/key-vault/general/private-link-service)
- [App Service health checks](https://learn.microsoft.com/azure/app-service/monitor-instances-health-check)
- [Static Web Apps build configuration](https://learn.microsoft.com/azure/static-web-apps/build-configuration)
- [GitHub deployment environments](https://docs.github.com/actions/deployment/targeting-different-environments/managing-environments-for-deployment)
