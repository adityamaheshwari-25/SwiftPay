locals {
  migration_host_name = "vm-${local.stem}-db-migrate"
}

resource "azurerm_subnet" "migration_host" {
  count = var.enable_migration_host ? 1 : 0

  name                 = "snet-db-admin"
  resource_group_name  = data.azurerm_resource_group.production.name
  virtual_network_name = azurerm_virtual_network.production.name
  address_prefixes     = var.migration_host_subnet_prefixes

  default_outbound_access_enabled = false
}

resource "azurerm_network_security_group" "migration_host" {
  count = var.enable_migration_host ? 1 : 0

  name                = "nsg-${local.stem}-db-migrate"
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  tags                = local.common_tags

  security_rule {
    name                       = "AllowSshFromApprovedOperator"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = var.migration_operator_cidr
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "DenyOtherInbound"
    priority                   = 4096
    direction                  = "Inbound"
    access                     = "Deny"
    protocol                   = "*"
    source_port_range          = "*"
    destination_port_range     = "*"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "AllowAzurePlatformDnsUdp"
    priority                   = 100
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Udp"
    source_port_range          = "*"
    destination_port_range     = "53"
    source_address_prefix      = "*"
    destination_address_prefix = "168.63.129.16"
  }

  security_rule {
    name                       = "AllowAzurePlatformDnsTcp"
    priority                   = 110
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "53"
    source_address_prefix      = "*"
    destination_address_prefix = "168.63.129.16"
  }

  security_rule {
    name                       = "AllowProductionMysql"
    priority                   = 120
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "3306"
    source_address_prefix      = "*"
    destination_address_prefix = azurerm_private_endpoint.mysql.private_service_connection[0].private_ip_address
  }

  security_rule {
    name                       = "AllowPrivateAzureServicesHttps"
    priority                   = 130
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "VirtualNetwork"
  }

  security_rule {
    name                       = "AllowAzureControlPlaneHttps"
    priority                   = 140
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "*"
    destination_address_prefix = "AzureCloud"
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
    name                       = "AllowInternetPackageRepositoriesHttp"
    priority                   = 160
    direction                  = "Outbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "80"
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

resource "azurerm_subnet_network_security_group_association" "migration_host" {
  count = var.enable_migration_host ? 1 : 0

  subnet_id                 = azurerm_subnet.migration_host[0].id
  network_security_group_id = azurerm_network_security_group.migration_host[0].id
}

resource "azurerm_subnet_nat_gateway_association" "migration_host" {
  count = var.enable_migration_host ? 1 : 0

  subnet_id      = azurerm_subnet.migration_host[0].id
  nat_gateway_id = azurerm_nat_gateway.production.id
}

resource "azurerm_public_ip" "migration_host" {
  count = var.enable_migration_host ? 1 : 0

  name                = "pip-${local.stem}-db-migrate"
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  allocation_method   = "Static"
  sku                 = "Standard"
  tags                = local.common_tags
}

resource "azurerm_network_interface" "migration_host" {
  count = var.enable_migration_host ? 1 : 0

  name                = "nic-${local.stem}-db-migrate"
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  tags                = local.common_tags

  ip_configuration {
    name                          = "primary"
    subnet_id                     = azurerm_subnet.migration_host[0].id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.migration_host[0].id
  }

  depends_on = [
    azurerm_subnet_nat_gateway_association.migration_host,
    azurerm_subnet_network_security_group_association.migration_host,
  ]
}

resource "azurerm_linux_virtual_machine" "migration_host" {
  count = var.enable_migration_host ? 1 : 0

  name                = local.migration_host_name
  computer_name       = "dbmigrate"
  resource_group_name = data.azurerm_resource_group.production.name
  location            = data.azurerm_resource_group.production.location
  size                = var.migration_host_vm_size
  admin_username      = "azureadmin"

  network_interface_ids = [azurerm_network_interface.migration_host[0].id]

  disable_password_authentication = true
  secure_boot_enabled             = true
  vtpm_enabled                    = true

  admin_ssh_key {
    username   = "azureadmin"
    public_key = trimspace(var.migration_host_ssh_public_key)
  }

  identity {
    type = "SystemAssigned"
  }

  os_disk {
    name                 = "osdisk-${local.stem}-db-migrate"
    caching              = "ReadWrite"
    storage_account_type = "StandardSSD_LRS"
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "ubuntu-24_04-lts"
    sku       = "server"
    version   = "latest"
  }

  custom_data = base64encode(<<-CLOUD_INIT
    #cloud-config
    package_update: true
    packages:
      - ca-certificates
      - curl
      - docker.io
      - git
      - jq
      - mysql-client
    runcmd:
      - systemctl enable --now docker
      - usermod -aG docker azureadmin
      - docker pull flyway/flyway:11.14.1
  CLOUD_INIT
  )

  boot_diagnostics {}

  tags = merge(local.common_tags, {
    component = "temporary-database-migration"
    expires   = "remove-after-approved-migration"
  })
}
