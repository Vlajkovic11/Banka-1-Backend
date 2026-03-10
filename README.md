# Banka-1-Backend

## Getting Started

### Prerequisites

- **Java 21**
- **Node.js** (for OpenAPI validation) — install `redocly`:
  ```bash
  npm install -g @redocly/cli
  ```
- **.NET SDK** (required if any .NET microservices are present)
- **Docker** (for Docker build validation, required once services are containerized)

### Setup after cloning

Run the setup script once to activate Git hooks:

```bash
./setup-hooks.sh
```

This configures Git to use the hooks in `.github/githooks/`.

> **This step is required.** Without it, pre-push checks will not run.

---

## Pre-push Hooks

Every `git push` automatically runs three checks. The push is aborted if any check fails.

### 1. Unit and Integration Tests

Runs all tests across all services. Test commands are auto-detected by build system:

| Build system | Detection | Command |
|---|---|---|
| Gradle (root multi-project) | `gradlew` + `settings.gradle` | `./gradlew test` |
| Gradle (per-service) | `gradlew` | `./gradlew test` |
| Maven | `pom.xml` | `./mvnw verify` |
| Node.js | `package.json` | `npm ci && npm test` |
| Go | `go.mod` | `go test ./...` |
| Python | `requirements.txt` / `pyproject.toml` | `python -m pytest` |
| .NET | `*.sln` / `*.csproj` | `dotnet test` |

Unit and integration tests are both placed in `src/test/java` (for JVM services).

### 2. OpenAPI / Swagger Validation

Validates all OpenAPI spec files found in service directories at these paths:

```
<service>/docs/openapi.yml
<service>/docs/openapi.yaml
<service>/src/main/resources/openapi.yml
<service>/src/main/resources/openapi.yaml
```

Requires `redocly` to be installed (see Prerequisites). Empty spec files are skipped with a warning.

### 3. Docker Build Validation

Runs `docker compose build --no-cache` from the repo root.

Requires:
- Docker installed and running
- `docker-compose.yml` present at the repo root

If either is missing, this step is skipped with a warning.

---

## Adding a New Microservice

1. Create a directory at the repo root (e.g. `user-service/`).
2. Add a `Dockerfile` inside it.
3. Add the service to `docker-compose.yml` at the repo root.
4. Place tests in the standard location for your build system.
5. Add an OpenAPI spec at `<service>/docs/openapi.yml` if the service exposes an HTTP API.

The pre-push hook will automatically pick up the new service.
