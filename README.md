# Task Management API

Task management REST API built with Java 25 and Spring Boot 3.5. Tasks are
stored in MongoDB; users and Flyway migrations use PostgreSQL. Spring Security
protects task endpoints with JWT.

## Run

Docker is the only requirement:

```powershell
docker compose up -d --build
docker compose logs -f api
```

The API is available at `http://localhost:8080`. Stop it with:

```powershell
docker compose down
```

## Completed phases

- Phase 1: MongoDB CRUD and HTTP status handling
- Phase 2: DTOs, validation, global errors and unit tests
- Phase 3: status/priority filters, pagination, sorting and timestamps
- Phase 4: PostgreSQL users, Flyway, JWT authentication and task ownership

Phase 5 is not implemented.

## API

Authentication:

```text
POST /api/auth/register
POST /api/auth/login
```

Tasks:

```text
POST   /api/tasks
GET    /api/tasks
GET    /api/tasks/{id}
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
```

Register, log in and create a task:

```powershell
$credentials = '{"email":"alice@example.com","password":"password123"}'

$credentials | curl.exe -s `
  -X POST "http://localhost:8080/api/auth/register" `
  -H "Content-Type: application/json" --data-binary "@-"

$token = ($credentials | curl.exe -s `
  -X POST "http://localhost:8080/api/auth/login" `
  -H "Content-Type: application/json" --data-binary "@-" |
  ConvertFrom-Json).accessToken

$task = '{"title":"Learn Spring Boot","description":"Build the API","status":"TODO","priority":"HIGH"}'

$task | curl.exe -s -X POST "http://localhost:8080/api/tasks" `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" --data-binary "@-"
```

Filter, paginate and sort:

```powershell
curl.exe -H "Authorization: Bearer $token" `
  "http://localhost:8080/api/tasks?status=TODO&priority=HIGH&page=0&size=10&sort=createdAt,desc"
```

Every task is scoped to the authenticated user.

## Test data

The `seed` profile creates `demo@example.com / password123` and 12 owned tasks:

```powershell
$env:SPRING_PROFILES_ACTIVE = "seed"
docker compose up -d --build --force-recreate api
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

## Tests

```powershell
.\mvnw.cmd clean test
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

## Trade-offs

MongoDB tasks reference the PostgreSQL user UUID through `userId`; referential
integrity is enforced by the application. List endpoints apply page and sort
parameters but return a plain JSON list without total-count metadata.

Starting with the phase 5 architecture would produce cleaner service boundaries,
but two applications, Kafka and event contracts would add enough setup and
operational work to risk missing the one-day deadline. I prioritized a
complete and runnable implementation of phases 1-4.

Credentials in Compose are local development defaults kept for reproducible
review. Production deployments should inject them externally.
