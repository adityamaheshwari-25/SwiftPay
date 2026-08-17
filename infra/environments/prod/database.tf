resource "azurerm_mysql_flexible_server" "primary" {
  name                = local.names.mysql
  resource_group_name = data.azurerm_resource_group.production.name
  location            = var.mysql_location

  administrator_login               = var.database_admin_username
  administrator_password_wo         = var.database_admin_password
  administrator_password_wo_version = var.database_admin_password_version

  version = var.mysql_version
  zone    = var.mysql_primary_zone

  sku_name                     = var.mysql_sku_name
  backup_retention_days        = var.mysql_backup_retention_days
  geo_redundant_backup_enabled = var.mysql_geo_redundant_backup_enabled

  public_network_access = "Disabled"

  dynamic "high_availability" {
    for_each = var.mysql_zone_redundant_high_availability_enabled ? [1] : []

    content {
      mode                      = "ZoneRedundant"
      standby_availability_zone = var.mysql_standby_zone
    }
  }

  maintenance_window {
    day_of_week  = 0
    start_hour   = 2
    start_minute = 0
  }

  storage {
    auto_grow_enabled  = true
    io_scaling_enabled = true
    size_gb            = var.mysql_storage_size_gb
  }

  tags = local.common_tags

  lifecycle {
    ignore_changes = [
      zone,
      high_availability[0].standby_availability_zone,
    ]
  }
}

resource "azurerm_private_endpoint" "mysql" {
  name                = "pep-${local.stem}-mysql"
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  subnet_id           = azurerm_subnet.private_endpoints.id
  tags                = local.common_tags

  private_service_connection {
    name                           = "psc-${local.stem}-mysql"
    private_connection_resource_id = azurerm_mysql_flexible_server.primary.id
    subresource_names              = ["mysqlServer"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "default"
    private_dns_zone_ids = [azurerm_private_dns_zone.mysql.id]
  }

  depends_on = [azurerm_private_dns_zone_virtual_network_link.mysql]
}

resource "azurerm_mysql_flexible_database" "application" {
  name                = var.database_name
  resource_group_name = data.azurerm_resource_group.production.name
  server_name         = azurerm_mysql_flexible_server.primary.name
  charset             = "utf8mb4"
  collation           = "utf8mb4_unicode_ci"
}

resource "azurerm_mysql_flexible_server_configuration" "secure_and_observable" {
  for_each = {
    long_query_time          = "2"
    require_secure_transport = "ON"
    slow_query_log           = "ON"
  }

  name                = each.key
  resource_group_name = data.azurerm_resource_group.production.name
  server_name         = azurerm_mysql_flexible_server.primary.name
  value               = each.value
}
