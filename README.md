# Task Management API

Java 25, Spring Boot 3.5, MongoDB. Implements CRUD, validation, error
handling, filtering, pagination, sorting, priorities and audit timestamps.

## Run

Docker with Compose is enough. The API is available on port `8080`.

```powershell
docker compose up --build -d
docker compose logs -f api
```

```powershell
docker compose down
```

## Test data

```powershell
$env:SPRING_PROFILES_ACTIVE = "seed"
docker compose up --build -d
Remove-Item Env:SPRING_PROFILES_ACTIVE
```

The `seed` profile upserts 12 tasks. Use `docker compose down -v` to reset
the database.

## API

`POST /api/tasks`

`GET /api/tasks`

`GET /api/tasks/{id}`

`PUT /api/tasks/{id}`

`DELETE /api/tasks/{id}`

```powershell
curl.exe "http://localhost:8080/api/tasks?status=TODO&priority=HIGH&page=0&size=10&sort=createdAt,desc"
```

## Tests

```powershell
.\mvnw.cmd clean test
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```
