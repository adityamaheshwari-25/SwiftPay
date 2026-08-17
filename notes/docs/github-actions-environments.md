# GitHub Actions Environments in SwiftPay

## Purpose of this note

This note explains:

- what a GitHub Actions Environment is;
- what it is not;
- why SwiftPay uses two protected GitHub Environments;
- every workflow location that references them;
- how environment secrets and variables reach jobs;
- how environment names participate in Azure OIDC authentication;
- how approvals and branch/tag restrictions work; and
- how to inspect and configure the environments in the GitHub UI.

## Short definition

A GitHub Actions Environment is a GitHub deployment-control boundary representing a target such as production, staging, development, or production infrastructure.

An environment can provide:

- required reviewers;
- prevention of self-review;
- wait timers;
- allowed deployment branches and tags;
- custom deployment-protection rules;
- environment-scoped secrets;
- environment-scoped variables;
- deployment records; and
- a deployment URL.

A job that references a protected environment waits until its protection rules pass. Environment secrets are not made available to the job until approval/protection requirements are satisfied.

## What a GitHub Environment is not

A GitHub Environment is not automatically any of these:

- a Git branch;
- an Azure subscription;
- an Azure resource group;
- an Azure App Service;
- an Azure App Service deployment slot;
- a Terraform workspace;
- a server; or
- a Kubernetes namespace.

It is a GitHub-side security, configuration, approval and deployment-tracking object.

## SwiftPay's two GitHub Environments

The repository runbook requires these two environments:

```text
production-infrastructure
production
```

They must be created under:

```text
GitHub repository
  -> Settings
  -> Environments
```

They are separate to enforce least privilege.

| GitHub Environment | Used for | Main identity/access |
| --- | --- | --- |
| `production-infrastructure` | Terraform plan/apply and state access | Terraform CI identity with infrastructure permissions |
| `production` | Backend and frontend application deployment | Narrow backend deployment identity and frontend deployment token |

## Why use two instead of one?

The Terraform identity needs privileges to manage production infrastructure and read/write Terraform state.

The application deployment identity needs only enough access to deploy the backend and perform App Service slot operations.

Separating them means:

```text
Application deployment credentials
  !=
Terraform infrastructure credentials
```

If the application deployment identity is compromised, it should not automatically grant the broader Terraform permissions.

## `production-infrastructure` in prod-infrastructure.yml

### Production plan job

```yaml
production_plan:
  name: Plan production infrastructure
  if: github.event_name == 'workflow_dispatch' && github.ref == 'refs/heads/main'
  needs: validate
  runs-on: ubuntu-latest
  timeout-minutes: 90
  environment: production-infrastructure
  permissions:
    contents: read
    id-token: write
```

The important line is:

```yaml
environment: production-infrastructure
```

This is shorthand for associating that job with the GitHub Environment named `production-infrastructure`.

Consequences can include:

- waiting for required reviewer approval;
- enforcing permitted branch/tag rules;
- receiving environment-scoped secrets and variables; and
- recording an infrastructure deployment event.

The job also requests:

```yaml
id-token: write
```

This allows GitHub Actions to request an OIDC token for Azure login. It does not itself grant Azure permission; Azure validates the token against the federated credential created by bootstrap.

### Production apply job

```yaml
production_apply:
  name: Approve and apply the exact production plan
  if: github.event_name == 'workflow_dispatch' && github.ref == 'refs/heads/main' && inputs.operation == 'apply'
  needs: production_plan
  runs-on: ubuntu-latest
  timeout-minutes: 90
  environment: production-infrastructure
  permissions:
    contents: read
    id-token: write
```

The apply job references the same protected environment:

```yaml
environment: production-infrastructure
```

If required reviewers are configured, the repository's two sequential environment-associated jobs can create two gates:

1. approval before the production plan job; and
2. approval before the production apply job.

The second approval should occur only after the plan output has been reviewed.

## `production` in prod-backend.yml

The backend deploy job uses the `production` GitHub Environment:

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

This uses the expanded environment form:

```yaml
environment:
  name: production
  url: https://${{ steps.webapp.outputs.production_hostname }}
```

Meaning:

- `name: production` associates the job with the protected GitHub Environment.
- `url` provides GitHub with the deployed production backend URL.
- GitHub can display that URL alongside the deployment record.

The backend build job does not reference `production`. It can compile and test without obtaining protected deployment credentials.

Only the deploy job enters the environment, receives its protected values, authenticates to Azure, and changes the web app.

## `production` in prod-frontend.yml

The frontend deploy job also uses the `production` GitHub Environment:

```yaml
deploy:
  name: Deploy frontend to production
  if: >-
    ${{
      (github.event_name == 'release' && github.event.release.prerelease == false) ||
      (github.event_name == 'workflow_dispatch' && github.ref == 'refs/heads/main')
    }}
  needs: build
  runs-on: ubuntu-latest
  environment: production
  timeout-minutes: 10
```

The frontend build job does not enter the protected environment. It performs installation, linting, validation and build before the protected deployment boundary.

The deploy job enters `production`, downloads the verified artifact, and receives the protected Static Web Apps deployment token.

## Why there is no GitHub `staging` Environment here

Azure's staging slot and GitHub's environments are different systems.

This repository treats the Azure staging slot as a temporary technical step inside one approved production deployment:

```text
GitHub Environment: production
  -> authorize deployment job
  -> deploy candidate to Azure staging slot
  -> health-check staging
  -> swap candidate into Azure production slot
  -> health-check production
  -> roll back if necessary
```

The staging slot is not treated as a separate long-lived business deployment target requiring a separate GitHub approval.

A separate GitHub `staging` Environment would make sense if the repository had an independently operated staging environment with its own:

- credentials;
- users/testers;
- data;
- deployment schedule;
- approval rules; and
- long-lived application endpoint.

That is not the current architecture.

## Environment-scoped values in workflow code

### Backend Azure login

The backend deploy job uses:

```yaml
- name: Sign in to Azure with OpenID Connect
  uses: azure/login@<pinned-commit>
  with:
    client-id: ${{ secrets.AZURE_CLIENT_ID }}
    tenant-id: ${{ secrets.AZURE_TENANT_ID }}
    subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
```

Because the job references `production`, the `secrets` names resolve to secrets configured for that environment, subject to GitHub's precedence rules if identically named secrets also exist at other scopes.

### Backend target variables

The backend deploy job uses environment variables such as:

```yaml
env:
  RESOURCE_GROUP: ${{ vars.AZURE_RESOURCE_GROUP }}
  WEBAPP_NAME: ${{ vars.AZURE_WEBAPP_NAME }}
```

These identify the Azure resource group and App Service target.

### Frontend deployment secret

The frontend deployment uses:

```yaml
${{ secrets.AZURE_STATIC_WEB_APPS_API_TOKEN }}
```

This token belongs in the protected `production` environment because it authorizes production frontend deployment.

### Infrastructure values

The infrastructure jobs use values such as:

```yaml
${{ secrets.AZURE_CLIENT_ID }}
${{ secrets.AZURE_TENANT_ID }}
${{ secrets.AZURE_SUBSCRIPTION_ID }}
${{ vars.TF_STATE_RESOURCE_GROUP }}
${{ vars.TF_STATE_STORAGE_ACCOUNT }}
${{ vars.TF_STATE_CONTAINER }}
${{ vars.TF_STATE_KEY }}
${{ vars.TF_VAR_resource_group_name }}
${{ vars.TF_VAR_backend_deploy_principal_object_id }}
${{ vars.TF_VAR_unique_suffix }}
```

Because the plan/apply jobs reference `production-infrastructure`, these names are intended to resolve to that environment's infrastructure-specific configuration.

## Secrets versus variables

### Environment secrets

Use secrets for sensitive values that must not be displayed normally, such as:

- identity/client configuration treated as protected deployment input;
- database passwords;
- JWT signing material;
- superadmin password;
- plan encryption passphrase; and
- Static Web Apps deployment token.

Workflow syntax:

```yaml
${{ secrets.SECRET_NAME }}
```

GitHub masks recognized secret values in logs, but workflows must still avoid printing, transforming, or exposing them.

### Environment variables

Use variables for non-secret configuration, such as:

- resource group name;
- web app name;
- state container name;
- state key; and
- resource naming suffix.

Workflow syntax:

```yaml
${{ vars.VARIABLE_NAME }}
```

Variables are not secret and should not contain passwords or tokens.

## Bootstrap OIDC subjects contain environment names

Bootstrap constructs two OIDC subjects:

```hcl
locals {
  github_repository_subject = "${var.github_organization}@${var.github_owner_id}/${var.github_repository}@${var.github_repository_id}"
  terraform_github_subject  = "repo:${local.github_repository_subject}:environment:${var.terraform_github_environment}"
  deploy_github_subject     = "repo:${local.github_repository_subject}:environment:${var.deploy_github_environment}"
}
```

GitHub repositories using immutable OIDC subjects include the numeric owner and
repository IDs. For this repository, the resulting trusted subjects are:

```text
repo:adityamaheshwari-25@281927465/SwiftPay@1309554667:environment:production-infrastructure
repo:adityamaheshwari-25@281927465/SwiftPay@1309554667:environment:production
```

With the repository defaults, these conceptually become:

```text
repo:<owner>@<owner-id>/<repository>@<repository-id>:environment:production-infrastructure
repo:<owner>@<owner-id>/<repository>@<repository-id>:environment:production
```

Bootstrap places them into Azure federated identity credentials:

```hcl
resource "azurerm_federated_identity_credential" "terraform_ci" {
  name                = "github-${var.terraform_github_environment}-terraform"
  parent_id           = azurerm_user_assigned_identity.terraform_ci.id
  audience            = ["api://AzureADTokenExchange"]
  issuer              = "https://token.actions.githubusercontent.com"
  subject             = local.terraform_github_subject
}

resource "azurerm_federated_identity_credential" "backend_deploy_ci" {
  name                = "github-${var.deploy_github_environment}-backend-deploy"
  parent_id           = azurerm_user_assigned_identity.backend_deploy_ci.id
  audience            = ["api://AzureADTokenExchange"]
  issuer              = "https://token.actions.githubusercontent.com"
  subject             = local.deploy_github_subject
}
```

This means Azure trusts OIDC tokens only when the token's repository and GitHub Environment subject match the configured values.

The environment name is therefore security-sensitive.

If `production` or `production-infrastructure` is renamed in GitHub without updating and applying bootstrap, Azure OIDC login will fail because the subject claim no longer matches.

## Bootstrap outputs for GitHub Environment configuration

Bootstrap produces a map aligned with `production-infrastructure`:

```hcl
output "production_infrastructure_github_environment_variables" {
  value = {
    AZURE_CLIENT_ID                           = azurerm_user_assigned_identity.terraform_ci.client_id
    AZURE_TENANT_ID                           = data.azurerm_client_config.current.tenant_id
    AZURE_SUBSCRIPTION_ID                     = var.subscription_id
    TF_STATE_RESOURCE_GROUP                   = azurerm_resource_group.state.name
    TF_STATE_STORAGE_ACCOUNT                  = azurerm_storage_account.state.name
    TF_STATE_CONTAINER                        = azapi_resource.state_container.name
    TF_STATE_KEY                              = "${var.name_prefix}/${var.environment}.tfstate"
    TF_VAR_resource_group_name                = azurerm_resource_group.production.name
    TF_VAR_backend_deploy_principal_object_id = azurerm_user_assigned_identity.backend_deploy_ci.principal_id
    TF_VAR_unique_suffix                      = var.unique_suffix
  }
}
```

Bootstrap also produces a map aligned with the application `production` environment:

```hcl
output "production_github_environment_variables" {
  value = {
    AZURE_CLIENT_ID       = azurerm_user_assigned_identity.backend_deploy_ci.client_id
    AZURE_TENANT_ID       = data.azurerm_client_config.current.tenant_id
    AZURE_SUBSCRIPTION_ID = var.subscription_id
    AZURE_RESOURCE_GROUP  = azurerm_resource_group.production.name
  }
}
```

These outputs do not automatically create or populate GitHub Environments. An authorized repository administrator must create the environments and place values into the correct secret/variable fields.

## Intended `production-infrastructure` configuration

### Secrets

```text
AZURE_CLIENT_ID
AZURE_TENANT_ID
AZURE_SUBSCRIPTION_ID
TF_VAR_database_admin_password
TF_VAR_database_application_password
TF_VAR_jwt_secret
TF_VAR_superadmin_password
TF_PLAN_ENCRYPTION_PASSPHRASE
```

### Variables

```text
TF_STATE_RESOURCE_GROUP
TF_STATE_STORAGE_ACCOUNT
TF_STATE_CONTAINER
TF_STATE_KEY
TF_VAR_resource_group_name
TF_VAR_backend_deploy_principal_object_id
TF_VAR_unique_suffix
TF_VAR_superadmin_email
TF_VAR_alert_email_receivers
TF_VAR_static_web_app_location
```

The workflow validates these values before Azure login and planning.

## Intended `production` configuration

### Secrets

```text
AZURE_CLIENT_ID
AZURE_TENANT_ID
AZURE_SUBSCRIPTION_ID
AZURE_STATIC_WEB_APPS_API_TOKEN
```

### Variables

```text
AZURE_RESOURCE_GROUP
AZURE_WEBAPP_NAME
```

The backend deployment identity uses OIDC. The frontend action uses the Static Web Apps deployment token.

## Repository-level frontend variables

The frontend build job intentionally runs before entering the protected environment. It requires these as repository or organization Actions variables:

```text
PRODUCTION_API_BASE_URL
PRODUCTION_FRONTEND_URL
```

They are not secrets because frontend build-time `VITE_*` values become visible in the browser bundle.

This separation allows lint/build verification before the protected deployment job requests production approval and secrets.

## GitHub UI: create or inspect the environments

1. Open the repository on GitHub.
2. Select **Settings**.
3. In the left navigation, select **Environments**.
4. Create or select:

   ```text
   production-infrastructure
   production
   ```

5. Review each environment's deployment-protection rules.
6. Review deployment branches and tags.
7. Review environment secrets.
8. Review environment variables.

## Recommended protection for `production-infrastructure`

Configure:

- required reviewers;
- prevent self-review where available;
- prevent administrator bypass for normal operations where available;
- allow deployment only from `main`;
- separate approvers from day-to-day authors where possible; and
- retain the exact name `production-infrastructure` unless bootstrap is updated first.

Because both the plan and apply jobs use this environment, reviewers may see two approval requests in one apply workflow run.

## Recommended protection for `production`

Configure:

- required reviewers;
- prevent self-review where available;
- prevent administrator bypass for normal releases where available;
- allow deliberate manual deployments from `main`;
- allow stable release tags such as `v*` created from reviewed `main` history; and
- retain the exact name `production` unless bootstrap is updated first.

## What happens when a job reaches a protected environment

Conceptually:

```text
Workflow build/validation succeeds
  -> deploy or infrastructure job references an environment
  -> GitHub checks branch/tag restrictions
  -> GitHub checks required reviewers and other protection rules
  -> job waits if approval is required
  -> reviewer approves
  -> environment secrets/variables become available to the job
  -> job requests OIDC token if id-token: write is present
  -> Azure validates repository + environment subject
  -> authorized deployment/operation runs
  -> GitHub records deployment status and optional URL
```

## Relationship to Azure deployment slots

GitHub Environment and Azure slots solve different problems:

```text
GitHub Environment
  -> controls who may deploy
  -> controls which ref may deploy
  -> protects secrets and variables
  -> participates in OIDC trust
  -> records deployment status

Azure deployment slots
  -> run old and new application versions
  -> warm and health-check the candidate
  -> move the candidate into production through swap
  -> retain the old version for immediate rollback
```

In SwiftPay:

```text
GitHub production approval
  -> backend deploy job receives credentials
  -> job deploys to Azure staging slot
  -> job validates staging
  -> job swaps staging into Azure production slot
```

## Common misunderstandings

### Misunderstanding: `environment: production` creates Azure production

It does not. Terraform creates Azure resources. The GitHub `environment` line associates a workflow job with a GitHub deployment-control object.

### Misunderstanding: GitHub production environment equals Azure production slot

They are not the same. The GitHub Environment authorizes the job; the Azure slot runs the application.

### Misunderstanding: GitHub Environment values are automatically available everywhere

They are available only to jobs that reference that environment. Environment secrets can also remain unavailable until protection rules pass.

### Misunderstanding: `id-token: write` is an Azure permission

It only permits requesting a GitHub OIDC token. Azure grants actual access only when the federated credential subject and Azure RBAC assignments match.

### Misunderstanding: changing the environment name is cosmetic

It is not cosmetic here. The environment name is part of the Azure OIDC subject configured by bootstrap.

## Official GitHub reference

```text
https://docs.github.com/en/actions/reference/workflows-and-actions/deployments-and-environments
```
