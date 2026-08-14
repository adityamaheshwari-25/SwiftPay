resource "azurerm_key_vault" "application" {
  name                = local.names.key_vault
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  tenant_id           = data.azurerm_client_config.current.tenant_id
  sku_name            = "standard"

  rbac_authorization_enabled    = true
  purge_protection_enabled      = true
  public_network_access_enabled = false
  soft_delete_retention_days    = 90

  network_acls {
    bypass         = "AzureServices"
    default_action = "Deny"
  }

  tags = local.common_tags
}

resource "azurerm_private_endpoint" "key_vault" {
  name                = "pep-${local.stem}-key-vault"
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  subnet_id           = azurerm_subnet.private_endpoints.id
  tags                = local.common_tags

  private_service_connection {
    name                           = "psc-${local.stem}-key-vault"
    private_connection_resource_id = azurerm_key_vault.application.id
    subresource_names              = ["vault"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "default"
    private_dns_zone_ids = [azurerm_private_dns_zone.key_vault.id]
  }
}

resource "azurerm_storage_account" "kyc" {
  name                = local.names.storage_account
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location

  account_tier             = "Standard"
  account_replication_type = "ZRS"
  account_kind             = "StorageV2"
  access_tier              = "Hot"

  allow_nested_items_to_be_public   = false
  cross_tenant_replication_enabled  = false
  default_to_oauth_authentication   = true
  https_traffic_only_enabled        = true
  infrastructure_encryption_enabled = true
  local_user_enabled                = false
  min_tls_version                   = "TLS1_2"
  public_network_access_enabled     = false
  shared_access_key_enabled         = false

  blob_properties {
    change_feed_enabled      = true
    last_access_time_enabled = true
    versioning_enabled       = true

    container_delete_retention_policy {
      days = 30
    }

    delete_retention_policy {
      days                     = 30
      permanent_delete_enabled = false
    }
  }

  tags = local.common_tags
}

# ARM-plane container creation works even though public data-plane access and
# storage account keys are disabled.
resource "azapi_resource" "kyc_container" {
  type      = "Microsoft.Storage/storageAccounts/blobServices/containers@2025-01-01"
  name      = var.kyc_container_name
  parent_id = "${azurerm_storage_account.kyc.id}/blobServices/default"

  body = {
    properties = {
      defaultEncryptionScope      = "$account-encryption-key"
      denyEncryptionScopeOverride = true
      publicAccess                = "None"
    }
  }
}

resource "azurerm_private_endpoint" "blob" {
  name                = "pep-${local.stem}-blob"
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  subnet_id           = azurerm_subnet.private_endpoints.id
  tags                = local.common_tags

  private_service_connection {
    name                           = "psc-${local.stem}-blob"
    private_connection_resource_id = azurerm_storage_account.kyc.id
    subresource_names              = ["blob"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "default"
    private_dns_zone_ids = [azurerm_private_dns_zone.blob.id]
  }
}

# Key Vault firewall rules govern the data plane only. Deploying child secret
# resources through ARM/AzAPI uses management.azure.com, so CI does not need a
# private runner or temporary public vault access. sensitive_body is write-only
# and therefore keeps secret values out of Terraform state.
resource "azapi_resource" "application_secret" {
  for_each = local.app_secret_names

  type      = "Microsoft.KeyVault/vaults/secrets@2024-11-01"
  name      = each.value
  parent_id = azurerm_key_vault.application.id

  body = {
    properties = {
      attributes = {
        enabled = true
      }
      contentType = "text/plain"
    }
  }

  sensitive_body = {
    properties = {
      value = local.app_secret_values[each.value]
    }
  }

  sensitive_body_version = {
    "properties.value" = local.app_secret_versions[each.value]
  }
}
