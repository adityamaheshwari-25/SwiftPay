variable "subscription_id" {
  description = "Azure subscription in which the bootstrap and production resource groups will be created."
  type        = string

  validation {
    condition     = can(regex("^[0-9a-fA-F-]{36}$", var.subscription_id))
    error_message = "subscription_id must be an Azure subscription UUID."
  }
}

variable "location" {
  description = "Primary Azure region for the bootstrap and production resource groups."
  type        = string
  default     = "Central India"
}

variable "name_prefix" {
  description = "Lowercase workload prefix used in resource names."
  type        = string
  default     = "swiftpay"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,18}[a-z0-9]$", var.name_prefix))
    error_message = "name_prefix must be 3-20 lowercase letters, digits, or hyphens, beginning with a letter and ending with a letter or digit."
  }
}

variable "environment" {
  description = "Production environment label."
  type        = string
  default     = "prod"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,8}[a-z0-9]$", var.environment))
    error_message = "environment must be 3-10 lowercase letters, digits, or hyphens."
  }
}

variable "unique_suffix" {
  description = "Globally unique 3-8 character suffix used for the state storage account and CI identities."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9]{3,8}$", var.unique_suffix))
    error_message = "unique_suffix must contain 3-8 lowercase letters or digits."
  }
}

variable "github_organization" {
  description = "GitHub organization or user that owns the repository."
  type        = string
}

variable "github_owner_id" {
  description = "Immutable numeric GitHub ID of the organization or user that owns the repository."
  type        = string

  validation {
    condition     = can(regex("^[0-9]+$", var.github_owner_id))
    error_message = "github_owner_id must be the numeric GitHub owner ID."
  }
}

variable "github_repository" {
  description = "GitHub repository name, without the owner."
  type        = string
}

variable "github_repository_id" {
  description = "Immutable numeric GitHub repository ID used in OIDC subject claims."
  type        = string

  validation {
    condition     = can(regex("^[0-9]+$", var.github_repository_id))
    error_message = "github_repository_id must be the numeric GitHub repository ID."
  }
}

variable "terraform_github_environment" {
  description = "Protected GitHub Environment whose infrastructure workflow may exchange OIDC tokens for the Terraform CI identity."
  type        = string
  default     = "production-infrastructure"
}

variable "deploy_github_environment" {
  description = "Protected GitHub Environment whose backend workflow may exchange OIDC tokens for the deployment identity."
  type        = string
  default     = "production"
}

variable "state_blob_retention_days" {
  description = "Soft-delete retention for Terraform state blobs and containers."
  type        = number
  default     = 30

  validation {
    condition     = var.state_blob_retention_days >= 7 && var.state_blob_retention_days <= 365
    error_message = "state_blob_retention_days must be between 7 and 365."
  }
}

variable "tags" {
  description = "Additional tags to merge into all taggable bootstrap resources."
  type        = map(string)
  default     = {}
}
