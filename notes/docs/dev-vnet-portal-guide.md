# Add a VNet to the existing development environment in Azure Portal

This guide adds private outbound networking to the existing SwiftPay development
environment without rebuilding the App Service. It is written for the Azure
Portal so you can see what each resource and connection does.

> Read this before changing anything: App Service VNet integration controls
> **outbound** traffic from the Java backend. It does not put the backend's
> public HTTPS endpoint inside the VNet. The React application runs in each
> user's browser, so the backend must remain publicly reachable unless you add a
> separate public gateway or require users to connect through a VPN.

## Result

```text
User's browser
  |-- HTTPS --> Azure Static Web Apps (public)
  `-- HTTPS --> Java App Service (public inbound endpoint)
                       |
                       | outbound VNet integration
                       v
              snet-appservice-dev (/26)
                       |
          +------------+-------------+
          |                          |
          v                          v
  MySQL private endpoint     snet-private-endpoints-dev (/24)
  or existing private mode      |-- Key Vault private endpoint
                                `-- Blob private endpoint
```

The VNet provides a private route from the backend to MySQL, Key Vault, and Blob
Storage. Static Web Apps does not need to join this VNet for the current public
SPA architecture.

## Important MySQL decision

Open the development **Azure Database for MySQL flexible server**, select
**Networking**, and note its connectivity method.

- If it says **Public access (allowed IP addresses) and Private endpoint**, use
  the private-endpoint steps in this guide. Azure does not support converting
  this existing server in place to **Private access (VNet Integration)**.
- If it says **Private access (VNet Integration)**, the database is already in a
  delegated subnet. Do not create a MySQL private endpoint. Prefer adding the
  App Service integration subnet to that same VNet. If you must use a separate
  VNet, configure VNet peering as described in Path B; a private DNS link by
  itself does not create network reachability.
- Do not recreate the database just to follow the production network layout.
  A rebuild requires a tested dump/restore or point-in-time-restore migration.

Production Terraform creates a new MySQL server with private VNet integration
from the beginning. That is why the production layout differs from an existing
public-mode dev server.

## 1. Pre-change checklist

Before the portal changes:

1. Schedule a small maintenance window. Creating the VNet is non-disruptive,
   but disabling public access can interrupt the backend if DNS or routing is
   wrong.
2. Confirm the App Service and VNet will be in the same Azure region. Regional
   App Service VNet integration requires this.
3. Confirm the App Service Plan is a dedicated tier that supports VNet
   integration. Free and Shared plans do not.
4. On the MySQL server, open **Backup and restore** and confirm a recent backup
   and point-in-time restore window.
5. Record these current values:

   - Resource group name
   - App Service name and default hostname
   - App Service Plan and region
   - MySQL server name and hostname
   - Key Vault name
   - Storage account name and Blob endpoint
   - Current MySQL firewall rules
   - Current public-network settings on MySQL, Key Vault, and Storage

6. Open `https://<dev-app-hostname>/actuator/health` and save the healthy result
   as the before-change baseline.
7. Choose a non-overlapping address range. This guide uses `10.10.0.0/16`.
   Check all existing VNets and any office/VPN networks first. If that range is
   already used, choose another RFC1918 range and adjust the subnets below.

## 2. Create the development VNet

If MySQL already uses **Private access (VNet Integration)**, first open the
server's **Networking** page and identify its VNet. Prefer using that existing
VNet in the steps below: add new, non-overlapping App Service and private-
endpoint subnets to it and substitute its name for `vnet-swiftpay-dev`. Create
a separate VNet only when your network design requires it, then complete the
bidirectional peering steps in Path B before testing.

1. In [Azure Portal](https://portal.azure.com), search for **Virtual networks**.
2. Select **Create**.
3. On **Basics**, select the dev subscription and resource group, then enter:

   | Setting | Value |
   | --- | --- |
   | Name | `vnet-swiftpay-dev` |
   | Region | The App Service region |

4. On **IP addresses**, set the IPv4 address space to `10.10.0.0/16`.
5. Remove any automatically proposed subnet if it conflicts with this plan.
6. Create these two subnets:

   | Subnet | CIDR | Purpose |
   | --- | --- | --- |
   | `snet-appservice-dev` | `10.10.1.0/26` | Dedicated App Service integration subnet |
   | `snet-private-endpoints-dev` | `10.10.2.0/24` | MySQL, Key Vault, and Blob private endpoints |

7. For `snet-appservice-dev`, set **Subnet delegation** to
   `Microsoft.Web/serverFarms`. Do not place VMs, private endpoints, or other
   resources in this subnet.
8. Leave the private-endpoint subnet undelegated. If the portal shows a
   **Private endpoint network policy** setting, keep it disabled unless you
   intentionally designed NSG/route-table policies for private endpoints.
9. Select **Review + create**, then **Create**.

Why `/26` for App Service: Azure permits smaller subnets in some cases, but
recommends `/26` so scale-out and platform upgrade operations do not exhaust
addresses.

## 3. Integrate the backend App Service

1. Open the development **App Service**.
2. Select **Settings > Networking**.
3. Under **Outbound traffic configuration**, select **Virtual network
   integration**. The link may initially say **Not configured**.
4. Select **Add virtual network integration** or **Add**.
5. Choose `vnet-swiftpay-dev` and `snet-appservice-dev`, then select **Connect**.
6. Wait for the connection status to show **Connected**.
7. Do not disable public inbound access on the App Service. The browser-hosted
   frontend still needs its public API and SSE endpoints.
8. Leave **Route all** off until the private endpoints and DNS checks below pass.

The portal may add the `Microsoft.Web/serverFarms` delegation automatically if
you did not add it while creating the subnet.

## 4. Add private access to the existing MySQL server

Follow exactly one of the two paths below.

### Path A: the server was created in public-access mode

1. Open the MySQL Flexible Server.
2. Select **Networking > Private endpoint connections**.
3. Select **Create private endpoint**.
4. On **Basics**, use the dev subscription/resource group, a name such as
   `pe-mysql-swiftpay-dev`, and the VNet's region.
5. On **Resource**, confirm the selected resource is the existing MySQL server
   and select its MySQL server subresource (normally `mysqlServer`).
6. On **Virtual network**, select:

   - Virtual network: `vnet-swiftpay-dev`
   - Subnet: `snet-private-endpoints-dev`

7. On **DNS**, enable private DNS integration and use/create
   `privatelink.mysql.database.azure.com` linked to `vnet-swiftpay-dev`.
8. Create the endpoint and wait for its connection state to show **Approved**.
9. Keep MySQL public access enabled for now. It is the rollback path until the
   backend test passes.

This adds Private Link to the existing public-connectivity server; it does not
change the server to MySQL's mutually exclusive delegated-VNet connectivity
mode.

### Path B: the server already uses private VNet integration

1. Open the MySQL server's **Networking** page and identify its delegated VNet,
   delegated subnet, and private DNS zone.
2. If App Service is integrated with that same VNet, no peering is needed.
   Continue with step 6.
3. If App Service is integrated with a separate `vnet-swiftpay-dev`, open one
   VNet, select **Peerings > Add**, and create both directions of the peering:

   - `vnet-swiftpay-dev-to-mysql-vnet`
   - `mysql-vnet-to-vnet-swiftpay-dev`

   Allow virtual-network access in both directions. Gateway transit and remote
   gateways are not needed unless your existing hub-and-spoke design explicitly
   requires them.
4. Confirm both peering directions show **Connected**. Review NSGs and route
   tables on both subnets so traffic from `snet-appservice-dev` to the MySQL
   delegated subnet on TCP `3306` is allowed.
5. Check **Effective routes** or your organization routing controls and confirm
   the delegated MySQL address range is reachable from the App Service VNet.
6. Search Azure Portal for **Private DNS zones** and open the zone used by the
   MySQL server.
7. Under **Virtual network links**, confirm the App Service integration VNet is
   linked. If it is missing, select **Add**, use a name such as
   `link-swiftpay-dev`, select that VNet, leave automatic VM registration
   disabled, and save.
8. Do not add a private endpoint to this server. Its delegated-subnet network
   mode and private-endpoint mode are different deployment models.

## 5. Add a Key Vault private endpoint

1. Open the development **Key Vault**.
2. Select **Networking > Private endpoint connections**, then **Create**.
3. Use a name such as `pe-kv-swiftpay-dev` and the VNet's region.
4. Select the existing vault and the `vault` subresource.
5. Select `vnet-swiftpay-dev` and `snet-private-endpoints-dev`.
6. Enable private DNS integration and use/create
   `privatelink.vaultcore.azure.net` linked to the VNet.
7. Create the endpoint and wait for **Approved**.
8. Keep Key Vault public network access unchanged until validation is complete.

The App Service's system-assigned managed identity must still have **Key Vault
Secrets User**. A private endpoint changes the route; it does not grant access.

## 6. Add a Blob Storage private endpoint

1. Open the development **Storage account**.
2. Select **Security + networking > Networking > Private endpoint
   connections**, then **Create**.
3. Use a name such as `pe-blob-swiftpay-dev` and the VNet's region.
4. Select the existing storage account and only the `blob` subresource. The
   application does not require File, Queue, Table, DFS, or Web endpoints.
5. Select `vnet-swiftpay-dev` and `snet-private-endpoints-dev`.
6. Enable private DNS integration and use/create
   `privatelink.blob.core.windows.net` linked to the VNet.
7. Create the endpoint and wait for **Approved**.

The App Service identity must retain **Storage Blob Data Contributor**, and the
`kyc-documents` container must remain private.

## 7. Validate private DNS before closing public access

Open **App Service > Development Tools > SSH** (or its browser console). From
inside the running App Service, resolve the three normal service hostnames:

```bash
getent hosts <mysql-server>.mysql.database.azure.com
getent hosts <vault-name>.vault.azure.net
getent hosts <storage-account>.blob.core.windows.net
```

`nslookup` can be used if `getent` is unavailable. Each resolution path should
ultimately return an address from `10.10.2.0/24` for resources using private
endpoints. The MySQL delegated-VNet path returns an address from its own
delegated subnet.

Then validate at application level:

1. Restart the App Service so Key Vault references are refreshed.
2. In **Settings > Environment variables**, confirm every Key Vault reference
   reports a resolved status.
3. Confirm `/actuator/health` returns `{"status":"UP"}`.
4. Sign in through the dev frontend and load a database-backed dashboard.
5. Upload and retrieve a disposable KYC test document to exercise Blob Storage.
6. Inspect **Log stream** and Application Insights for DNS, connection timeout,
   `403`, or MySQL TLS errors.

Do not proceed if any of these tests fail.

## 8. Enable Route All

1. Return to **App Service > Settings > Networking > Virtual network
   integration**.
2. Open the connected VNet integration.
3. Enable **Route all** (the portal may label it **Outbound internet traffic**
   or **Application routing > Route all**) and save.
4. Restart the App Service.
5. Repeat the health, database, Key Vault-reference, and Blob tests.

Route All is especially important for reliable private Key Vault references on
Linux App Service. It also means outbound application traffic is subject to the
VNet's routes and network controls. Do not add a deny-all NSG or user-defined
route until all runtime dependencies are known.

## 9. Disable public data-plane access

Only do this after the private tests pass.

1. **MySQL public-mode server:** open **Networking**, clear **Allow public access
   to this resource through the internet using a public IP address**, and save.
   Keep the private endpoint. Existing firewall rules stop being the active
   path.
2. **Key Vault:** open **Networking > Firewalls and virtual networks**, set
   public network access to **Disabled**, and save.
3. **Storage:** open **Networking > Firewalls and virtual networks**, set public
   network access to **Disabled**, and save.
4. Restart App Service and run the complete validation once more.

After public access is disabled, your laptop cannot browse blobs or read vault
secrets through their data-plane endpoints unless it has private connectivity
to the VNet (VPN, ExpressRoute, a jump host, or another approved private path).
The Azure Portal control plane still shows the resources.

For a lower-cost learning setup, you may temporarily leave public access on and
restrict it with firewalls while proving private routing. That is not the final
security posture used by the production Terraform.

## 10. What not to change

- Do not add the Static Web App to the App Service integration subnet.
- Do not use the App Service integration subnet for private endpoints.
- Do not change `VITE_API_BASE_URL` to a `10.x.x.x` address or private DNS name;
  that code runs in the user's browser.
- Do not disable App Service public ingress while the frontend calls its default
  public hostname.
- Do not enable MySQL's broad **Allow public access from any Azure service**
  option as a substitute for private networking.
- Do not delete the old public firewall rules until the private route is proven
  and the rollback window has closed.

## 11. Troubleshooting

### Hostname still resolves to a public address

Check that the correct private DNS zone exists, contains an A record for the
resource, and has a **Virtual network link** to `vnet-swiftpay-dev`. Use the
service's normal hostname in application settings; do not hard-code the private
endpoint IP.

### Key Vault references show an error

Confirm all four items: VNet integration is connected, Route All is enabled,
`privatelink.vaultcore.azure.net` is linked, and the App Service identity has
**Key Vault Secrets User**. Restart or explicitly refresh App Service Key Vault
references after fixing the route.

### Blob requests return 403

A working private route is not authorization. Confirm the App Service's current
system identity has **Storage Blob Data Contributor** on the storage account or
container. Role changes can take several minutes to propagate.

### MySQL times out

Confirm private DNS first, then port `3306`, the private endpoint approval state,
and TLS in `DB_URL`. Use at least `sslMode=REQUIRED`; prefer
`sslMode=VERIFY_IDENTITY` after confirming the Azure CA chain is trusted so the
certificate hostname is verified as well. If the server was created in
delegated-VNet mode, verify the App Service uses the same VNet or has connected
bidirectional peering, the private DNS zone is linked to the App Service VNet,
and subnet NSGs/routes allow TCP `3306`.

### App works until Route All is enabled

Review effective NSG rules, user-defined routes, and any firewall/NAT design.
Route All also captures internet-bound runtime traffic. Temporarily turn Route
All off to confirm the diagnosis, then fix the route rather than leaving private
Key Vault access unreliable.

## 12. Rollback

If the app fails after public access is disabled:

1. For **Path A only**, re-enable MySQL public network access and restore only
   the previously approved firewall rules; do not open `0.0.0.0/0`.
2. For **Path B**, delegated-VNet connectivity cannot be changed to public mode.
   Restore/fix the App Service VNet integration, bidirectional peering, DNS
   links, NSGs, or routes instead.
3. Re-enable the previous Key Vault and Storage public-network settings if their
   private paths are also failing.
4. Restart App Service and verify health.
5. Turn off Route All if a new VNet route or NSG caused the outage.
6. Keep the VNet and private endpoints for diagnosis. Disconnect App Service
   VNet integration only if the route itself is confirmed to be the cause.
7. Delete private endpoints or DNS links only after the public path is healthy
   and you have captured the failure evidence.

## Completion checklist

- [ ] App Service VNet integration shows Connected.
- [ ] `snet-appservice-dev` is dedicated and delegated to
      `Microsoft.Web/serverFarms`.
- [ ] Private endpoints are in `snet-private-endpoints-dev`.
- [ ] Private DNS zones are linked to `vnet-swiftpay-dev`.
- [ ] Delegated-VNet MySQL uses the same VNet or connected bidirectional peering;
      DNS is not being mistaken for routing.
- [ ] Normal service hostnames resolve privately from App Service.
- [ ] MySQL, Key Vault references, and KYC Blob operations work.
- [ ] Route All is enabled and the application remains healthy.
- [ ] MySQL, Key Vault, and Storage public access is disabled or a temporary
      exception is documented with an expiry date.
- [ ] The App Service API remains reachable over HTTPS from the dev frontend.
- [ ] Application Insights shows successful dependency calls after the change.

## Official references

- [Enable App Service VNet integration](https://learn.microsoft.com/azure/app-service/configure-vnet-integration-enable)
- [App Service VNet integration concepts](https://learn.microsoft.com/azure/app-service/overview-vnet-integration)
- [MySQL Flexible Server networking options](https://learn.microsoft.com/azure/mysql/flexible-server/quickstart-create-server-portal)
- [Deny public access to MySQL by using the portal](https://learn.microsoft.com/azure/mysql/flexible-server/how-to-networking-private-link-deny-public-access)
- [Azure Private Endpoint DNS configuration](https://learn.microsoft.com/azure/private-link/private-endpoint-dns)
- [Key Vault private-link configuration](https://learn.microsoft.com/azure/key-vault/general/private-link-service)
- [Storage private endpoints](https://learn.microsoft.com/azure/storage/common/storage-private-endpoints)
- [App Service Key Vault references](https://learn.microsoft.com/azure/app-service/app-service-key-vault-references)
