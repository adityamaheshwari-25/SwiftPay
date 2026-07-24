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
│   └── vnet-swiftpay-dev               Virtual network
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

## 2. Create one virtual network per environment

This enables private MySQL connectivity.

1. Search for **Virtual networks** and choose **Create**.
2. Create `vnet-swiftpay-dev` in `rg-swiftpay-dev` with address space
   `10.10.0.0/16`.
3. Add `snet-appservice` as `10.10.1.0/24`.
4. Add `snet-mysql` as `10.10.2.0/24` and delegate it to
   `Microsoft.DBforMySQL/flexibleServers`.
5. Repeat in production with `vnet-swiftpay-prod`, `10.20.0.0/16`,
   `10.20.1.0/24`, and `10.20.2.0/24`.

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
5. On **Networking**, choose **Private access (VNet Integration)**. Select the
   environment VNet and `snet-mysql`. Let Azure create or select the linked
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
5. Open **Networking > VNet integration > Add VNet**, select the environment
   VNet and `snet-appservice`.

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
| `CORS_ALLOWED_ORIGINS` | Exact Static Web App URL, added after step 8 |
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

The repository is a monorepo, so the backend project root is
`PaytmCloneBackend`.

1. Open the App Service and select **Deployment > Deployment Center**.
2. Select **GitHub**, authorize the repository, and select the intended branch:
   normally a development branch for dev and `main` for production.
3. Select **GitHub Actions** as the provider and save.
4. Inspect the generated workflow in GitHub. Its build commands must execute in
   `PaytmCloneBackend`, run `./mvnw test` (or `mvnw.cmd test` only on Windows),
   package the application, and deploy the generated `target/*.jar`.
5. Watch **Deployment Center > Logs** until deployment succeeds.
6. Open `https://<app-name>.azurewebsites.net/actuator/health`. It should return
   a minimal response with status `UP`.

No controller is needed for the health URL; Spring Boot Actuator registers it.
All other Actuator endpoints remain disabled.

## 8. Create Azure Static Web Apps

Repeat once for dev and once for prod.

1. Search for **Static Web Apps** and choose **Create**.
2. Select the environment resource group, choose a plan, and connect the GitHub
   repository and branch.
3. Use **Custom** build settings:

   - App location: `PaytmCloneFrontend`
   - API location: leave empty
   - Output location: `dist`

4. Create the resource. Azure commits a GitHub Actions workflow to the selected
   branch.
5. In the generated workflow, expose these variables to the frontend build:

```yaml
env:
  VITE_API_BASE_URL: https://<app-name>.azurewebsites.net/api/v1
  VITE_APP_NAME: PayWallet
```

Vite values are compiled into the browser bundle, so setting them only as
runtime Static Web Apps settings is not sufficient. The API URL is not a secret
and can instead be stored as a GitHub Actions repository/environment variable.
Use a different URL in the dev and production workflows.

6. After the workflow succeeds, copy the default Static Web App URL.
7. Return to the matching App Service environment variables and set
   `CORS_ALLOWED_ORIGINS` to that exact origin, for example
   `https://kind-tree-012345678.1.azurestaticapps.net`. Do not add a trailing
   slash or use `*`.
8. Restart the App Service and test sign-in and an authenticated API request
   from the Static Web App.

The repository's `public/staticwebapp.config.json` supplies SPA route fallback
and basic response security headers.

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
