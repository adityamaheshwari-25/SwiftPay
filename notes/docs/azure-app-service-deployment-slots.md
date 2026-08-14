# Azure App Service production and staging slots in SwiftPay

## Purpose of this note

This note explains:

- where the production App Service is declared;
- where the staging slot is declared;
- how the backend artifact is deployed to staging;
- how staging is health-checked;
- how Azure swaps staging into production;
- how rollback works;
- how to inspect deployment slots in the Azure Portal; and
- how bootstrap, production Terraform, and the deployment workflow relate.

## The three concepts are different

| Concept | Type | Purpose |
| --- | --- | --- |
| Bootstrap stack | Terraform/Azure foundation | Creates state storage, resource groups, identities, OIDC and initial permissions |
| Production slot | Azure App Service runtime | Runs the backend version that serves real users |
| Staging slot | Azure App Service runtime | Temporarily runs and validates the next backend version before promotion |

Bootstrap is not an App Service slot.

## Repository files

The main files are:

```text
infra/bootstrap/main.tf
infra/bootstrap/outputs.tf
infra/environments/prod/applications.tf
infra/environments/prod/access-control.tf
.github/workflows/prod-backend.yml
notes/docs/production-terraform-runbook.md
```

## Production slot code

The default production slot is represented by the main Linux Web App resource in:

```text
infra/environments/prod/applications.tf
```

```hcl
resource "azurerm_linux_web_app" "backend" {
  name                = local.names.backend_web_app
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  service_plan_id     = azurerm_service_plan.backend.id

  enabled                                        = true
  https_only                                     = true
  public_network_access_enabled                  = true
  client_affinity_enabled                        = false
  ftp_publish_basic_authentication_enabled       = false
  webdeploy_publish_basic_authentication_enabled = false
  virtual_network_subnet_id                      = azurerm_subnet.app_integration.id
  key_vault_reference_identity_id                = azurerm_user_assigned_identity.backend_runtime.id

  app_settings = merge(local.backend_common_app_settings, {
    SCHEDULING_ENABLED = "true"
  })

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.backend_runtime.id]
  }

  sticky_settings {
    app_setting_names = ["SCHEDULING_ENABLED"]
  }

  site_config {
    always_on                         = true
    health_check_path                 = "/actuator/health"
    health_check_eviction_time_in_min = 5

    application_stack {
      java_server         = "JAVA"
      java_server_version = "17"
      java_version        = "17"
    }
  }
}
```

Important points:

- The main `azurerm_linux_web_app.backend` is the default production slot.
- It is enabled continuously.
- It exposes `/actuator/health` as its health-check path.
- It uses Java 17.
- It uses the production runtime identity.
- Scheduled jobs are enabled in production.
- `SCHEDULING_ENABLED` is configured as a sticky slot setting.

Its normal hostname has a form similar to:

```text
https://<app-name>.azurewebsites.net
```

## Staging slot code

The additional staging slot is declared in the same file:

```hcl
resource "azurerm_linux_web_app_slot" "staging" {
  name           = "staging"
  app_service_id = azurerm_linux_web_app.backend.id

  enabled                                        = false
  https_only                                     = true
  public_network_access_enabled                  = true
  client_affinity_enabled                        = false
  ftp_publish_basic_authentication_enabled       = false
  webdeploy_publish_basic_authentication_enabled = false
  virtual_network_subnet_id                      = azurerm_subnet.app_integration.id
  key_vault_reference_identity_id                = azurerm_user_assigned_identity.backend_runtime.id

  app_settings = merge(local.backend_common_app_settings, {
    SCHEDULING_ENABLED = "false"
  })

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.backend_runtime.id]
  }

  site_config {
    always_on                         = true
    health_check_path                 = "/actuator/health"
    health_check_eviction_time_in_min = 5

    application_stack {
      java_server         = "JAVA"
      java_server_version = "17"
      java_version        = "17"
    }
  }
}
```

Important points:

- It is an additional slot under the production App Service.
- It is normally stopped because Terraform declares `enabled = false`.
- The deployment workflow starts it immediately before deployment.
- It uses the same runtime identity, VNet integration, Key Vault references and common application configuration.
- Scheduled jobs are disabled in staging.
- The workflow stops staging after every deployment attempt.

Its hostname normally has a form similar to:

```text
https://<app-name>-staging.azurewebsites.net
```

## Sticky scheduled-job setting

Production declares:

```hcl
app_settings = merge(local.backend_common_app_settings, {
  SCHEDULING_ENABLED = "true"
})

sticky_settings {
  app_setting_names = ["SCHEDULING_ENABLED"]
}
```

Staging declares:

```hcl
app_settings = merge(local.backend_common_app_settings, {
  SCHEDULING_ENABLED = "false"
})
```

The intended result is:

```text
Production slot: SCHEDULING_ENABLED=true
Staging slot:    SCHEDULING_ENABLED=false
```

The setting remains associated with its intended slot during swaps. This prevents the temporary staging instance from executing production scheduled work.

## Deployment identity authorization

Bootstrap creates a dedicated backend deployment identity. Production Terraform grants it Website Contributor at the web-app scope:

```hcl
resource "azurerm_role_assignment" "backend_deploy" {
  scope                            = azurerm_linux_web_app.backend.id
  role_definition_name             = "Website Contributor"
  principal_id                     = var.backend_deploy_principal_object_id
  principal_type                   = "ServicePrincipal"
  skip_service_principal_aad_check = true
}
```

This identity is used for application deployment and slot operations. It is deliberately separate from the more privileged Terraform identity and from the application's runtime identity.

## Deployment workflow entry point

The protected backend deployment job is in:

```text
.github/workflows/prod-backend.yml
```

```yaml
deploy:
  name: Deploy through staging slot
  needs: build
  if: >-
    ${{
      (github.event_name == 'release' && github.event.release.prerelease == false) ||
      (github.event_name == 'workflow_dispatch' && github.ref == 'refs/heads/main')
    }}
  runs-on: ubuntu-latest
  timeout-minutes: 30
  environment:
    name: production
    url: https://${{ steps.webapp.outputs.production_hostname }}
  permissions:
    contents: read
    id-token: write
```

The deployment job runs only for:

- a published stable GitHub Release; or
- a manual workflow run from `main`.

It enters the protected GitHub Environment named `production` and receives OIDC permission.

## Resolve both slot hostnames

The workflow retrieves the production and staging hostnames:

```bash
production_hostname="$(
  az webapp show \
    --resource-group "$RESOURCE_GROUP" \
    --name "$WEBAPP_NAME" \
    --query defaultHostName \
    --output tsv
)"

staging_hostname="$(
  az webapp show \
    --resource-group "$RESOURCE_GROUP" \
    --name "$WEBAPP_NAME" \
    --slot staging \
    --query defaultHostName \
    --output tsv
)"
```

The production lookup omits `--slot`; therefore, it resolves the default production slot.

The staging lookup includes:

```text
--slot staging
```

## Start staging

The staging slot is normally stopped. The workflow starts it with:

```bash
az webapp start \
  --resource-group "$RESOURCE_GROUP" \
  --name "$WEBAPP_NAME" \
  --slot staging
```

## Deploy the JAR to staging

The workflow deliberately targets `staging`:

```yaml
- name: Deploy JAR to staging slot
  uses: azure/webapps-deploy@<pinned-commit>
  with:
    app-name: ${{ vars.AZURE_WEBAPP_NAME }}
    slot-name: staging
    package: deployment/swiftpay-backend.jar
```

The currently live production slot is not overwritten during this step.

## Staging health check

The workflow checks:

```text
https://<staging-hostname>/actuator/health
```

Relevant code:

```bash
for attempt in {1..20}; do
  status_code="$(
    curl --silent --show-error \
      --connect-timeout 5 \
      --max-time 10 \
      --output health.json \
      --write-out '%{http_code}' \
      "$HEALTH_URL" || true
  )"

  if [[ "$status_code" == "200" ]] && \
     jq --exit-status '.status == "UP"' health.json >/dev/null 2>&1; then
    echo "Staging health check passed: $HEALTH_URL"
    exit 0
  fi

  sleep 5
done

exit 1
```

If staging does not return HTTP 200 and JSON status `UP`, the job fails before the swap. The old production application continues serving users.

## Swap staging into production

The exact swap command is:

```bash
az webapp deployment slot swap \
  --resource-group "$RESOURCE_GROUP" \
  --name "$WEBAPP_NAME" \
  --slot staging \
  --target-slot production
```

Interpretation:

```text
Source slot: staging
Target slot: production
```

Before the swap:

```text
Production hostname -> old stable application
Staging hostname    -> new candidate application
```

After the swap:

```text
Production hostname -> new candidate application
Staging hostname    -> previous production application
```

The hostnames remain associated with their slots. Azure exchanges the applicable application content and configuration; it does not rename the hostnames.

Azure warms and validates source instances as part of its swap process before changing production traffic routing.

## Post-swap production health check

After swapping, the workflow checks:

```text
https://<production-hostname>/actuator/health
```

It again requires:

```text
HTTP 200
JSON status == UP
```

This catches failures that occur only after production promotion.

## Rollback code

Rollback runs only when:

- the swap step succeeded; and
- the production health check failed.

```yaml
if: ${{ failure() && steps.swap.outcome == 'success' && steps.production_health.outcome == 'failure' }}
```

The rollback uses the same swap command:

```bash
az webapp deployment slot swap \
  --resource-group "$RESOURCE_GROUP" \
  --name "$WEBAPP_NAME" \
  --slot staging \
  --target-slot production
```

Because the previous production application moved into staging during the first swap, a second swap restores it to production.

The workflow then checks production health again.

## Cleanup code

The workflow attempts to stop staging even after failures:

```yaml
- name: Stop staging after the deployment attempt
  if: ${{ always() && steps.webapp.outcome == 'success' }}
  continue-on-error: true
  run: |
    az webapp stop \
      --resource-group "$RESOURCE_GROUP" \
      --name "$WEBAPP_NAME" \
      --slot staging
```

Stopping staging prevents the idle slot from running application activity, scheduled jobs, or unnecessary database connections. It does not necessarily reduce the price of the already allocated App Service Plan.

## Complete deployment flow

```text
1. Production runs version v1; staging is stopped.
2. Workflow starts staging.
3. Workflow deploys version v2 to staging.
4. Workflow checks staging /actuator/health.
5. If unhealthy, stop without changing production.
6. If healthy, swap staging into production.
7. Production serves v2; staging holds v1.
8. Workflow checks production /actuator/health.
9. If unhealthy, swap again to restore v1.
10. Stop staging during cleanup.
```

## Why use deployment slots?

Deployment slots provide:

- validation in Azure before production promotion;
- Java/Spring Boot startup and warm-up before real traffic;
- reduced deployment downtime;
- a stable production hostname;
- a fast swap-based rollback path;
- separation between artifact deployment and traffic promotion; and
- protection against replacing a healthy production version with an obviously unhealthy candidate.

This is a common blue-green style production deployment pattern.

## Important limitations

The staging slot is not a completely separate testing environment.

In this repository:

- staging and production share the App Service Plan;
- staging uses production-connected resources;
- staging uses the production database;
- scheduled jobs are disabled in staging;
- schema mutation is not allowed from staging;
- database changes must be compatible with the old and new application during deployment; and
- an actuator health check cannot prove every business operation works.

Slots reduce deployment risk but do not replace automated tests, database migration controls, monitoring, or incident procedures.

## Azure Portal: inspect the feature before implementing this repository

### If no App Service exists yet

The repository-specific production and staging slots will not appear in Azure until the production Terraform stack creates the App Service resources.

To inspect the feature without deploying this repository, use one of these approaches:

1. Read the official Azure deployment-slot documentation and screenshots:

   https://learn.microsoft.com/en-us/azure/app-service/deploy-staging-slots

2. Inspect an existing nonproduction App Service that uses a Standard, Premium, or Isolated App Service Plan.

3. Create a temporary nonproduction App Service only if its cost, permissions and cleanup have been explicitly approved. Creating an App Service Plan can incur charges.

Deployment slots require a Standard, Premium, or Isolated App Service Plan tier.

### Portal navigation for an existing App Service

1. Open https://portal.azure.com.
2. Search for and select **App Services**.
3. Select the target web app.
4. In the left menu, open **Deployment**.
5. Select **Deployment slots**.
6. The page shows the default production slot and additional slots.
7. Select **Add Slot** to inspect the slot-creation dialog.
8. Enter a name such as `staging` and optionally choose a configuration source.
9. Cancel the dialog if the goal is only to inspect the UI.

Do not complete resource creation or a manual swap merely for exploration without approval.

### What appears after Terraform is applied

After production Terraform creates these resources, the Deployment slots page should show approximately:

```text
Slot          State       Traffic
production    Running     production traffic
staging       Stopped     0% direct production traffic
```

The exact portal columns can change over time.

Selecting the staging slot opens a separate App Service Slot page with:

- its own hostname;
- configuration;
- logs;
- identity information;
- networking settings;
- deployment information; and
- start/stop controls.

### Inspecting the Swap UI

From the App Service's **Deployment slots** page, Azure provides a **Swap** action.

The swap screen allows selection of:

```text
Source: staging
Target: production
```

It also displays configuration changes that participate in the swap.

For this repository, routine swaps should be performed by `prod-backend.yml`, not manually in the portal, because the workflow also performs:

- artifact traceability;
- pre-swap health validation;
- post-swap health validation;
- automatic rollback; and
- cleanup.

A portal swap bypasses parts of that controlled workflow.

## How bootstrap relates to the slots

```text
Bootstrap Terraform
  -> creates production resource group
  -> creates Terraform CI identity
  -> creates backend deployment identity
  -> creates GitHub OIDC credentials
  -> creates Terraform state storage

Production Terraform
  -> uses the production resource group
  -> creates App Service Plan
  -> creates production web app
  -> creates staging slot
  -> grants deployment identity Website Contributor

Backend workflow
  -> enters protected GitHub production environment
  -> receives deployment identity values
  -> authenticates to Azure with OIDC
  -> deploys to staging
  -> validates staging
  -> swaps staging to production
  -> validates production
  -> rolls back if needed
```

Bootstrap does not create or swap the staging slot. It creates the identity and foundational resources that make the later Terraform and deployment workflows possible.
