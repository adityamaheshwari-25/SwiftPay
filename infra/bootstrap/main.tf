data "azurerm_client_config" "current" {}

locals {
  stem               = "${var.name_prefix}-${var.environment}-${var.unique_suffix}"
  state_account_name = substr("sttf${var.unique_suffix}${replace(var.name_prefix, "-", "")}${replace(var.environment, "-", "")}", 0, 24)

  common_tags = merge(
    {
      application = var.name_prefix
      environment = var.environment
      managed-by  = "terraform"
      component   = "bootstrap"
    },
    var.tags,
  )

  github_repository_subject = "${var.github_organization}@${var.github_owner_id}/${var.github_repository}@${var.github_repository_id}"
  terraform_github_subject  = "repo:${local.github_repository_subject}:environment:${var.terraform_github_environment}"
  deploy_github_subject     = "repo:${local.github_repository_subject}:environment:${var.deploy_github_environment}"
}

resource "azurerm_resource_group" "state" {
  name     = "rg-${var.name_prefix}-tfstate-${var.unique_suffix}"
  location = var.location
  tags     = local.common_tags
}

resource "azurerm_resource_group" "production" {
  name     = "rg-${local.stem}"
  location = var.location
  tags = merge(local.common_tags, {
    component = "production"
  })
}

resource "azurerm_storage_account" "state" {
  name                = local.state_account_name
  resource_group_name = azurerm_resource_group.state.name
  location            = azurerm_resource_group.state.location

  account_tier             = "Standard"
  account_replication_type = "ZRS"
  account_kind             = "StorageV2"

  allow_nested_items_to_be_public   = false
  cross_tenant_replication_enabled  = false
  default_to_oauth_authentication   = true
  https_traffic_only_enabled        = true
  infrastructure_encryption_enabled = true
  local_user_enabled                = false
  min_tls_version                   = "TLS1_2"
  public_network_access_enabled     = true
  shared_access_key_enabled         = false

  blob_properties {
    change_feed_enabled = true
    versioning_enabled  = true

    container_delete_retention_policy {
      days = var.state_blob_retention_days
    }

    delete_retention_policy {
      days                     = var.state_blob_retention_days
      permanent_delete_enabled = false
    }
  }

  tags = local.common_tags
}

# The container is created through the ARM control plane. This keeps bootstrap
# independent of account keys and of direct access to the Storage data plane.
resource "azapi_resource" "state_container" {
  type      = "Microsoft.Storage/storageAccounts/blobServices/containers@2025-01-01"
  name      = "tfstate"
  parent_id = "${azurerm_storage_account.state.id}/blobServices/default"

  body = {
    properties = {
      publicAccess = "None"
    }
  }
}

resource "azurerm_management_lock" "state_storage" {
  name       = "protect-terraform-state"
  scope      = azurerm_storage_account.state.id
  lock_level = "CanNotDelete"
  notes      = "Protects Terraform state from accidental deletion. Terraform removes this lock first during an intentional bootstrap destroy."
}

resource "azurerm_user_assigned_identity" "terraform_ci" {
  name                = "id-${local.stem}-terraform-ci"
  resource_group_name = azurerm_resource_group.state.name
  location            = azurerm_resource_group.state.location
  tags                = local.common_tags
}

resource "azurerm_user_assigned_identity" "backend_deploy_ci" {
  name                = "id-${local.stem}-backend-deploy-ci"
  resource_group_name = azurerm_resource_group.state.name
  location            = azurerm_resource_group.state.location
  tags                = local.common_tags
}

resource "azurerm_federated_identity_credential" "terraform_ci" {
  name                = "github-${var.terraform_github_environment}-terraform"
  resource_group_name = azurerm_resource_group.state.name
  parent_id           = azurerm_user_assigned_identity.terraform_ci.id
  audience            = ["api://AzureADTokenExchange"]
  issuer              = "https://token.actions.githubusercontent.com"
  subject             = local.terraform_github_subject
}

resource "azurerm_federated_identity_credential" "backend_deploy_ci" {
  name                = "github-${var.deploy_github_environment}-backend-deploy"
  resource_group_name = azurerm_resource_group.state.name
  parent_id           = azurerm_user_assigned_identity.backend_deploy_ci.id
  audience            = ["api://AzureADTokenExchange"]
  issuer              = "https://token.actions.githubusercontent.com"
  subject             = local.deploy_github_subject
}

resource "azurerm_role_assignment" "terraform_state" {
  scope                = azurerm_storage_account.state.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_user_assigned_identity.terraform_ci.principal_id
  principal_type       = "ServicePrincipal"
}

resource "azurerm_role_assignment" "terraform_production_contributor" {
  scope                = azurerm_resource_group.production.id
  role_definition_name = "Contributor"
  principal_id         = azurerm_user_assigned_identity.terraform_ci.principal_id
  principal_type       = "ServicePrincipal"
}

# Contributor cannot create role assignments. RBAC Administrator is scoped only
# to the production resource group so the CI identity has no subscription-wide
# authorization rights.
resource "azurerm_role_assignment" "terraform_production_rbac" {
  scope                = azurerm_resource_group.production.id
  role_definition_name = "Role Based Access Control Administrator"
  principal_id         = azurerm_user_assigned_identity.terraform_ci.principal_id
  principal_type       = "ServicePrincipal"

  # Permit CI to create/delete only the three runtime/deployment assignments
  # declared by the production stack, and only for service principals/managed
  # identities. It cannot grant Owner, Contributor, or another RBAC-admin role.
  condition_version = "2.0"
  condition         = <<-CONDITION
    (
      (
        !(ActionMatches{'Microsoft.Authorization/roleAssignments/write'})
      )
      OR
      (
        @Request[Microsoft.Authorization/roleAssignments:RoleDefinitionId] ForAnyOfAnyValues:GuidEquals {
          4633458b-17de-408a-b874-0445c86b69e6,
          ba92f5b4-2d11-453d-a403-e96b0029c9fe,
          de139f84-1756-47ae-9be6-808fbbe84772
        }
        AND
        @Request[Microsoft.Authorization/roleAssignments:PrincipalType] ForAnyOfAnyValues:StringEqualsIgnoreCase {'ServicePrincipal'}
      )
    )
    AND
    (
      (
        !(ActionMatches{'Microsoft.Authorization/roleAssignments/delete'})
      )
      OR
      (
        @Resource[Microsoft.Authorization/roleAssignments:RoleDefinitionId] ForAnyOfAnyValues:GuidEquals {
          4633458b-17de-408a-b874-0445c86b69e6,
          ba92f5b4-2d11-453d-a403-e96b0029c9fe,
          de139f84-1756-47ae-9be6-808fbbe84772
        }
        AND
        @Resource[Microsoft.Authorization/roleAssignments:PrincipalType] ForAnyOfAnyValues:StringEqualsIgnoreCase {'ServicePrincipal'}
      )
    )
  CONDITION
}
