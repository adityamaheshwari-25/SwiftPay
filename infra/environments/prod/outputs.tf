output "resource_group_name" {
  description = "Production resource group."
  value       = data.azurerm_resource_group.production.name
}

output "virtual_network_id" {
  description = "Production VNet resource ID."
  value       = azurerm_virtual_network.production.id
}

output "subnet_ids" {
  description = "Production subnet resource IDs."
  value = {
    app_integration   = azurerm_subnet.app_integration.id
    private_endpoints = azurerm_subnet.private_endpoints.id
  }
}

output "nat_gateway_public_ip" {
  description = "Stable backend outbound public IP for partner allowlists."
  value       = azurerm_public_ip.nat.ip_address
}

output "backend_web_app_name" {
  description = "Production backend App Service name."
  value       = azurerm_linux_web_app.backend.name
}

output "backend_web_app_url" {
  description = "Public HTTPS backend URL. Direct App Service ingress preserves long-lived SSE support."
  value       = "https://${azurerm_linux_web_app.backend.default_hostname}"
}

output "backend_health_url" {
  description = "Production backend health endpoint."
  value       = "https://${azurerm_linux_web_app.backend.default_hostname}/actuator/health"
}

output "backend_runtime_identity" {
  description = "Shared user-assigned runtime identity used by the production app and staging slot."
  value = {
    resource_id  = azurerm_user_assigned_identity.backend_runtime.id
    client_id    = azurerm_user_assigned_identity.backend_runtime.client_id
    principal_id = azurerm_user_assigned_identity.backend_runtime.principal_id
  }
}

output "staging_slot" {
  description = "Staging slot deployment details. The slot is stopped by default."
  value = {
    name                          = azurerm_linux_web_app_slot.staging.name
    hostname                      = azurerm_linux_web_app_slot.staging.default_hostname
    health_url                    = "https://${azurerm_linux_web_app_slot.staging.default_hostname}/actuator/health"
    runtime_identity_principal_id = azurerm_user_assigned_identity.backend_runtime.principal_id
  }
}

output "frontend_static_web_app_name" {
  description = "Production Azure Static Web App name."
  value       = azurerm_static_web_app.frontend.name
}

output "frontend_static_web_app_location" {
  description = "Static Web Apps control-plane region (separate from the workload resource-group region)."
  value       = azurerm_static_web_app.frontend.location
}

output "frontend_url" {
  description = "Production Azure Static Web App URL."
  value       = "https://${azurerm_static_web_app.frontend.default_host_name}"
}

output "frontend_deployment_token" {
  description = "Sensitive Azure Static Web Apps deployment token. Store as AZURE_STATIC_WEB_APPS_API_TOKEN in the protected production GitHub Environment."
  value       = azurerm_static_web_app.frontend.api_key
  sensitive   = true
}

output "mysql_fqdn" {
  description = "Private DNS FQDN of MySQL Flexible Server."
  value       = azurerm_mysql_flexible_server.primary.fqdn
}

output "mysql_jdbc_url" {
  description = "Non-credential JDBC URL using certificate hostname verification."
  value       = local.database_jdbc_url
}

output "mysql_application_username" {
  description = "Least-privilege runtime login that the VNet-connected migration process must create before backend deployment."
  value       = var.database_application_username
}

output "key_vault_name" {
  description = "Private production Key Vault name."
  value       = azurerm_key_vault.application.name
}

output "kyc_storage" {
  description = "Private KYC Blob Storage details."
  value = {
    account_name   = azurerm_storage_account.kyc.name
    blob_endpoint  = azurerm_storage_account.kyc.primary_blob_endpoint
    container_name = azapi_resource.kyc_container.name
  }
}

output "monitoring" {
  description = "Production observability resource IDs."
  value = {
    log_analytics_workspace_id = azurerm_log_analytics_workspace.production.id
    application_insights_id    = azurerm_application_insights.backend.id
    action_group_id            = azurerm_monitor_action_group.operations.id
  }
}

output "github_environment_variables" {
  description = "Non-secret backend deployment variables to configure in the protected production GitHub Environment."
  value = {
    AZURE_RESOURCE_GROUP      = data.azurerm_resource_group.production.name
    AZURE_WEBAPP_NAME         = azurerm_linux_web_app.backend.name
    AZURE_STATIC_WEB_APP_NAME = azurerm_static_web_app.frontend.name
    BACKEND_URL               = "https://${azurerm_linux_web_app.backend.default_hostname}"
  }
}

output "frontend_repository_variables" {
  description = "Non-secret variables to configure at GitHub repository or organization scope because the frontend build job does not enter the production Environment."
  value = {
    PRODUCTION_API_BASE_URL = "https://${azurerm_linux_web_app.backend.default_hostname}/api/v1"
    PRODUCTION_FRONTEND_URL = "https://${azurerm_static_web_app.frontend.default_host_name}"
  }
}
