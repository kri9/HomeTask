param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Invoke-CurlRequest {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Method,

        [Parameter(Mandatory = $true)]
        [string]$Url,

        [object]$Body,

        [hashtable]$Headers = @{},

        [Parameter(Mandatory = $true)]
        [int[]]$ExpectedStatus
    )

    $responseFile = [System.IO.Path]::GetTempFileName()
    $requestFile = $null

    try {
        $arguments = @(
            "--silent",
            "--show-error",
            "--output", $responseFile,
            "--write-out", "%{http_code}",
            "--request", $Method,
            $Url
        )

        foreach ($header in $Headers.GetEnumerator()) {
            $arguments += @(
                "--header",
                "$($header.Key): $($header.Value)"
            )
        }

        if ($null -ne $Body) {
            $requestFile = [System.IO.Path]::GetTempFileName()
            $json = $Body | ConvertTo-Json -Depth 5 -Compress
            $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
            [System.IO.File]::WriteAllText(
                    $requestFile,
                    $json,
                    $utf8WithoutBom
            )

            $arguments += @(
                "--header", "Content-Type: application/json",
                "--data-binary", "@$requestFile"
            )
        }

        $statusText = & curl.exe @arguments

        if ($LASTEXITCODE -ne 0) {
            throw "curl failed with exit code $LASTEXITCODE"
        }

        $status = [int]$statusText
        $content = Get-Content -Raw $responseFile

        if ($ExpectedStatus -notcontains $status) {
            throw "Expected HTTP $ExpectedStatus, received $status. Body: $content"
        }

        $parsedBody = if ([string]::IsNullOrWhiteSpace($content)) {
            $null
        } else {
            $content | ConvertFrom-Json
        }

        return [PSCustomObject]@{
            StatusCode = $status
            Body       = $parsedBody
        }
    } finally {
        Remove-Item -Force -ErrorAction SilentlyContinue $responseFile

        if ($null -ne $requestFile) {
            Remove-Item -Force -ErrorAction SilentlyContinue $requestFile
        }
    }
}

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$createdIds = [System.Collections.Generic.List[string]]::new()
$ownerHeaders = $null

try {
    Write-Host "Checking authentication..."

    Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks" `
        -ExpectedStatus 401 | Out-Null

    $ownerCredentials = @{
        email    = "phase4-smoke-owner@example.com"
        password = "smoke-password-123"
    }

    $otherCredentials = @{
        email    = "phase4-smoke-other@example.com"
        password = "smoke-password-123"
    }

    Invoke-CurlRequest `
        -Method "POST" `
        -Url "$BaseUrl/api/auth/register" `
        -Body $ownerCredentials `
        -ExpectedStatus @(201, 409) | Out-Null

    Invoke-CurlRequest `
        -Method "POST" `
        -Url "$BaseUrl/api/auth/register" `
        -Body $otherCredentials `
        -ExpectedStatus @(201, 409) | Out-Null

    $ownerLogin = Invoke-CurlRequest `
        -Method "POST" `
        -Url "$BaseUrl/api/auth/login" `
        -Body $ownerCredentials `
        -ExpectedStatus 200

    $otherLogin = Invoke-CurlRequest `
        -Method "POST" `
        -Url "$BaseUrl/api/auth/login" `
        -Body $otherCredentials `
        -ExpectedStatus 200

    $ownerHeaders = @{
        Authorization = "Bearer $($ownerLogin.Body.accessToken)"
    }

    $otherHeaders = @{
        Authorization = "Bearer $($otherLogin.Body.accessToken)"
    }

    Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks?page=0&size=1" `
        -Headers $ownerHeaders `
        -ExpectedStatus 200 | Out-Null

    Write-Host "Authentication: OK"

    $requests = @(
        @{
            title       = "Smoke TODO $suffix"
            description = "Phase 1-4 smoke test"
            status      = "TODO"
            priority    = "HIGH"
        },
        @{
            title       = "Smoke progress $suffix"
            description = "Phase 1-4 smoke test"
            status      = "IN_PROGRESS"
            priority    = "LOW"
        },
        @{
            title       = "Smoke done $suffix"
            description = "Phase 1-4 smoke test"
            status      = "DONE"
            priority    = "MEDIUM"
        }
    )

    $createdTasks = foreach ($request in $requests) {
        $response = Invoke-CurlRequest `
            -Method "POST" `
            -Url "$BaseUrl/api/tasks" `
            -Body $request `
            -Headers $ownerHeaders `
            -ExpectedStatus 201

        $createdIds.Add([string]$response.Body.id)
        Start-Sleep -Milliseconds 25
        $response.Body
    }

    Write-Host "CRUD creation: OK"

    $taskId = [string]$createdTasks[0].id
    $single = Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks/$taskId" `
        -Headers $ownerHeaders `
        -ExpectedStatus 200

    Assert-True `
        -Condition ($single.Body.title -eq "Smoke TODO $suffix") `
        -Message "GET by id returned the wrong task"

    Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks/$taskId" `
        -Headers $otherHeaders `
        -ExpectedStatus 404 | Out-Null

    $otherTasks = Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks?page=0&size=100" `
        -Headers $otherHeaders `
        -ExpectedStatus 200

    $leakedTasks = @($otherTasks.Body | Where-Object {
            $createdIds.Contains([string]$_.id)
        })

    Assert-True `
        -Condition ($leakedTasks.Count -eq 0) `
        -Message "Another user can see the owner's tasks"

    Write-Host "User isolation: OK"

    $page = Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks?page=0&size=2&sort=createdAt,desc" `
        -Headers $ownerHeaders `
        -ExpectedStatus 200

    $pageItems = @($page.Body)
    Assert-True `
        -Condition ($pageItems.Count -le 2) `
        -Message "Pagination size was not applied"

    if ($pageItems.Count -eq 2) {
        $firstTimestamp = [DateTimeOffset]$pageItems[0].createdAt
        $secondTimestamp = [DateTimeOffset]$pageItems[1].createdAt

        Assert-True `
            -Condition ($firstTimestamp -ge $secondTimestamp) `
            -Message "Descending createdAt sorting was not applied"
    }

    $statusFilter = Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks?status=TODO&page=0&size=100" `
        -Headers $ownerHeaders `
        -ExpectedStatus 200

    Assert-True `
        -Condition (@($statusFilter.Body | Where-Object {
                    $_.status -ne "TODO"
                }).Count -eq 0) `
        -Message "Status filter returned a non-TODO task"

    $priorityFilter = Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks?priority=HIGH&page=0&size=100" `
        -Headers $ownerHeaders `
        -ExpectedStatus 200

    Assert-True `
        -Condition (@($priorityFilter.Body | Where-Object {
                    $_.priority -ne "HIGH"
                }).Count -eq 0) `
        -Message "Priority filter returned a non-HIGH task"

    $combinedFilter = Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks?status=TODO&priority=HIGH&page=0&size=100" `
        -Headers $ownerHeaders `
        -ExpectedStatus 200

    Assert-True `
        -Condition (@($combinedFilter.Body | Where-Object {
                    $_.status -ne "TODO" -or $_.priority -ne "HIGH"
                }).Count -eq 0) `
        -Message "Combined filters returned an invalid task"

    Write-Host "Filtering, pagination and sorting: OK"

    $createdAt = [DateTimeOffset]$single.Body.createdAt
    Start-Sleep -Milliseconds 25

    $updated = Invoke-CurlRequest `
        -Method "PUT" `
        -Url "$BaseUrl/api/tasks/$taskId" `
        -Body @{
            title       = "Smoke updated $suffix"
            description = "Updated by smoke test"
            status      = "DONE"
            priority    = "MEDIUM"
        } `
        -Headers $ownerHeaders `
        -ExpectedStatus 200

    Assert-True `
        -Condition ($updated.Body.status -eq "DONE") `
        -Message "PUT did not update status"
    Assert-True `
        -Condition ([DateTimeOffset]$updated.Body.updatedAt -ge $createdAt) `
        -Message "updatedAt was not maintained"

    Invoke-CurlRequest `
        -Method "POST" `
        -Url "$BaseUrl/api/tasks" `
        -Body @{
            title       = ""
            description = "Invalid task"
            status      = "TODO"
            priority    = "HIGH"
        } `
        -Headers $ownerHeaders `
        -ExpectedStatus 400 | Out-Null

    Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks?priority=CRITICAL" `
        -Headers $ownerHeaders `
        -ExpectedStatus 400 | Out-Null

    Write-Host "Validation and error handling: OK"

    Invoke-CurlRequest `
        -Method "DELETE" `
        -Url "$BaseUrl/api/tasks/$taskId" `
        -Headers $ownerHeaders `
        -ExpectedStatus 204 | Out-Null

    Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks/$taskId" `
        -Headers $ownerHeaders `
        -ExpectedStatus 404 | Out-Null

    Write-Host "Delete and not-found handling: OK"
    Write-Host "All Phase 1-4 smoke checks passed." -ForegroundColor Green
} finally {
    if ($null -ne $ownerHeaders) {
        foreach ($id in $createdIds) {
            try {
                Invoke-CurlRequest `
                    -Method "DELETE" `
                    -Url "$BaseUrl/api/tasks/$id" `
                    -Headers $ownerHeaders `
                    -ExpectedStatus @(204, 404) | Out-Null
            } catch {
                Write-Warning "Could not clean up smoke task $id"
            }
        }
    }
}
