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

try {
    Write-Host "Checking that the API is reachable..."
    Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks?page=0&size=1" `
        -ExpectedStatus 200 | Out-Null

    $requests = @(
        @{
            title       = "Smoke TODO $suffix"
            description = "Phase 1-3 smoke test"
            status      = "TODO"
            priority    = "HIGH"
        },
        @{
            title       = "Smoke progress $suffix"
            description = "Phase 1-3 smoke test"
            status      = "IN_PROGRESS"
            priority    = "LOW"
        },
        @{
            title       = "Smoke done $suffix"
            description = "Phase 1-3 smoke test"
            status      = "DONE"
            priority    = "MEDIUM"
        }
    )

    $createdTasks = foreach ($request in $requests) {
        $response = Invoke-CurlRequest `
            -Method "POST" `
            -Url "$BaseUrl/api/tasks" `
            -Body $request `
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
        -ExpectedStatus 200

    Assert-True `
        -Condition ($single.Body.title -eq "Smoke TODO $suffix") `
        -Message "GET by id returned the wrong task"

    $page = Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks?page=0&size=2&sort=createdAt,desc" `
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
        -ExpectedStatus 200

    Assert-True `
        -Condition (@($statusFilter.Body | Where-Object {
                    $_.status -ne "TODO"
                }).Count -eq 0) `
        -Message "Status filter returned a non-TODO task"

    $priorityFilter = Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks?priority=HIGH&page=0&size=100" `
        -ExpectedStatus 200

    Assert-True `
        -Condition (@($priorityFilter.Body | Where-Object {
                    $_.priority -ne "HIGH"
                }).Count -eq 0) `
        -Message "Priority filter returned a non-HIGH task"

    $combinedFilter = Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks?status=TODO&priority=HIGH&page=0&size=100" `
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
        -ExpectedStatus 400 | Out-Null

    Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks?priority=CRITICAL" `
        -ExpectedStatus 400 | Out-Null

    Write-Host "Validation and error handling: OK"

    Invoke-CurlRequest `
        -Method "DELETE" `
        -Url "$BaseUrl/api/tasks/$taskId" `
        -ExpectedStatus 204 | Out-Null

    Invoke-CurlRequest `
        -Method "GET" `
        -Url "$BaseUrl/api/tasks/$taskId" `
        -ExpectedStatus 404 | Out-Null

    Write-Host "Delete and not-found handling: OK"
    Write-Host "All Phase 1-3 smoke checks passed." -ForegroundColor Green
} finally {
    foreach ($id in $createdIds) {
        try {
            Invoke-CurlRequest `
                -Method "DELETE" `
                -Url "$BaseUrl/api/tasks/$id" `
                -ExpectedStatus @(204, 404) | Out-Null
        } catch {
            Write-Warning "Could not clean up smoke task $id"
        }
    }
}
