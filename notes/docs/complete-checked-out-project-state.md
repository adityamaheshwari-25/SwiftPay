# Complete checked-out project state in GitHub Actions

## Short definition

A complete checked-out project state is the set of tracked repository files that exist at one selected Git commit or Git reference after `actions/checkout` places them in a workflow runner's working directory.

The workflow receives complete files from that revision. It does not receive only the lines displayed as changed in a pull request.

## Git commit as a snapshot

A Git commit represents a snapshot of all tracked files at a particular point in repository history.

Suppose `main` contains:

```text
A
B
C
```

A feature branch modifies only `B`. The pull request diff primarily displays the change to `B`, but the checked-out project state contains:

```text
A
proposed version of B
C
```

The runner needs all three files because the change to `B` could break something in `A` or `C`.

## Pull request diff versus checked-out state

These are different concepts.

### Pull request diff

The diff is a review view showing what the source branch proposes to add, modify, or remove relative to the target branch.

It helps reviewers answer:

> What is this pull request changing?

### Checked-out project state

The checked-out state is the file tree that workflow commands actually see on the runner.

It helps CI answer:

> Does the complete proposed project work after these changes are integrated?

Therefore, CI tools do not normally execute against isolated changed lines. They execute against files in the checked-out working tree, according to each command's configured scope.

## What happens for a pull request

For a normal `pull_request` workflow, GitHub usually creates or selects a test/merge commit under a reference similar to:

```text
refs/pull/<pull-request-number>/merge
```

This synthetic merge commit represents:

```text
current target branch + proposed pull request changes
```

For a pull request targeting `main`, it allows CI to test whether the proposed changes work when combined with the current `main` branch.

For a `pull_request` event, `github.sha` normally identifies this PR test/merge commit, rather than necessarily identifying the source branch's latest commit directly.

## What `actions/checkout` does

A workflow runner starts without the repository's working tree. A step such as:

```yaml
- name: Check out repository
  uses: actions/checkout@<pinned-commit>
```

places the selected repository revision into the runner's working directory.

If the workflow specifies:

```yaml
with:
  ref: ${{ github.sha }}
```

it explicitly requests the revision identified by the event's `github.sha` value.

The following jobs can then use the same exact revision consistently:

- validation;
- build;
- test;
- plan; and
- deployment preparation.

## Path filters do not limit the checked-out files

A path filter such as:

```yaml
pull_request:
  branches:
    - main
  paths:
    - "PaytmCloneFrontend/**"
    - ".github/workflows/prod-frontend.yml"
```

answers only:

> Should this workflow start for this pull request?

If a matching path changed, the workflow starts. The path filter does not instruct `actions/checkout` to download only those changed lines or files.

The responsibilities are separate:

- `branches` checks the pull request's target branch.
- `paths` checks whether relevant paths changed and decides whether to trigger the workflow.
- `actions/checkout` obtains the selected revision's tracked file tree.
- Later commands decide which directories and files they process.

## Command scope still matters

The repository can be checked out as a complete tracked-file snapshot while individual commands operate on narrower scopes.

For example:

```yaml
working-directory: PaytmCloneFrontend
run: npm run lint
```

The repository is checked out, but the command starts inside `PaytmCloneFrontend`.

Likewise:

```yaml
working-directory: PaytmCloneBackend
run: ./mvnw clean verify
```

runs Maven against the backend project.

Therefore, "complete checked-out project state" does not mean every tool always analyzes every repository file. It means the selected tracked-file snapshot is available, while each command follows its own working directory, configuration, exclusions, and arguments.

## SwiftPay frontend example

When a pull request targeting `main` changes `PaytmCloneFrontend/**`, `prod-frontend.yml` starts.

The workflow then:

1. Checks out the selected PR revision.
2. Sets up Node.js.
3. Runs `npm ci` inside `PaytmCloneFrontend`.
4. Runs `npm run lint`, which currently executes `eslint .`.
5. Validates the production API configuration.
6. Runs `npm run build`, which executes `vite build`.
7. Prepares the generated `dist` directory.

Even if the PR changes only one React component, Vite builds the complete frontend application represented by the checked-out revision. This can expose broken imports, incompatible interfaces, invalid configuration, or build failures elsewhere in the frontend.

## SwiftPay backend example

When a pull request targeting `main` changes `PaytmCloneBackend/**`, `prod-backend.yml` starts.

It checks out the selected PR revision and runs:

```text
./mvnw --batch-mode --no-transfer-progress clean verify
```

Maven processes the backend project according to `pom.xml`. It does not compile or test only the changed Java lines. A small change can therefore reveal compilation or test failures in other backend classes that depend on it.

## SwiftPay Terraform example

When a pull request targeting `main` changes `infra/**`, `prod-infrastructure.yml` starts its validation job.

The workflow checks out the selected revision and runs formatting and validation against the Terraform configurations:

```text
terraform fmt -check -recursive infra
terraform -chdir=infra/bootstrap init -backend=false -input=false
terraform -chdir=infra/bootstrap validate -no-color
terraform -chdir=infra/environments/prod init -backend=false -input=false
terraform -chdir=infra/environments/prod validate -no-color
```

Terraform therefore sees the complete relevant configuration files from the selected revision, not only the changed Terraform lines.

A changed variable, module reference, provider configuration, or resource can break references in another Terraform file, so validating the configuration as a unit is important.

## Complete file tree versus complete Git history

A complete checked-out tracked-file tree does not necessarily mean the complete Git history was downloaded.

These are separate:

- Working tree: files from the selected revision that tools compile, lint, test, or validate.
- Git history: earlier commits, branches, and tags that led to the selected revision.

`actions/checkout` can provide the selected file tree with shallow history. If a workflow needs full history, it can request:

```yaml
with:
  fetch-depth: 0
```

The SwiftPay backend and frontend workflows use `fetch-depth: 0` because release validation checks whether the release commit is an ancestor of the protected `main` branch.

Terraform formatting and configuration validation do not need full repository history, so the infrastructure workflow does not request it.

## Tracked, untracked, and generated files

The initial checkout contains tracked files from the selected revision.

It does not automatically contain:

- untracked local files that were never committed;
- ignored files from a developer's machine;
- local secrets;
- local build outputs; or
- files generated only after workflow commands run.

For example, `PaytmCloneFrontend/dist` is not supplied as original checked-out source. The workflow creates it by running `vite build`.

Similarly, a backend JAR is generated by Maven after checkout.

## Why this model is useful

Testing the complete proposed project state can detect:

- broken imports;
- compilation errors;
- incompatible method or component changes;
- failing tests;
- lint errors;
- broken Terraform references;
- configuration errors; and
- integration conflicts with the latest target branch.

Testing only changed lines would miss many of these relationships.

## Important limitation

A successful checkout, lint, build, test, or `terraform validate` does not prove that production deployment will succeed.

Some problems require live systems or a real deployment context, such as:

- cloud permissions;
- live Terraform state;
- Azure API behavior;
- quotas;
- unavailable cloud SKUs;
- runtime networking;
- external service failures; and
- production-only data conditions.

CI validates as much as its configured commands and available environment allow.

## Quick summary

```text
PR diff
  = the proposed changes reviewers see

Path filter
  = decides whether a workflow starts

Git commit/SHA
  = identifies one exact repository snapshot

actions/checkout
  = places that selected tracked-file snapshot on the runner

CI command
  = operates on its configured scope within that snapshot
```

The key idea is:

> GitHub uses the changed paths to decide whether to run CI, but CI checks the selected project snapshot rather than only the changed lines.
