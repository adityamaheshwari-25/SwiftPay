data "azurerm_client_config" "current" {}

data "azurerm_resource_group" "production" {
  name = var.resource_group_name
}

locals {
  stem         = "${var.name_prefix}-${var.environment}-${var.unique_suffix}"
  compact_stem = replace(local.stem, "-", "")

  names = {
    action_group     = "ag-${local.stem}"
    app_insights     = "appi-${local.stem}"
    app_service_plan = "asp-${local.stem}"
    backend_web_app  = substr("app-${local.stem}", 0, 60)
    frontend_static  = substr("swa-${local.stem}", 0, 40)
    key_vault        = substr("kv${var.unique_suffix}${local.compact_stem}", 0, 24)
    log_analytics    = "log-${local.stem}"
    mysql            = substr("mysql-${local.stem}", 0, 63)
    nat_gateway      = "nat-${local.stem}"
    nat_public_ip    = "pip-${local.stem}-nat"
    network_security = "nsg-${local.stem}-app"
    storage_account  = substr("st${var.unique_suffix}${local.compact_stem}", 0, 24)
    virtual_network  = "vnet-${local.stem}"
  }

  common_tags = merge(
    {
      application = var.name_prefix
      environment = var.environment
      managed-by  = "terraform"
      criticality = "high"
    },
    var.tags,
  )

  frontend_origin = "https://${azurerm_static_web_app.frontend.default_host_name}"
  cors_origins    = distinct(concat([local.frontend_origin], var.additional_cors_allowed_origins))

  database_jdbc_url = "jdbc:mysql://${azurerm_mysql_flexible_server.primary.fqdn}:3306/${azurerm_mysql_flexible_database.application.name}?sslMode=VERIFY_IDENTITY&serverTimezone=UTC"

  app_secret_names = toset([
    "DB-PASSWORD",
    "DB-URL",
    "DB-USERNAME",
    "JWT-SECRET",
    "SUPERADMIN-EMAIL",
    "SUPERADMIN-PASSWORD",
  ])

  app_secret_values = {
    "DB-PASSWORD"         = var.database_application_password
    "DB-URL"              = local.database_jdbc_url
    "DB-USERNAME"         = var.database_application_username
    "JWT-SECRET"          = var.jwt_secret
    "SUPERADMIN-EMAIL"    = var.superadmin_email
    "SUPERADMIN-PASSWORD" = var.superadmin_password
  }

  app_secret_versions = {
    "DB-PASSWORD"         = tostring(var.database_application_password_version)
    "DB-URL"              = substr(sha256(local.database_jdbc_url), 0, 16)
    "DB-USERNAME"         = substr(sha256(var.database_application_username), 0, 16)
    "JWT-SECRET"          = tostring(var.application_secret_version)
    "SUPERADMIN-EMAIL"    = tostring(var.application_secret_version)
    "SUPERADMIN-PASSWORD" = tostring(var.application_secret_version)
  }

  key_vault_references = {
    DB_URL              = "@Microsoft.KeyVault(VaultName=${local.names.key_vault};SecretName=DB-URL)"
    DB_USERNAME         = "@Microsoft.KeyVault(VaultName=${local.names.key_vault};SecretName=DB-USERNAME)"
    DB_PASSWORD         = "@Microsoft.KeyVault(VaultName=${local.names.key_vault};SecretName=DB-PASSWORD)"
    JWT_SECRET          = "@Microsoft.KeyVault(VaultName=${local.names.key_vault};SecretName=JWT-SECRET)"
    SUPERADMIN_EMAIL    = "@Microsoft.KeyVault(VaultName=${local.names.key_vault};SecretName=SUPERADMIN-EMAIL)"
    SUPERADMIN_PASSWORD = "@Microsoft.KeyVault(VaultName=${local.names.key_vault};SecretName=SUPERADMIN-PASSWORD)"
  }
}
