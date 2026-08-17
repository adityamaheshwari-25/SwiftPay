variable "subscription_id" {
  description = "Azure subscription containing the existing production resource group."
  type        = string

  validation {
    condition     = can(regex("^[0-9a-fA-F-]{36}$", var.subscription_id))
    error_message = "subscription_id must be an Azure subscription UUID."
  }
}

variable "resource_group_name" {
  description = "Name of the existing production resource group created by bootstrap."
  type        = string
}

variable "backend_deploy_principal_object_id" {
  description = "Object/principal ID of the backend deployment managed identity created by bootstrap."
  type        = string

  validation {
    condition     = can(regex("^[0-9a-fA-F-]{36}$", var.backend_deploy_principal_object_id))
    error_message = "backend_deploy_principal_object_id must be a Microsoft Entra object UUID, not a client ID."
  }
}

variable "name_prefix" {
  description = "Lowercase workload prefix used in Azure resource names."
  type        = string
  default     = "swiftpay"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,18}[a-z0-9]$", var.name_prefix))
    error_message = "name_prefix must be 3-20 lowercase letters, digits, or hyphens, beginning with a letter and ending with a letter or digit."
  }
}

variable "environment" {
  description = "Environment label."
  type        = string
  default     = "prod"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,8}[a-z0-9]$", var.environment))
    error_message = "environment must be 3-10 lowercase letters, digits, or hyphens."
  }
}

variable "unique_suffix" {
  description = "Globally unique 3-8 character suffix; use the same value as bootstrap."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9]{3,8}$", var.unique_suffix))
    error_message = "unique_suffix must contain 3-8 lowercase letters or digits."
  }
}

variable "tags" {
  description = "Additional tags merged into all taggable production resources."
  type        = map(string)
  default     = {}
}

variable "static_web_app_location" {
  description = "Azure Static Web Apps control-plane region. This service is not available in Central India."
  type        = string
  default     = "East Asia"

  validation {
    condition = contains([
      "Central US",
      "East Asia",
      "East US 2",
      "West Europe",
      "West US 2",
    ], var.static_web_app_location)
    error_message = "static_web_app_location must be a currently supported Static Web Apps region: Central US, East Asia, East US 2, West Europe, or West US 2."
  }
}

variable "vnet_address_space" {
  description = "Production virtual network address space."
  type        = list(string)
  default     = ["10.20.0.0/16"]
}

variable "app_integration_subnet_prefixes" {
  description = "Dedicated subnet delegated to App Service regional VNet integration."
  type        = list(string)
  default     = ["10.20.1.0/26"]
}

variable "private_endpoint_subnet_prefixes" {
  description = "Dedicated subnet for Storage, Key Vault, and MySQL private endpoints."
  type        = list(string)
  default     = ["10.20.3.0/24"]
}

variable "database_name" {
  description = "MySQL application database name."
  type        = string
  default     = "swiftpay"

  validation {
    condition     = can(regex("^[A-Za-z][A-Za-z0-9_]{0,62}$", var.database_name))
    error_message = "database_name must be a valid MySQL identifier of at most 63 characters."
  }
}

variable "database_admin_username" {
  description = "MySQL administrator login."
  type        = string
  default     = "swiftpayadmin"

  validation {
    condition     = length(var.database_admin_username) >= 1 && length(var.database_admin_username) <= 32
    error_message = "database_admin_username must be between 1 and 32 characters."
  }
}

variable "database_admin_password" {
  description = "MySQL administrator password. Supply through TF_VAR_database_admin_password. Stored with write-only provider fields."
  type        = string
  sensitive   = true
  ephemeral   = true

  validation {
    condition     = length(var.database_admin_password) >= 12 && length(var.database_admin_password) <= 128
    error_message = "database_admin_password must be between 12 and 128 characters."
  }
}

variable "database_admin_password_version" {
  description = "Increment to rotate the write-only MySQL administrator password."
  type        = number
  default     = 1

  validation {
    condition     = var.database_admin_password_version >= 1 && floor(var.database_admin_password_version) == var.database_admin_password_version
    error_message = "database_admin_password_version must be a positive integer."
  }
}

variable "database_application_username" {
  description = "Least-privilege MySQL login used by App Service. Create and grant it through the reviewed, VNet-connected migration process before deploying the backend."
  type        = string
  default     = "swiftpayapp"

  validation {
    condition     = can(regex("^[A-Za-z][A-Za-z0-9_]{0,31}$", var.database_application_username))
    error_message = "database_application_username must be a valid MySQL identifier of at most 32 characters."
  }
}

variable "database_application_password" {
  description = "Password for the least-privilege runtime MySQL login. Supply through TF_VAR_database_application_password; Terraform writes it only to Key Vault and does not create the database login."
  type        = string
  sensitive   = true
  ephemeral   = true

  validation {
    condition     = length(var.database_application_password) >= 16 && length(var.database_application_password) <= 128
    error_message = "database_application_password must be between 16 and 128 characters."
  }
}

variable "database_application_password_version" {
  description = "Increment after coordinating a runtime MySQL password rotation to publish a new write-only Key Vault secret value."
  type        = number
  default     = 1

  validation {
    condition     = var.database_application_password_version >= 1 && floor(var.database_application_password_version) == var.database_application_password_version
    error_message = "database_application_password_version must be a positive integer."
  }
}

variable "mysql_version" {
  description = "Azure Database for MySQL Flexible Server engine version. Validate application compatibility before changing."
  type        = string
  default     = "8.0.21"

  validation {
    condition     = contains(["8.0.21", "8.4"], var.mysql_version)
    error_message = "mysql_version must be 8.0.21 or 8.4."
  }
}

variable "mysql_location" {
  description = "Azure region for MySQL Flexible Server. This can differ from the application region when Private Link is used."
  type        = string
  default     = "South India"
}

variable "mysql_sku_name" {
  description = "Production MySQL Flexible Server SKU."
  type        = string
  default     = "GP_Standard_D2ds_v4"
}

variable "mysql_storage_size_gb" {
  description = "Initial MySQL storage allocation. Storage can grow but cannot be reduced in place."
  type        = number
  default     = 128

  validation {
    condition     = var.mysql_storage_size_gb >= 20 && var.mysql_storage_size_gb <= 16384
    error_message = "mysql_storage_size_gb must be between 20 and 16384."
  }
}

variable "mysql_backup_retention_days" {
  description = "Point-in-time restore retention for MySQL."
  type        = number
  default     = 35

  validation {
    condition     = var.mysql_backup_retention_days >= 7 && var.mysql_backup_retention_days <= 35
    error_message = "mysql_backup_retention_days must be between 7 and 35."
  }
}

variable "mysql_geo_redundant_backup_enabled" {
  description = "Enable geo-redundant MySQL backups when the selected region/SKU supports them."
  type        = bool
  default     = false
}

variable "mysql_zone_redundant_high_availability_enabled" {
  description = "Deploy a zone-redundant MySQL standby only when the selected region and subscription support it."
  type        = bool
  default     = false
}

variable "mysql_primary_zone" {
  description = "Optional primary availability zone. Null lets Azure select it."
  type        = string
  default     = null
  nullable    = true

  validation {
    condition     = var.mysql_primary_zone == null ? true : contains(["1", "2", "3"], var.mysql_primary_zone)
    error_message = "mysql_primary_zone must be null, 1, 2, or 3."
  }
}

variable "mysql_standby_zone" {
  description = "Optional standby availability zone. Null lets Azure select it."
  type        = string
  default     = null
  nullable    = true

  validation {
    condition     = var.mysql_standby_zone == null ? true : contains(["1", "2", "3"], var.mysql_standby_zone)
    error_message = "mysql_standby_zone must be null, 1, 2, or 3."
  }
}

variable "service_plan_sku_name" {
  description = "Linux App Service Plan SKU. Standard S1 supports the staging slot without requiring PremiumV3 quota."
  type        = string
  default     = "S1"
}

variable "app_service_instance_count" {
  description = "Active App Service workers. Keep at 1 until scheduled jobs and in-memory SSE are made horizontally safe."
  type        = number
  default     = 1

  validation {
    condition     = var.app_service_instance_count >= 1 && floor(var.app_service_instance_count) == var.app_service_instance_count
    error_message = "app_service_instance_count must be a positive integer."
  }
}

variable "app_service_zone_balancing_enabled" {
  description = "Enable App Service zone balancing only after setting a compatible multi-instance worker count."
  type        = bool
  default     = false
}

variable "enable_app_service_autoscale" {
  description = "Enable CPU autoscale after the application is horizontally safe."
  type        = bool
  default     = false
}

variable "app_service_autoscale_min_instances" {
  description = "Minimum worker count when autoscale is enabled."
  type        = number
  default     = 1
}

variable "app_service_autoscale_default_instances" {
  description = "Default worker count when autoscale is enabled."
  type        = number
  default     = 1
}

variable "app_service_autoscale_max_instances" {
  description = "Maximum worker count when autoscale is enabled. Increase only after horizontal-safety work."
  type        = number
  default     = 1
}

variable "hikari_maximum_pool_size" {
  description = "Maximum database connections per App Service instance. Account for production and staging slots."
  type        = number
  default     = 10

  validation {
    condition     = var.hikari_maximum_pool_size >= 2 && var.hikari_maximum_pool_size <= 100
    error_message = "hikari_maximum_pool_size must be between 2 and 100."
  }
}

variable "hikari_minimum_idle" {
  description = "Minimum idle database connections per App Service instance."
  type        = number
  default     = 2

  validation {
    condition     = var.hikari_minimum_idle >= 0 && var.hikari_minimum_idle <= 100
    error_message = "hikari_minimum_idle must be between 0 and 100."
  }
}

variable "hikari_connection_timeout_ms" {
  description = "Hikari connection acquisition timeout in milliseconds."
  type        = number
  default     = 30000
}

variable "hikari_idle_timeout_ms" {
  description = "Hikari idle connection timeout in milliseconds."
  type        = number
  default     = 600000
}

variable "hikari_max_lifetime_ms" {
  description = "Hikari maximum connection lifetime in milliseconds."
  type        = number
  default     = 1800000
}

variable "hikari_validation_timeout_ms" {
  description = "Hikari connection validation timeout in milliseconds."
  type        = number
  default     = 5000
}

variable "jwt_secret" {
  description = "High-entropy HMAC signing secret. Supply through TF_VAR_jwt_secret."
  type        = string
  sensitive   = true
  ephemeral   = true

  validation {
    condition     = length(var.jwt_secret) >= 32
    error_message = "jwt_secret must contain at least 32 characters of high-entropy material."
  }
}

variable "jwt_expiration" {
  description = "JWT lifetime as a Spring Duration (for example 10h, 30m, or 3600s)."
  type        = string
  default     = "10h"

  validation {
    condition     = can(regex("^[1-9][0-9]*(ms|s|m|h|d)$", var.jwt_expiration))
    error_message = "jwt_expiration must be a positive Spring Duration such as 10h, 30m, or 3600s."
  }
}

variable "superadmin_email" {
  description = "Bootstrap super-administrator email consumed by the backend."
  type        = string

  validation {
    condition     = can(regex("^[^@]+@[^@]+\\.[^@]+$", var.superadmin_email))
    error_message = "superadmin_email must be a valid email address."
  }
}

variable "superadmin_password" {
  description = "Bootstrap super-administrator password. Supply through TF_VAR_superadmin_password."
  type        = string
  sensitive   = true
  ephemeral   = true

  validation {
    condition     = length(var.superadmin_password) >= 14
    error_message = "superadmin_password must contain at least 14 characters."
  }
}

variable "application_secret_version" {
  description = "Increment to rotate JWT and super-administrator Key Vault secrets."
  type        = number
  default     = 1

  validation {
    condition     = var.application_secret_version >= 1 && floor(var.application_secret_version) == var.application_secret_version
    error_message = "application_secret_version must be a positive integer."
  }
}

variable "jpa_ddl_auto" {
  description = "Hibernate schema strategy. Production is locked to validate so startup and slot health checks cannot mutate the shared production schema."
  type        = string
  default     = "validate"

  validation {
    condition     = var.jpa_ddl_auto == "validate"
    error_message = "jpa_ddl_auto must remain validate in production; apply reviewed versioned migrations separately."
  }
}

variable "additional_cors_allowed_origins" {
  description = "Additional exact HTTPS origins, such as a future custom frontend domain. The Static Web App origin is always included."
  type        = list(string)
  default     = []

  validation {
    condition = alltrue([
      for origin in var.additional_cors_allowed_origins :
      can(regex("^https://(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?::[1-9][0-9]{0,4})?$", origin))
    ])
    error_message = "Each CORS entry must be an exact HTTPS DNS origin with an optional port and no wildcard, credentials, path, query, fragment, or trailing slash."
  }
}

variable "kyc_container_name" {
  description = "Private Blob container used for KYC documents."
  type        = string
  default     = "kyc-documents"

  validation {
    condition     = can(regex("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?$", var.kyc_container_name))
    error_message = "kyc_container_name must be a valid lowercase Blob container name."
  }
}

variable "log_analytics_retention_days" {
  description = "Log Analytics retention in days."
  type        = number
  default     = 90

  validation {
    condition     = var.log_analytics_retention_days >= 30 && var.log_analytics_retention_days <= 730
    error_message = "log_analytics_retention_days must be between 30 and 730."
  }
}

variable "monitoring_daily_cap_gb" {
  description = "Daily Log Analytics/Application Insights ingestion cap in GB to bound unexpected telemetry cost."
  type        = number
  default     = 5

  validation {
    condition     = var.monitoring_daily_cap_gb >= 1
    error_message = "monitoring_daily_cap_gb must be at least 1 GB."
  }
}

variable "alert_email_receivers" {
  description = "Non-empty map of Azure Monitor receiver names to notification email addresses."
  type        = map(string)

  validation {
    condition = (
      length(var.alert_email_receivers) > 0 &&
      alltrue([
        for name, email in var.alert_email_receivers :
        length(name) >= 1 && length(name) <= 64 && can(regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", email))
      ])
    )
    error_message = "alert_email_receivers must contain at least one receiver; names must be 1-64 characters and values must be valid email addresses."
  }
}

variable "enable_delete_locks" {
  description = "Apply CanNotDelete locks to the production database, vault, and storage account. Disable before an intentional destroy/replacement."
  type        = bool
  default     = true
}

variable "enable_migration_host" {
  description = "Temporarily create the SSH-key-only production database migration VM and its isolated administration subnet. Disable immediately after migration evidence is captured."
  type        = bool
  default     = false
}

variable "migration_host_subnet_prefixes" {
  description = "Dedicated subnet used only by the temporary database migration VM."
  type        = list(string)
  default     = ["10.20.4.0/27"]

  validation {
    condition     = length(var.migration_host_subnet_prefixes) == 1 && can(cidrhost(var.migration_host_subnet_prefixes[0], 1))
    error_message = "migration_host_subnet_prefixes must contain exactly one valid CIDR prefix."
  }
}

variable "migration_operator_cidr" {
  description = "Single trusted public IPv4 address in /32 notation allowed to SSH to the temporary migration VM. Required only when enable_migration_host=true."
  type        = string
  default     = null
  nullable    = true

  validation {
    condition = (
      !var.enable_migration_host ||
      try(cidrnetmask(var.migration_operator_cidr) == "255.255.255.255", false)
    )
    error_message = "migration_operator_cidr must be a valid public IPv4 /32 when enable_migration_host=true."
  }
}

variable "migration_host_ssh_public_key" {
  description = "OpenSSH public key used for the temporary migration VM. Required only when enable_migration_host=true."
  type        = string
  default     = null
  nullable    = true

  validation {
    condition = (
      !var.enable_migration_host ||
      try(can(regex("^(ssh-ed25519|ssh-rsa|ecdsa-sha2-nistp(256|384|521)) [A-Za-z0-9+/]+={0,3}( .*)?$", trimspace(var.migration_host_ssh_public_key))), false)
    )
    error_message = "migration_host_ssh_public_key must be a valid OpenSSH public key when enable_migration_host=true."
  }
}

variable "migration_host_vm_size" {
  description = "Azure VM size for the temporary database migration host."
  type        = string
  default     = "Standard_D2as_v5"

  validation {
    condition     = can(regex("^Standard_[A-Za-z0-9_]+$", var.migration_host_vm_size))
    error_message = "migration_host_vm_size must be a valid Azure Standard VM SKU name."
  }
}
