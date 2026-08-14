locals {
  protected_resource_ids = {
    key-vault = azurerm_key_vault.application.id
    mysql     = azurerm_mysql_flexible_server.primary.id
    storage   = azurerm_storage_account.kyc.id
  }
}

resource "azurerm_user_assigned_identity" "backend_runtime" {
  name                = "id-${local.stem}-backend-runtime"
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  tags                = local.common_tags
}

resource "azurerm_role_assignment" "backend_key_vault_secrets" {
  scope                            = azurerm_key_vault.application.id
  role_definition_name             = "Key Vault Secrets User"
  principal_id                     = azurerm_user_assigned_identity.backend_runtime.principal_id
  principal_type                   = "ServicePrincipal"
  skip_service_principal_aad_check = true
}

resource "azurerm_role_assignment" "backend_blob_data" {
  scope                            = azapi_resource.kyc_container.id
  role_definition_name             = "Storage Blob Data Contributor"
  principal_id                     = azurerm_user_assigned_identity.backend_runtime.principal_id
  principal_type                   = "ServicePrincipal"
  skip_service_principal_aad_check = true
}

resource "azurerm_role_assignment" "backend_deploy" {
  scope                            = azurerm_linux_web_app.backend.id
  role_definition_name             = "Website Contributor"
  principal_id                     = var.backend_deploy_principal_object_id
  principal_type                   = "ServicePrincipal"
  skip_service_principal_aad_check = true
}

resource "azurerm_management_lock" "production_data" {
  for_each = var.enable_delete_locks ? local.protected_resource_ids : {}

  name       = "protect-${each.key}"
  scope      = each.value
  lock_level = "CanNotDelete"
  notes      = "Production data protection managed by Terraform. Set enable_delete_locks=false before an intentional replacement or destroy."
}
