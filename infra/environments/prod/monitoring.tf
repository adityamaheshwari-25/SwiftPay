resource "azurerm_log_analytics_workspace" "production" {
  name                = local.names.log_analytics
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  sku                 = "PerGB2018"
  retention_in_days   = var.log_analytics_retention_days
  daily_quota_gb      = var.monitoring_daily_cap_gb
  tags                = local.common_tags
}

resource "azurerm_application_insights" "backend" {
  name                = local.names.app_insights
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  application_type    = "web"
  workspace_id        = azurerm_log_analytics_workspace.production.id

  daily_data_cap_in_gb                 = var.monitoring_daily_cap_gb
  daily_data_cap_notifications_enabled = true

  tags = local.common_tags
}

locals {
  diagnostic_targets = {
    backend-app  = azurerm_linux_web_app.backend.id
    backend-slot = azurerm_linux_web_app_slot.staging.id
    blob-service = "${azurerm_storage_account.kyc.id}/blobServices/default"
    key-vault    = azurerm_key_vault.application.id
    mysql        = azurerm_mysql_flexible_server.primary.id
    nat-gateway  = azurerm_nat_gateway.production.id
    nsg-app      = azurerm_network_security_group.app_integration.id
  }
}

# Query the supported categories from each deployed resource. This avoids
# hard-coding categories that vary by Azure service API version or region.
data "azurerm_monitor_diagnostic_categories" "production" {
  for_each = local.diagnostic_targets

  resource_id = each.value
}

resource "azurerm_monitor_diagnostic_setting" "production" {
  for_each = local.diagnostic_targets

  name                       = "diag-${each.key}-to-log-analytics"
  target_resource_id         = each.value
  log_analytics_workspace_id = azurerm_log_analytics_workspace.production.id

  dynamic "enabled_log" {
    for_each = toset(data.azurerm_monitor_diagnostic_categories.production[each.key].log_category_types)

    content {
      category = enabled_log.value
    }
  }

  dynamic "enabled_metric" {
    for_each = toset(data.azurerm_monitor_diagnostic_categories.production[each.key].metrics)

    content {
      category = enabled_metric.value
    }
  }
}

resource "azurerm_monitor_action_group" "operations" {
  name                = local.names.action_group
  resource_group_name = data.azurerm_resource_group.production.name
  short_name          = substr("ag${var.unique_suffix}", 0, 12)
  enabled             = true
  tags                = local.common_tags

  dynamic "email_receiver" {
    for_each = var.alert_email_receivers

    content {
      name                    = email_receiver.key
      email_address           = email_receiver.value
      use_common_alert_schema = true
    }
  }
}

resource "azurerm_monitor_metric_alert" "backend_http_5xx" {
  name                = "alert-${local.stem}-backend-http5xx"
  resource_group_name = data.azurerm_resource_group.production.name
  scopes              = [azurerm_linux_web_app.backend.id]
  description         = "Production backend is returning elevated HTTP 5xx responses."
  severity            = 1
  frequency           = "PT1M"
  window_size         = "PT5M"
  auto_mitigate       = true
  tags                = local.common_tags

  criteria {
    metric_namespace = "Microsoft.Web/sites"
    metric_name      = "Http5xx"
    aggregation      = "Total"
    operator         = "GreaterThan"
    threshold        = 5
  }

  action {
    action_group_id = azurerm_monitor_action_group.operations.id
  }
}

resource "azurerm_monitor_metric_alert" "backend_response_time" {
  name                = "alert-${local.stem}-backend-response-time"
  resource_group_name = data.azurerm_resource_group.production.name
  scopes              = [azurerm_linux_web_app.backend.id]
  description         = "Production backend average response time is above two seconds."
  severity            = 2
  frequency           = "PT1M"
  window_size         = "PT5M"
  auto_mitigate       = true
  tags                = local.common_tags

  criteria {
    metric_namespace = "Microsoft.Web/sites"
    metric_name      = "AverageResponseTime"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 2
  }

  action {
    action_group_id = azurerm_monitor_action_group.operations.id
  }
}

resource "azurerm_monitor_metric_alert" "app_service_plan_cpu" {
  name                = "alert-${local.stem}-plan-cpu"
  resource_group_name = data.azurerm_resource_group.production.name
  scopes              = [azurerm_service_plan.backend.id]
  description         = "Production App Service Plan CPU is above 80 percent."
  severity            = 2
  frequency           = "PT1M"
  window_size         = "PT5M"
  auto_mitigate       = true
  tags                = local.common_tags

  criteria {
    metric_namespace = "Microsoft.Web/serverfarms"
    metric_name      = "CpuPercentage"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.operations.id
  }
}

resource "azurerm_monitor_metric_alert" "mysql_cpu" {
  name                = "alert-${local.stem}-mysql-cpu"
  resource_group_name = data.azurerm_resource_group.production.name
  scopes              = [azurerm_mysql_flexible_server.primary.id]
  description         = "Production MySQL CPU is above 80 percent."
  severity            = 1
  frequency           = "PT1M"
  window_size         = "PT5M"
  auto_mitigate       = true
  tags                = local.common_tags

  criteria {
    metric_namespace = "Microsoft.DBforMySQL/flexibleServers"
    metric_name      = "cpu_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.operations.id
  }
}

resource "azurerm_monitor_metric_alert" "mysql_storage" {
  name                = "alert-${local.stem}-mysql-storage"
  resource_group_name = data.azurerm_resource_group.production.name
  scopes              = [azurerm_mysql_flexible_server.primary.id]
  description         = "Production MySQL storage is above 80 percent."
  severity            = 1
  frequency           = "PT5M"
  window_size         = "PT15M"
  auto_mitigate       = true
  tags                = local.common_tags

  criteria {
    metric_namespace = "Microsoft.DBforMySQL/flexibleServers"
    metric_name      = "storage_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.operations.id
  }
}

# Autoscale is intentionally opt-in. The current application uses in-memory SSE
# and uncoordinated scheduled jobs, so multiple active instances are unsafe until
# those workloads are externalized or distributed-lock protected.
resource "azurerm_monitor_autoscale_setting" "backend" {
  count = var.enable_app_service_autoscale ? 1 : 0

  name                = "autoscale-${local.stem}-backend"
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  target_resource_id  = azurerm_service_plan.backend.id
  enabled             = true
  tags                = local.common_tags

  profile {
    name = "cpu-based"

    capacity {
      default = var.app_service_autoscale_default_instances
      minimum = var.app_service_autoscale_min_instances
      maximum = var.app_service_autoscale_max_instances
    }

    rule {
      metric_trigger {
        metric_name        = "CpuPercentage"
        metric_namespace   = "Microsoft.Web/serverfarms"
        metric_resource_id = azurerm_service_plan.backend.id
        time_grain         = "PT1M"
        statistic          = "Average"
        time_window        = "PT5M"
        time_aggregation   = "Average"
        operator           = "GreaterThan"
        threshold          = 70
      }

      scale_action {
        direction = "Increase"
        type      = "ChangeCount"
        value     = "1"
        cooldown  = "PT5M"
      }
    }

    rule {
      metric_trigger {
        metric_name        = "CpuPercentage"
        metric_namespace   = "Microsoft.Web/serverfarms"
        metric_resource_id = azurerm_service_plan.backend.id
        time_grain         = "PT1M"
        statistic          = "Average"
        time_window        = "PT10M"
        time_aggregation   = "Average"
        operator           = "LessThan"
        threshold          = 30
      }

      scale_action {
        direction = "Decrease"
        type      = "ChangeCount"
        value     = "1"
        cooldown  = "PT10M"
      }
    }
  }

  lifecycle {
    precondition {
      condition = (
        var.app_service_autoscale_max_instances > var.app_service_autoscale_min_instances &&
        var.app_service_autoscale_default_instances >= var.app_service_autoscale_min_instances &&
        var.app_service_autoscale_default_instances <= var.app_service_autoscale_max_instances
      )
      error_message = "When autoscale is enabled, max must be greater than min and default must be within the min/max range."
    }
  }
}
