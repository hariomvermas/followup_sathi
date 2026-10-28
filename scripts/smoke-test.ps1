# Live smoke test for the FollowUp Saathi backend (Stage 2 gap-fill endpoints).
# Usage: powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
# Requires the backend running on localhost:8080 (mvn spring-boot:run).
$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"
$stamp = Get-Date -Format "yyyyMMddHHmmss"
$email = "smoke$stamp@example.com"
$password = "Password123"
$script:s = $null
$script:token = $null

function api($method, $uri, $body) {
    $headers = @{ "X-XSRF-TOKEN" = $script:token }
    if ($body) {
        Invoke-RestMethod -Method $method -Uri "$base$uri" -WebSession $script:s `
            -Headers $headers -ContentType "application/json" `
            -Body ($body | ConvertTo-Json -Depth 5)
    } else {
        Invoke-RestMethod -Method $method -Uri "$base$uri" -WebSession $script:s `
            -Headers $headers
    }
}

function step($name) { Write-Host "`n== $name" -ForegroundColor Cyan }

# --- 0. Unauthenticated API returns 401 --------------------------------------
step "401 without session"
try {
    Invoke-RestMethod -Uri "$base/api/dashboard" -ErrorAction Stop | Out-Null
    Write-Host "FAIL: expected 401"
} catch {
    Write-Host ("status: " + $_.Exception.Response.StatusCode.value__)
}

# --- 1. CSRF bootstrap -------------------------------------------------------
step "GET /api/auth/csrf"
$csrf = Invoke-RestMethod -Uri "$base/api/auth/csrf" -SessionVariable s
$script:s = $s
$script:token = $csrf.token
Write-Host ("token present: " + ($script:token.Length -gt 0))

# --- 2. Register + onboarding ------------------------------------------------
step "register $email"
api POST "/api/auth/register" @{
    name = "Smoke Test"; email = $email; mobile = "9876500000"
    password = $password; confirmPassword = $password
} | Out-Null
Write-Host "registered"

step "login (register does not create a session by design)"
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
    -ContentType "application/json" `
    -Body (@{ email = $email; password = $password } | ConvertTo-Json) `
    -SessionVariable webs
$script:s = $webs
$script:token = (Invoke-RestMethod -Uri "$base/api/auth/csrf" `
    -WebSession $script:s).token
Write-Host ("login ok: " + $login.email)

step "onboarding (PUT /api/business)"
api PUT "/api/business" @{
    businessName = "Smoke Salon"; businessCategory = "Salon"
    timeZone = "Asia/Kolkata"; preferredLanguage = "en"
    workingDays = @(1, 2, 3, 4, 5, 6); reminderEnabled = $true
    reminderTime = "09:00"
} | Out-Null
Write-Host "business saved"

# --- 3. Enquiries + follow-ups ------------------------------------------------
step "create enquiries"
$yesterday = (Get-Date).AddDays(-1).ToString("yyyy-MM-dd")
$today = (Get-Date).ToString("yyyy-MM-dd")
$enq1 = api POST "/api/enquiries" @{
    customerName = "Ramesh Kumar"; phoneNumber = "9812345678"
    serviceName = "Haircut + Beard"; source = "PHONE_CALL"
    priority = "HIGH"; notes = "Smoke test enquiry"
}
$enq2 = api POST "/api/enquiries" @{
    customerName = "Suresh Verma"; phoneNumber = "9898989898"
    serviceName = "Hair color"; source = "WALK_IN"; priority = "MEDIUM"
}
Write-Host ("enquiries created: {0}, {1}" -f $enq1.id, $enq2.id)

step "schedule follow-ups (yesterday -> overdue, today -> today)"
$fu1 = api POST "/api/enquiries/$($enq1.id)/follow-ups" @{ date = $yesterday; reason = "Call Ramesh" }
$fu2 = api POST "/api/enquiries/$($enq2.id)/follow-ups" @{ date = $today; reason = "Remind Suresh" }
Write-Host ("follow-ups created: {0}, {1}" -f $fu1.id, $fu2.id)

step "enquiry-scoped follow-up list"
$perEnquiry = api GET "/api/enquiries/$($enq1.id)/follow-ups"
Write-Host ("rows: " + $perEnquiry.Count)

# --- 4. Global follow-up buckets ---------------------------------------------
step "GET /api/follow-ups (ALL)"
$all = api GET "/api/follow-ups"
Write-Host ("today={0} upcoming={1} overdue={2} items={3}" -f `
    $all.todayCount, $all.upcomingCount, $all.overdueCount, $all.items.Count)

step "bucket=TODAY"
$todayBucket = api GET "/api/follow-ups?bucket=TODAY"
Write-Host ("items: " + $todayBucket.items.Count)

step "bucket=OVERDUE"
$overdueBucket = api GET "/api/follow-ups?bucket=OVERDUE"
$first = $overdueBucket.items | Select-Object -First 1
Write-Host ("items: {0}; first: {1} overdue={2}" -f `
    $overdueBucket.items.Count, $first.customerName, $first.overdue)

# --- 5. Complete a follow-up, check enquiry status -----------------------------
step "complete follow-up $(-join $fu2.id) with outcome"
api PATCH "/api/enquiries/$($enq2.id)/follow-ups/$($fu2.id)/complete" `
    @{ outcomeType = "INTERESTED"; notes = "Wants to book next week" } | Out-Null
Write-Host "completed"

step "reports summary after conversion"
$summary = api GET "/api/reports/summary"
$summary | ConvertTo-Json -Depth 5

step "reports CSV"
try {
    # Invoke-WebRequest chokes on the attachment Content-Disposition header in
    # Windows PowerShell 5.1, so read the body with Invoke-RestMethod instead.
    $csv = Invoke-RestMethod -Uri "$base/api/reports/summary?format=csv" -WebSession $script:s `
        -Headers @{ "X-XSRF-TOKEN" = $script:token } -Method GET
    $firstLine = ($csv -split "`r`n")[0]
    Write-Host ("csv ok; first line: " + $firstLine)
} catch {
    Write-Host "CSV failed: $_"
}

# --- 6. Account: password change ----------------------------------------------
step "change password"
api POST "/api/account/password" @{
    currentPassword = $password; newPassword = "Password456"
} | Out-Null
Write-Host "password changed"

step "login with the NEW password (old one must fail)"
try {
    Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ErrorAction Stop `
        -ContentType "application/json" `
        -Body (@{ email = $email; password = $password } | ConvertTo-Json) | Out-Null
    Write-Host "FAIL: old password still accepted"
} catch {
    Write-Host ("old password rejected with status: " + $_.Exception.Response.StatusCode.value__)
}
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
    -ContentType "application/json" `
    -Body (@{ email = $email; password = "Password456" } | ConvertTo-Json) `
    -SessionVariable s
$script:s = $s
$script:token = (Invoke-RestMethod -Uri "$base/api/auth/csrf" -WebSession $script:s).token
Write-Host ("login ok: " + $login.email)

# --- 7. Account: deletion ------------------------------------------------------
step "delete account (wrong confirm text must fail)"
try {
    api DELETE "/api/account/me" -ErrorAction Stop `
        @{ password = "Password456"; confirmText = "delete" } | Out-Null
    Write-Host "FAIL: wrong confirm text accepted"
} catch {
    Write-Host ("rejected with status: " + $_.Exception.Response.StatusCode.value__)
}
step "delete account (correct)"
api DELETE "/api/account/me" @{ password = "Password456"; confirmText = "DELETE" } | Out-Null
Write-Host "deleted"

step "session is gone after deletion"
try {
    api GET "/api/auth/me" -ErrorAction Stop | Out-Null
    Write-Host "FAIL: session still valid"
} catch {
    Write-Host ("status: " + $_.Exception.Response.StatusCode.value__)
}

Write-Host "`nSmoke test finished." -ForegroundColor Green
