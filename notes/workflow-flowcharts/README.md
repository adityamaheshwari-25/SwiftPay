# SwiftPay workflow flowcharts

These diagrams describe the workflows currently present in `.github/workflows`.
A pull request run validates code; it does not deploy production. Production
application deployment is release- or manually initiated, while production
infrastructure apply is manually initiated and approval-gated.

## Overall delivery flow

```mermaid
flowchart TD
    Change[Developer changes code] --> PR[Pull request to main]
    PR --> Path{Changed path}
    Path -->|PaytmCloneBackend| PB[Prod backend: test and package]
    Path -->|PaytmCloneFrontend| PF[Prod frontend: lint and build]
    Path -->|infra| PI[Prod infrastructure: fmt and validate]
    PB --> Review[Required checks and human review]
    PF --> Review
    PI --> Review
    Review --> Merge[Merge to main]

    Merge --> DevPath{Application path changed?}
    DevPath -->|Backend| DB[Dev backend: test, package, deploy, health check]
    DevPath -->|Frontend| DF[Dev frontend: build and deploy]

    Merge --> InfraDecision{Infrastructure change?}
    InfraDecision -->|Yes| ManualInfra[Dispatch Prod infrastructure with plan or apply]
    ManualInfra --> InfraPlan[Create remote-state-backed plan]
    InfraPlan -->|plan| StopPlan[Review plan; no mutation]
    InfraPlan -->|apply| InfraApproval[production-infrastructure approval]
    InfraApproval --> InfraApply[Apply the exact encrypted and checksummed plan]

    Merge --> Release[Publish stable vMAJOR.MINOR.PATCH release]
    Release --> ProdBackend[Prod backend build artifact]
    Release --> ProdFrontend[Prod frontend build artifact]
    ProdBackend --> ProdApproval1[production environment approval]
    ProdFrontend --> ProdApproval2[production environment approval]
    ProdApproval1 --> BackendDeploy[Staging deploy, health check, slot swap, production check]
    ProdApproval2 --> FrontendDeploy[Static Web App deploy and SHA verification]
```

There is no `workflow_call`, `workflow_run`, `repository_dispatch`, or `gh workflow`
step connecting these files. The production application workflows therefore do
not call the infrastructure workflow, and the infrastructure workflow does not
call the application workflows. Their dependency is operational: provision or
update infrastructure first, configure the GitHub environment values produced by
Terraform, and then publish a release.

## Dev backend

```mermaid
flowchart TD
    Trigger[Push to main changing backend, or manual dispatch] --> Checkout[Checkout]
    Checkout --> Java[Set up Java 17 and Maven cache]
    Java --> Verify[Maven clean verify]
    Verify --> OIDC[Azure login using OIDC]
    OIDC --> Resolve[Resolve configured development App Service]
    Resolve --> Deploy[Deploy JAR directly to development]
    Deploy --> Health[Poll actuator health endpoint]
    Health --> Result{Status UP?}
    Result -->|Yes| Success[Success]
    Result -->|No| Fail[Fail deployment]
```

## Dev frontend

```mermaid
flowchart TD
    Trigger[Push to main changing frontend, or manual dispatch] --> Checkout[Checkout]
    Checkout --> Node[Set up Node.js 22 and npm cache]
    Node --> Install[npm ci]
    Install --> Build[Build with development API URL]
    Build --> Deploy[Upload prebuilt dist to development Static Web App]
```

## Production backend

```mermaid
flowchart TD
    Trigger{Trigger} -->|Pull request| BuildPR[Test and package only]
    Trigger -->|Stable published release| RefCheck[Validate SemVer tag and ancestry from main]
    Trigger -->|Manual dispatch on main| BuildManual[Test and package]
    Trigger -->|Manual dispatch off main| Reject[Reject]

    RefCheck --> BuildRelease[Test and package]
    BuildRelease --> Artifact[Select exactly one executable JAR and upload artifact]
    BuildManual --> Artifact
    Artifact --> Approval[production environment approval]
    Approval --> OIDC[Azure login using OIDC]
    OIDC --> Start[Resolve and start staging slot]
    Start --> StageDeploy[Deploy artifact to staging]
    StageDeploy --> StageHealth{Staging healthy?}
    StageHealth -->|No| Fail[Fail and stop staging]
    StageHealth -->|Yes| Swap[Swap staging into production]
    Swap --> ProdHealth{Production healthy?}
    ProdHealth -->|Yes| Stop[Stop staging and succeed]
    ProdHealth -->|No| Rollback[Swap previous version back]
    Rollback --> RollbackHealth[Verify rollback health and stop staging]
```

## Production frontend

```mermaid
flowchart TD
    Trigger{Trigger} -->|Pull request| PRBuild[Lint, validate config, and build only]
    Trigger -->|Stable published release| RefCheck[Validate SemVer tag and ancestry from main]
    Trigger -->|Manual dispatch on main| ManualBuild[Lint, validate config, and build]
    Trigger -->|Manual dispatch off main| Reject[Reject]

    RefCheck --> ReleaseBuild[Lint, validate config, and build]
    ReleaseBuild --> Harden[Restrict CSP and create deployment.json with commit SHA]
    ManualBuild --> Harden
    Harden --> Artifact[Upload immutable production artifact]
    Artifact --> Approval[production environment approval]
    Approval --> Token[Validate deployment token and expected HTTPS origin]
    Token --> Deploy[Deploy prebuilt dist to production Static Web App]
    Deploy --> Verify[Poll deployment.json]
    Verify --> Match{Deployed SHA equals workflow SHA?}
    Match -->|Yes| Success[Success]
    Match -->|No| Fail[Fail deployment verification]
```

## Production infrastructure

```mermaid
flowchart TD
    Trigger{Trigger} -->|Pull request changing infra| Validate[Terraform fmt and validate bootstrap and prod]
    Trigger -->|Manual dispatch off main| Reject[Reject]
    Trigger -->|Manual dispatch on main| Validate
    Validate --> Mode{Event and operation}
    Mode -->|Pull request| ChecksDone[Checks complete; no Azure mutation]
    Mode -->|Manual plan| ProtectedPlan[Enter production-infrastructure environment]
    Mode -->|Manual apply| ProtectedPlan

    ProtectedPlan --> Inputs[Validate variables, secrets, formats, and policy values]
    Inputs --> OIDC[Azure login using OIDC]
    OIDC --> Init[Initialize Azure Blob remote backend]
    Init --> Plan[Create locked production plan]
    Plan --> Operation{Operation}
    Operation -->|plan| PlanDone[Display/review plan; no apply]
    Operation -->|apply| Encrypt[Hash, encrypt, and upload one-day plan artifact]
    Encrypt --> ApplyApproval[Approval gate for apply job]
    ApplyApproval --> Download[Download, decrypt, and verify checksum]
    Download --> ExactApply[Apply the identical saved plan]
    ExactApply --> Outputs[Print non-sensitive outputs]
```

## Recommended layout when development infrastructure is added

```text
infra/
|-- bootstrap/
|   |-- main.tf
|   |-- variables.tf
|   |-- outputs.tf
|   |-- providers.tf
|   `-- versions.tf
|-- modules/
|   |-- network/
|   |-- data-services/
|   |-- application/
|   `-- monitoring/
`-- environments/
    |-- dev/
    |   |-- backend.tf
    |   |-- main.tf
    |   |-- variables.tf
    |   |-- outputs.tf
    |   `-- terraform.tfvars.example
    `-- prod/
        |-- backend.tf
        |-- main.tf
        |-- variables.tf
        |-- outputs.tf
        `-- terraform.tfvars.example

.github/workflows/
|-- dev-infrastructure.yml
|-- dev-backend.yml
|-- dev-frontend.yml
|-- prod-infrastructure.yml
|-- prod-backend.yml
`-- prod-frontend.yml
```

Use a separate state key per root module and environment, for example
`swiftpay/bootstrap.tfstate`, `swiftpay/dev.tfstate`, and
`swiftpay/prod.tfstate`. Prefer reusable modules for shared resource patterns,
but keep separate environment root modules, credentials, GitHub environments,
resource groups, state keys, approvals, sizing, and safety policy. Do not make
`dev` and `prod` Terraform workspaces over one loosely parameterized root unless
the team deliberately accepts the weaker isolation and larger blast radius.
