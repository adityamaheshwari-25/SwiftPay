output "tenant_id" {
  description = "Microsoft Entra tenant ID for GitHub OIDC login."
  value       = data.azurerm_client_config.current.tenant_id
}

output "subscription_id" {
  description = "Azure subscription ID for GitHub OIDC login."
  value       = var.subscription_id
}

output "production_resource_group_name" {
  description = "Existing resource group name to pass into the production Terraform configuration."
  value       = azurerm_resource_group.production.name
}

output "terraform_ci_client_id" {
  description = "Client ID of the Terraform CI user-assigned managed identity."
  value       = azurerm_user_assigned_identity.terraform_ci.client_id
}

output "terraform_ci_principal_id" {
  description = "Object/principal ID of the Terraform CI identity."
  value       = azurerm_user_assigned_identity.terraform_ci.principal_id
}

output "backend_deploy_ci_client_id" {
  description = "Client ID of the backend deployment CI user-assigned managed identity."
  value       = azurerm_user_assigned_identity.backend_deploy_ci.client_id
}

output "backend_deploy_ci_principal_id" {
  description = "Object/principal ID to pass as backend_deploy_principal_object_id in production."
  value       = azurerm_user_assigned_identity.backend_deploy_ci.principal_id
}

output "state_resource_group_name" {
  description = "Terraform remote-state resource group."
  value       = azurerm_resource_group.state.name
}

output "state_storage_account_name" {
  description = "Terraform remote-state storage account."
  value       = azurerm_storage_account.state.name
}

output "state_container_name" {
  description = "Terraform remote-state blob container."
  value       = azapi_resource.state_container.name
}

output "bootstrap_backend_config_local" {
  description = "Non-secret backend values for migrating bootstrap state with a local Azure CLI-authenticated operator."
  value = {
    resource_group_name  = azurerm_resource_group.state.name
    storage_account_name = azurerm_storage_account.state.name
    container_name       = azapi_resource.state_container.name
    key                  = "${var.name_prefix}/bootstrap.tfstate"
    use_azuread_auth     = true
    tenant_id            = data.azurerm_client_config.current.tenant_id
    subscription_id      = var.subscription_id
  }
}

output "production_backend_config_local" {
  description = "Non-secret backend values for a local Azure CLI-authenticated operator."
  value = {
    resource_group_name  = azurerm_resource_group.state.name
    storage_account_name = azurerm_storage_account.state.name
    container_name       = azapi_resource.state_container.name
    key                  = "${var.name_prefix}/${var.environment}.tfstate"
    use_azuread_auth     = true
    tenant_id            = data.azurerm_client_config.current.tenant_id
    subscription_id      = var.subscription_id
  }
}

output "production_backend_config_oidc" {
  description = "Non-secret backend values for the GitHub Actions OIDC Terraform workflow."
  value = {
    resource_group_name  = azurerm_resource_group.state.name
    storage_account_name = azurerm_storage_account.state.name
    container_name       = azapi_resource.state_container.name
    key                  = "${var.name_prefix}/${var.environment}.tfstate"
    use_azuread_auth     = true
    use_oidc             = true
    tenant_id            = data.azurerm_client_config.current.tenant_id
    subscription_id      = var.subscription_id
    client_id            = azurerm_user_assigned_identity.terraform_ci.client_id
  }
}

output "production_infrastructure_github_environment_variables" {
  description = "Non-secret values whose keys exactly match the production-infrastructure workflow configuration."
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

output "production_github_environment_variables" {
  description = "Non-secret values whose keys exactly match the backend production deployment workflow."
  value = {
    AZURE_CLIENT_ID       = azurerm_user_assigned_identity.backend_deploy_ci.client_id
    AZURE_TENANT_ID       = data.azurerm_client_config.current.tenant_id
    AZURE_SUBSCRIPTION_ID = var.subscription_id
    AZURE_RESOURCE_GROUP  = azurerm_resource_group.production.name
  }
}
