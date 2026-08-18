locals {
  backend_common_app_settings = merge(
    local.key_vault_references,
    {
      APPLICATIONINSIGHTS_CONNECTION_STRING       = azurerm_application_insights.backend.connection_string
      ApplicationInsightsAgent_EXTENSION_VERSION  = "~3"
      XDT_MicrosoftApplicationInsights_Mode       = "recommended"
      AZURE_CLIENT_ID                             = azurerm_user_assigned_identity.backend_runtime.client_id
      AZURE_STORAGE_BLOB_CONTAINER                = var.kyc_container_name
      AZURE_STORAGE_BLOB_ENDPOINT                 = azurerm_storage_account.kyc.primary_blob_endpoint
      CORS_ALLOWED_ORIGINS                        = join(",", local.cors_origins)
      FLYWAY_ENABLED                              = "false"
      JPA_DDL_AUTO                                = var.jpa_ddl_auto
      JWT_EXPIRATION                              = var.jwt_expiration
      SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT = tostring(var.hikari_connection_timeout_ms)
      SPRING_DATASOURCE_HIKARI_IDLE_TIMEOUT       = tostring(var.hikari_idle_timeout_ms)
      SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE  = tostring(var.hikari_maximum_pool_size)
      SPRING_DATASOURCE_HIKARI_MAX_LIFETIME       = tostring(var.hikari_max_lifetime_ms)
      SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE       = tostring(var.hikari_minimum_idle)
      SPRING_DATASOURCE_HIKARI_VALIDATION_TIMEOUT = tostring(var.hikari_validation_timeout_ms)
      SPRING_PROFILES_ACTIVE                      = "prod"
      WEBSITES_CONTAINER_START_TIME_LIMIT         = "900"
      WEBSITE_WARMUP_PATH                         = "/actuator/health"
      WEBSITE_WARMUP_STATUSES                     = "200"
    },
  )
}

resource "azurerm_static_web_app" "frontend" {
  name                = local.names.frontend_static
  resource_group_name = data.azurerm_resource_group.production.name
  location            = var.static_web_app_location

  sku_tier                           = "Standard"
  sku_size                           = "Standard"
  public_network_access_enabled      = true
  preview_environments_enabled       = false
  configuration_file_changes_enabled = true

  tags = local.common_tags

  lifecycle {
    # The deployment-token GitHub Action sets repository metadata server-side.
    # It cannot supply repository_token back to Terraform, so ignore that drift.
    ignore_changes = [
      repository_branch,
      repository_url,
    ]
  }
}

resource "azurerm_service_plan" "backend" {
  name                = local.names.app_service_plan
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  os_type             = "Linux"
  sku_name            = var.service_plan_sku_name

  worker_count           = var.app_service_instance_count
  zone_balancing_enabled = var.app_service_zone_balancing_enabled

  tags = local.common_tags

  lifecycle {
    precondition {
      condition     = !var.app_service_zone_balancing_enabled || var.app_service_instance_count > 1
      error_message = "App Service zone balancing requires more than one worker and a region/SKU-compatible instance count."
    }
  }
}

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
    # OneDeploy's JAR deployment type installs the uploaded artifact as
    # /home/site/wwwroot/app.jar regardless of its local filename.
    app_command_line                  = "java -jar /home/site/wwwroot/app.jar --server.port=80"
    always_on                         = true
    ftps_state                        = "Disabled"
    health_check_path                 = "/actuator/health"
    health_check_eviction_time_in_min = 5
    http2_enabled                     = true
    minimum_tls_version               = "1.2"
    remote_debugging_enabled          = false
    scm_minimum_tls_version           = "1.2"
    use_32_bit_worker                 = false
    vnet_route_all_enabled            = true
    websockets_enabled                = false

    application_stack {
      java_server         = "JAVA"
      java_server_version = "17"
      java_version        = "17"
    }

    cors {
      allowed_origins     = local.cors_origins
      support_credentials = false
    }
  }

  logs {
    detailed_error_messages = false
    failed_request_tracing  = true

    application_logs {
      file_system_level = "Information"
    }

    http_logs {
      file_system {
        retention_in_days = 7
        retention_in_mb   = 100
      }
    }
  }

  tags = local.common_tags

  depends_on = [
    azapi_resource.application_secret,
    azurerm_role_assignment.backend_blob_data,
    azurerm_role_assignment.backend_key_vault_secrets,
    azurerm_mysql_flexible_database.application,
    azurerm_private_endpoint.mysql,
    azurerm_private_endpoint.blob,
    azurerm_private_endpoint.key_vault,
    azurerm_subnet_nat_gateway_association.app_integration,
    azurerm_subnet_network_security_group_association.app_integration,
  ]
}

resource "azurerm_linux_web_app_slot" "staging" {
  name           = "staging"
  app_service_id = azurerm_linux_web_app.backend.id

  # CI starts the slot immediately before deployment and stops it after swap.
  # This prevents the idle slot from consuming DB connections or running jobs.
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
    app_command_line                  = "java -jar /home/site/wwwroot/app.jar --server.port=80"
    always_on                         = true
    ftps_state                        = "Disabled"
    health_check_path                 = "/actuator/health"
    health_check_eviction_time_in_min = 5
    http2_enabled                     = true
    minimum_tls_version               = "1.2"
    remote_debugging_enabled          = false
    scm_minimum_tls_version           = "1.2"
    use_32_bit_worker                 = false
    vnet_route_all_enabled            = true
    websockets_enabled                = false

    application_stack {
      java_server         = "JAVA"
      java_server_version = "17"
      java_version        = "17"
    }

    cors {
      allowed_origins     = local.cors_origins
      support_credentials = false
    }
  }

  logs {
    detailed_error_messages = false
    failed_request_tracing  = true

    application_logs {
      file_system_level = "Information"
    }

    http_logs {
      file_system {
        retention_in_days = 7
        retention_in_mb   = 100
      }
    }
  }

  tags = local.common_tags

  depends_on = [
    azapi_resource.application_secret,
    azurerm_role_assignment.backend_blob_data,
    azurerm_role_assignment.backend_key_vault_secrets,
    azurerm_private_endpoint.blob,
    azurerm_private_endpoint.key_vault,
  ]
}
