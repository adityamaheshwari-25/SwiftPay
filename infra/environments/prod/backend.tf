terraform {
  # Supply the storage details at init time with backend.hcl. The backend uses
  # Microsoft Entra ID/OIDC; storage account keys are disabled by bootstrap.
  backend "azurerm" {}
}

