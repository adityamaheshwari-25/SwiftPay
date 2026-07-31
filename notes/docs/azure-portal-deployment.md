# Azure Portal deployment runbook

This runbook deploys the application into one Azure subscription with isolated
development and production resource groups.

## Target resource layout

Use a short globally unique suffix (for example, your initials plus four
digits) wherever Azure requires a globally unique name.

```text
Subscription
├── rg-swiftpay-dev
│   ├── swa-swiftpay-dev-<suffix>       Azure Static Web Apps
│   ├── plan-swiftpay-dev               Linux App Service Plan
│   ├── app-swiftpay-api-dev-<suffix>   App Service
│   ├── mysql-swiftpay-dev-<suffix>     MySQL Flexible Server
│   ├── kv-swiftpay-dev-<suffix>        Key Vault
│   ├── stswiftpaydev<suffix>           Storage account
│   │   └── kyc-documents               Private blob container
│   ├── appi-swiftpay-dev               Application Insights
│   └── vnet-swiftpay-dev               Optional; not used by current dev
│
└── rg-swiftpay-prod
    ├── swa-swiftpay-prod-<suffix>
    ├── plan-swiftpay-prod
    ├── app-swiftpay-api-prod-<suffix>
    ├── mysql-swiftpay-prod-<suffix>
    ├── kv-swiftpay-prod-<suffix>
    ├── stswiftpayprod<suffix>
    │   └── kyc-documents
    ├── appi-swiftpay-prod
    └── vnet-swiftpay-prod
```

The App Service Plan and Storage account are required additions to the original
layout. A Log Analytics workspace may also appear because workspace-based
Application Insights uses one.

## 1. Select the subscription and create resource groups

1. Sign in to the [Azure portal](https://portal.azure.com).
2. Search for **Subscriptions** and select the subscription that will own both
   environments. If no subscription exists, choose **Add**; the available
   billing offers depend on the organization's billing account and permissions.
3. Search for **Resource groups** and select **Create**.
4. Select the subscription, enter `rg-swiftpay-dev`, choose one primary Azure
   region, add the tag `Environment=Development`, and create it.
5. Repeat for `rg-swiftpay-prod` with `Environment=Production`.

Keep the App Service, database, Key Vault, storage account, Application Insights,
and virtual network for an environment in the same region whenever the service
supports that region.

## 2. Plan virtual networking

The current development environment intentionally uses public service endpoints
and does not require a VNet. Skip VNet integration for dev and follow the
restricted firewall instructions in section 8.

Before creating production:

1. Search for **Virtual networks** and choose **Create**.
2. Create `vnet-swiftpay-prod` in `rg-swiftpay-prod` with address space
   `10.20.0.0/16`.
3. Add `snet-appservice` as `10.20.1.0/24`.
4. Add `snet-mysql` as `10.20.2.0/24` and delegate it to
   `Microsoft.DBforMySQL/flexibleServers`.

Do not place other resources in the delegated MySQL subnet.

## 3. Create Azure Database for MySQL Flexible Server

Repeat these steps once in each resource group.

1. Search for **Azure Database for MySQL flexible servers** and choose
   **Create**.
2. Select the corresponding resource group and use
   `mysql-swiftpay-<env>-<suffix>`.
3. Choose a currently supported MySQL version and the same region as the
   backend. Select a small burstable compute size for development and a
   production-appropriate general-purpose size, availability, backup retention,
   and redundancy for production.
4. Choose MySQL authentication, create a unique administrator username, and
   generate a strong password. Do not reuse the dev credentials in production.
5. Choose networking by environment:
   - Current dev: **Public access**, TLS required, and firewall rules restricted
     to the App Service outbound addresses as described in section 8.
   - Future production: **Private access (VNet Integration)** using
     `vnet-swiftpay-prod` and `snet-mysql`; let Azure create/select the linked
     private DNS zone.
6. Create the server.
7. Open the server and use **Settings > Databases > Add** to create a database
   named `swiftpay`. If the portal does not show that blade, use the portal's
   Cloud Shell or MySQL client to run `CREATE DATABASE swiftpay;`.
8. Copy the server hostname from **Overview**.

The JDBC value used by the application is:

```text
jdbc:mysql://<server>.mysql.database.azure.com:3306/swiftpay?sslMode=REQUIRED&serverTimezone=UTC
```

## 4. Create Key Vault and secrets

Repeat per environment.

1. Search for **Key vaults**, choose **Create**, and use
   `kv-swiftpay-<env>-<suffix>`.
2. Select **Azure role-based access control** as the permission model. Leave
   soft delete enabled and enable purge protection for production.
3. After creation, open **Objects > Secrets** and create:

   - `DB-URL`
   - `DB-USERNAME`
   - `DB-PASSWORD`
   - `JWT-SECRET` (at least 32 random characters)
   - `SUPERADMIN-EMAIL`
   - `SUPERADMIN-PASSWORD`

Your own account needs a role such as **Key Vault Secrets Officer** to create
secrets. Never copy production secret values into the development vault.

## 5. Create Storage and the private Blob container

Repeat per environment.

1. Search for **Storage accounts** and choose **Create**.
2. Use a lowercase globally unique name with no hyphens, such as
   `stswiftpaydev<suffix>`. Select **Standard**, StorageV2, and at least LRS for
   development. Choose ZRS or GRS for production according to availability and
   recovery requirements.
3. Require secure transfer, set minimum TLS to 1.2 or newer, and disable
   anonymous blob access.
4. Under **Data protection**, enable blob soft delete, container soft delete,
   and versioning, especially in production.
5. After creation, open **Data storage > Containers**, choose **+ Container**,
   enter `kyc-documents`, and keep the anonymous access level **Private**.
6. Copy the Blob service endpoint from **Settings > Endpoints**.

The application does not use a storage key or connection string. It authenticates
with the App Service managed identity.

## 6. Create the Spring Boot App Service

Repeat per environment.

1. Search for **App Services**, choose **Create > Web App**, and select the
   environment resource group.
2. Set **Publish** to `Code`, **Runtime stack** to `Java 17`, **Java web server
   stack** to `Java SE`, and **Operating System** to `Linux`.
3. Create/select `plan-swiftpay-<env>`. A Basic plan is reasonable for a
   development environment; use a production tier with at least two instances
   when availability is required.
4. After creation, open **Settings > Identity**, turn the system-assigned
   identity **On**, and save.
5. Skip VNet integration for the current development App Service. For
   production, open **Networking > VNet integration > Add VNet** and select
   `vnet-swiftpay-prod` with `snet-appservice`.

### Assign managed-identity roles

1. Open the environment Key Vault, then **Access control (IAM) > Add role
   assignment**.
2. Assign **Key Vault Secrets User** to the App Service's system-assigned
   managed identity.
3. Open the environment Storage account and assign **Storage Blob Data
   Contributor** to the same managed identity.
4. Allow several minutes for role assignments to propagate.

### Configure App Service environment variables

Open **App Service > Settings > Environment variables > App settings** and add:

| Name | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `@Microsoft.KeyVault(VaultName=<vault>;SecretName=DB-URL)` |
| `DB_USERNAME` | `@Microsoft.KeyVault(VaultName=<vault>;SecretName=DB-USERNAME)` |
| `DB_PASSWORD` | `@Microsoft.KeyVault(VaultName=<vault>;SecretName=DB-PASSWORD)` |
| `JPA_DDL_AUTO` | `update` for the initial deployment |
| `CORS_ALLOWED_ORIGINS` | Exact development Static Web App URL from its Overview page |
| `JWT_SECRET` | `@Microsoft.KeyVault(VaultName=<vault>;SecretName=JWT-SECRET)` |
| `JWT_EXPIRATION` | `10h` |
| `SUPERADMIN_EMAIL` | `@Microsoft.KeyVault(VaultName=<vault>;SecretName=SUPERADMIN-EMAIL)` |
| `SUPERADMIN_PASSWORD` | `@Microsoft.KeyVault(VaultName=<vault>;SecretName=SUPERADMIN-PASSWORD)` |
| `AZURE_STORAGE_BLOB_ENDPOINT` | `https://<storage-account>.blob.core.windows.net` |
| `AZURE_STORAGE_BLOB_CONTAINER` | `kyc-documents` |

Save and restart the application. In the environment-variable list, confirm that
each Key Vault reference resolves successfully. The production profile writes
logs to the console rather than the temporary App Service filesystem.

For a mature production database, replace automatic Hibernate schema updates
with versioned Flyway or Liquibase migrations, then set `JPA_DDL_AUTO=validate`.

## 7. Deploy the backend JAR

The repository now owns the development backend workflow at
`.github/workflows/dev-backend.yml`; do not ask Deployment Center to generate a
second workflow. A push to `main` that changes `PaytmCloneBackend` runs all Maven
tests, packages the executable JAR, authenticates to Azure with OpenID Connect
(OIDC), deploys it, and verifies `/actuator/health`.

### Create the GitHub development environment

1. In `adityamaheshwari-25/SwiftPay`, open **Settings > Environments > New
   environment** and create `development`.
2. Under **Environment variables**, add:

   | Variable | Development value |
   | --- | --- |
   | `AZURE_WEBAPP_NAME` | Exact App Service resource name, without `.azurewebsites.net` |
   | `API_BASE_URL` | `https://<app-service-name>.azurewebsites.net/api/v1` |

3. Under **Environment secrets**, the completed dev setup will contain:

   - `AZURE_CLIENT_ID`
   - `AZURE_TENANT_ID`
   - `AZURE_SUBSCRIPTION_ID`
   - `AZURE_STATIC_WEB_APPS_API_TOKEN`

### Configure GitHub-to-Azure OIDC

This identity is only for CI/CD deployment. It is different from the App
Service's system-assigned managed identity, which the running application uses
for Key Vault and Blob Storage.

1. In Azure Portal, open **Microsoft Entra ID > App registrations > New
   registration** and create `github-swiftpay-dev`.
2. Copy its **Application (client) ID** and **Directory (tenant) ID**.
3. Open the registration's **Certificates & secrets > Federated credentials >
   Add credential**.
4. Choose **GitHub Actions deploying Azure resources** and enter:

   - Organization: `adityamaheshwari-25`
   - Repository: `SwiftPay`
   - Entity type: `Environment`
   - GitHub environment name: `development`

5. Open the development App Service, then **Access control (IAM) > Add role
   assignment**. Assign **Website Contributor** to the
   `github-swiftpay-dev` service principal. Scope it to this App Service rather
   than the whole subscription.
6. In Azure Portal, open **Subscriptions**, copy the subscription ID, and add
   the four values to the matching GitHub environment secrets:

   | GitHub secret | Azure value |
   | --- | --- |
   | `AZURE_CLIENT_ID` | Application (client) ID |
   | `AZURE_TENANT_ID` | Directory (tenant) ID |
   | `AZURE_SUBSCRIPTION_ID` | Azure subscription ID |

7. In GitHub, open **Actions > Dev backend CI/CD > Run workflow** for the first
   deployment, or push a backend change to `main`.
8. Open `https://<app-name>.azurewebsites.net/actuator/health`; the workflow
   also retries this check for up to three minutes and fails if the application
   does not report `UP`.

No controller is needed for the health URL; Spring Boot Actuator registers it.
All other Actuator endpoints remain disabled.

## 8. Create Azure Static Web Apps

The repository owns the development frontend workflow at
`.github/workflows/dev-frontend.yml`. It installs the locked npm dependencies,
builds `PaytmCloneFrontend/dist` with the development API URL, and uploads that
prebuilt directory to Static Web Apps.

1. Open the development Static Web App in Azure Portal.
2. From **Overview**, select **Manage deployment token** and copy/reset the
   deployment token.
3. In GitHub's `development` environment, create the secret
   `AZURE_STATIC_WEB_APPS_API_TOKEN` with that value.
4. Confirm that the `API_BASE_URL` environment variable created in section 7 is
   the exact development backend URL ending in `/api/v1`.
5. In GitHub, open **Actions > Dev frontend CI/CD > Run workflow**, or push a
   frontend change to `main`.
6. After the workflow succeeds, copy the default Static Web App URL.
7. Return to the development App Service environment variables and set
   `CORS_ALLOWED_ORIGINS` to that exact origin, for example
   `https://kind-tree-012345678.1.azurestaticapps.net`. Do not add a trailing
   slash or use `*`.
8. Restart the App Service and test sign-in and an authenticated API request
   from the Static Web App.

Vite values are compiled into the browser bundle, so setting the API URL only as
a runtime Static Web Apps setting is not sufficient.

The repository's `public/staticwebapp.config.json` supplies SPA route fallback
and basic response security headers.

### Branch strategy

The current single `main` branch is sufficient:

- Pushes to `main` automatically deploy changed backend/frontend code to dev.
- Short-lived feature branches and pull requests can be added for code review,
  but a permanent `dev` branch is not required.
- Production automation is intentionally not present yet. When production is
  created, use a separate GitHub `production` environment and deploy a release
  tag or manually approved commit from `main`. This avoids long-lived dev/prod
  branches drifting apart.

### Development networking without a VNet

Until production private networking is added:

1. Open **App Service > Properties** and copy all outbound and possible outbound
   IPv4 addresses.
2. Open the development MySQL Flexible Server **Networking** page and add
   firewall rules for those App Service outbound addresses.
3. Keep TLS required in the JDBC URL.
4. Key Vault and Storage may use public service endpoints for dev, but retain
   RBAC/managed-identity authorization and a private Blob container.
5. Do not expose MySQL to all IPv4 addresses. Production should use the planned
   VNet/private endpoint design.

## 9. Enable Application Insights and health checks

Repeat per App Service.

1. Open **App Service > Monitoring > Application Insights**.
2. Choose **Turn on Application Insights**, create or select
   `appi-swiftpay-<env>` in the same resource group, and apply. App Service can
   instrument Java without adding an Application Insights SDK dependency to the
   application.
3. Open **App Service > Monitoring > Health check**, enable it, and set the path
   to `/actuator/health`.
4. Save. For production, use two or more App Service instances so Azure can
   remove an unhealthy instance from load balancing.
5. In Application Insights, review **Live Metrics**, **Failures**, and
   **Application Map** after sending test traffic.

## 10. Migrate legacy local KYC documents

The repository previously contained KYC files under
`PaytmCloneBackend/uploads`. They are now ignored and untracked, but the local
copies were deliberately preserved to avoid data loss.

Before serving any existing KYC records from Azure:

1. Use **Storage account > Data storage > Containers > kyc-documents > Upload**
   (or Azure Storage Explorer) to upload each legacy file under an object key
   such as `kyc/user-<user-id>/<unique-name>.<extension>`.
2. Update that KYC row's existing `file_path` column to the new object key. The
   application intentionally reuses this column as `storageKey`.
3. Verify that both user and admin download endpoints return the uploaded file.
4. Only after verification and backup, securely remove the preserved local
   copies.

Do not upload the legacy directory to App Service. If those documents were ever
pushed to a remote Git repository, removing them from the current commit does
not remove them from Git history; arrange a controlled history rewrite and
credential/data-incident review before treating the repository as clean.

## 11. Final validation checklist

- Dev and prod use separate databases, vaults, storage accounts, identities,
  App Services, and frontend resources.
- Both backend URLs return `UP` from `/actuator/health`.
- Other Actuator URLs such as `/actuator/env` are unavailable.
- Key Vault references show as resolved.
- The App Service identity has only **Key Vault Secrets User** and **Storage Blob
  Data Contributor**, not broad Owner rights.
- The `kyc-documents` containers are private.
- Uploading a KYC document creates a blob and only metadata/object key in MySQL.
- Restarting or scaling the App Service does not lose uploaded documents.
- CORS lists only the matching environment's Static Web App origin.
- Static Web Apps deep links load through the SPA fallback.
- Application Insights receives requests, dependencies, and failures.
- Production backup retention, zone redundancy, scaling, budgets, alerts, and
  diagnostic retention have been reviewed.

## Official references

- [Azure resource groups](https://learn.microsoft.com/en-us/azure/azure-resource-manager/management/manage-resources-portal)
- [Create a Static Web App in the portal](https://learn.microsoft.com/en-us/azure/static-web-apps/get-started-portal)
- [Java on Azure App Service](https://learn.microsoft.com/en-us/azure/app-service/configure-language-java-deploy-run)
- [Create MySQL Flexible Server in the portal](https://learn.microsoft.com/en-us/azure/mysql/flexible-server/quickstart-create-server-portal)
- [App Service virtual network integration](https://learn.microsoft.com/en-us/azure/app-service/overview-vnet-integration)
- [Use Key Vault references in App Service](https://learn.microsoft.com/en-us/azure/app-service/app-service-key-vault-references)
- [Azure Blob Storage with Java](https://learn.microsoft.com/en-us/azure/storage/blobs/storage-quickstart-blobs-java)
- [App Service Health check](https://learn.microsoft.com/en-us/azure/app-service/monitor-instances-health-check)
- [Application Insights for Java App Service](https://learn.microsoft.com/en-us/azure/app-service/configure-language-java-apm)
- [Deploy App Service with GitHub Actions](https://learn.microsoft.com/en-us/azure/app-service/deploy-github-actions)
- [Authenticate GitHub Actions to Azure](https://learn.microsoft.com/en-us/azure/developer/github/connect-from-azure)
- [Static Web Apps build configuration](https://learn.microsoft.com/en-us/azure/static-web-apps/build-configuration)
- [GitHub deployment environments](https://docs.github.com/en/actions/concepts/workflows-and-actions/deployment-environments)
