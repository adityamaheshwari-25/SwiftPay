resource "azurerm_virtual_network" "production" {
  name                = local.names.virtual_network
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  address_space       = var.vnet_address_space
  tags                = local.common_tags
}

resource "azurerm_subnet" "app_integration" {
  name                 = "snet-app-integration"
  resource_group_name  = data.azurerm_resource_group.production.name
  virtual_network_name = azurerm_virtual_network.production.name
  address_prefixes     = var.app_integration_subnet_prefixes

  default_outbound_access_enabled = false

  delegation {
    name = "app-service-delegation"

    service_delegation {
      name = "Microsoft.Web/serverFarms"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/action",
      ]
    }
  }
}

resource "azurerm_subnet" "private_endpoints" {
  name                 = "snet-private-endpoints"
  resource_group_name  = data.azurerm_resource_group.production.name
  virtual_network_name = azurerm_virtual_network.production.name
  address_prefixes     = var.private_endpoint_subnet_prefixes

  default_outbound_access_enabled   = false
  private_endpoint_network_policies = "Disabled"
}

resource "azurerm_public_ip" "nat" {
  name                = local.names.nat_public_ip
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  allocation_method   = "Static"
  sku                 = "Standard"
  tags                = local.common_tags
}

resource "azurerm_nat_gateway" "production" {
  name                    = local.names.nat_gateway
  resource_group_name     = data.azurerm_resource_group.production.name
  location                = data.azurerm_resource_group.production.location
  sku_name                = "Standard"
  idle_timeout_in_minutes = 10
  tags                    = local.common_tags
}

resource "azurerm_nat_gateway_public_ip_association" "production" {
  nat_gateway_id       = azurerm_nat_gateway.production.id
  public_ip_address_id = azurerm_public_ip.nat.id
}

resource "azurerm_subnet_nat_gateway_association" "app_integration" {
  subnet_id      = azurerm_subnet.app_integration.id
  nat_gateway_id = azurerm_nat_gateway.production.id
}

resource "azurerm_network_security_group" "app_integration" {
  name                = local.names.network_security
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  tags                = local.common_tags

  security_rule {
    name                       = "AllowVirtualNetwork"
    priority                   = 110
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "*"
    source_address_prefix      = "*"
    destination_address_prefix = "VirtualNetwork"
  }

  security_rule {
    name                       = "AllowEntraIdHttps"
    priority                   = 120
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "AzureActiveDirectory"
  }

  security_rule {
    name                       = "AllowAzureMonitorHttps"
    priority                   = 130
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "AzureMonitor"
  }

  security_rule {
    name                       = "AllowStorageHttps"
    priority                   = 140
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "Storage"
  }

  # TLS certificate-chain validation can require public CRL/OCSP endpoints over
  # HTTP. Replace this broad Internet rule with FQDN controls on Azure Firewall
  # if centralized egress inspection is introduced later.
  security_rule {
    name                       = "AllowInternetCertificateValidationHttp"
    priority                   = 145
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "80"
    source_address_prefix      = "*"
    destination_address_prefix = "Internet"
  }

  security_rule {
    name                       = "AllowInternetHttps"
    priority                   = 150
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "Internet"
  }

  security_rule {
    name                       = "DenyOtherOutbound"
    priority                   = 4096
    direction                  = "Outbound"
    access                     = "Deny"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "*"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
}

resource "azurerm_subnet_network_security_group_association" "app_integration" {
  subnet_id                 = azurerm_subnet.app_integration.id
  network_security_group_id = azurerm_network_security_group.app_integration.id
}

resource "azurerm_private_dns_zone" "mysql" {
  name                = "privatelink.mysql.database.azure.com"
  resource_group_name = data.azurerm_resource_group.production.name
  tags                = local.common_tags
}

resource "azurerm_private_dns_zone" "key_vault" {
  name                = "privatelink.vaultcore.azure.net"
  resource_group_name = data.azurerm_resource_group.production.name
  tags                = local.common_tags
}

resource "azurerm_private_dns_zone" "blob" {
  name                = "privatelink.blob.core.windows.net"
  resource_group_name = data.azurerm_resource_group.production.name
  tags                = local.common_tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "mysql" {
  name                  = "link-${local.stem}-mysql"
  resource_group_name   = data.azurerm_resource_group.production.name
  private_dns_zone_name = azurerm_private_dns_zone.mysql.name
  virtual_network_id    = azurerm_virtual_network.production.id
  registration_enabled  = false
  tags                  = local.common_tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "key_vault" {
  name                  = "link-${local.stem}-key-vault"
  resource_group_name   = data.azurerm_resource_group.production.name
  private_dns_zone_name = azurerm_private_dns_zone.key_vault.name
  virtual_network_id    = azurerm_virtual_network.production.id
  registration_enabled  = false
  tags                  = local.common_tags
}

resource "azurerm_private_dns_zone_virtual_network_link" "blob" {
  name                  = "link-${local.stem}-blob"
  resource_group_name   = data.azurerm_resource_group.production.name
  private_dns_zone_name = azurerm_private_dns_zone.blob.name
  virtual_network_id    = azurerm_virtual_network.production.id
  registration_enabled  = false
  tags                  = local.common_tags
}
