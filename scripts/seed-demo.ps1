# Loads obviously fake demo data through the real API, so every business rule applies
# (joining fee, receipts, consent, once-a-day attendance).
#
# For a LOCAL demo database only. The demo members share one well-known password, so never run
# this against a database with real members.
#
# Usage (from the project root):
#   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\seed-demo.ps1
#
# Safe to run twice: existing demo members are reused and nobody is sold a second plan.

param(
  [string]$BaseUrl = 'http://localhost',
  [string]$AdminEmail
)

$ErrorActionPreference = 'Stop'

if ($BaseUrl -notmatch '^https?://(localhost|127\.0\.0\.1)') {
  $answer = Read-Host "$BaseUrl is not a local address. Type YES to continue"
  if ($answer -ne 'YES') { return }
}

if (-not $AdminEmail) { $AdminEmail = Read-Host 'Admin email' }
$secure = Read-Host 'Admin password' -AsSecureString
$AdminPassword = [System.Net.NetworkCredential]::new('', $secure).Password

# The door device key lives in .env.prod (one folder up from scripts/)
$deviceKey = $null
$envFile = Join-Path $PSScriptRoot '..\.env.prod'
if (Test-Path $envFile) {
  $line = Get-Content $envFile | Where-Object { $_ -like 'DEVICE_API_KEY=*' } | Select-Object -First 1
  if ($line) { $deviceKey = $line.Substring('DEVICE_API_KEY='.Length) }
}
if (-not $deviceKey) { Write-Warning 'DEVICE_API_KEY not found in .env.prod: door scans will be skipped.' }

function Call($Method, $Path, $Body = $null, $Token = $null, $Headers = @{}) {
  $h = @{} + $Headers
  if ($Token) { $h['Authorization'] = "Bearer $Token" }
  $req = @{ Method = $Method; Uri = "$BaseUrl/api/v1$Path"; Headers = $h }
  if ($null -ne $Body) {
    $req['ContentType'] = 'application/json'
    $req['Body'] = ($Body | ConvertTo-Json -Depth 5)
  }
  Invoke-RestMethod @req
}

$DemoPassword = 'Demo#Pass12345'

$login = Call POST '/auth/login' @{ email = $AdminEmail; password = $AdminPassword }
$adminToken = $login.accessToken
$plans = @(Call GET '/membership-plans' -Token $adminToken)
if ($plans.Count -eq 0) { throw 'No membership plans found.' }
Write-Host "Found $($plans.Count) plans."

# Door = the device number to link (0 = no fingerprint). Scans = how many times they go through the door today.
$demo = @(
  @{ Name = 'Demo Member 1'; Phone = '+91 90000 00001'; Method = 'CASH'; Sell = $true;  Door = 101; Scans = 3; Freeze = $false },
  @{ Name = 'Demo Member 2'; Phone = '+91 90000 00002'; Method = 'UPI';  Sell = $true;  Door = 102; Scans = 1; Freeze = $false },
  @{ Name = 'Demo Member 3'; Phone = '+91 90000 00003'; Method = 'CASH'; Sell = $true;  Door = 103; Scans = 2; Freeze = $false },
  @{ Name = 'Demo Member 4'; Phone = '+91 90000 00004'; Method = 'UPI';  Sell = $true;  Door = 104; Scans = 1; Freeze = $true  },
  @{ Name = 'Demo Member 5'; Phone = '+91 90000 00005'; Method = 'CASH'; Sell = $true;  Door = 0;   Scans = 0; Freeze = $false },
  @{ Name = 'Demo Member 6'; Phone = '+91 90000 00006'; Method = 'UPI';  Sell = $true;  Door = 0;   Scans = 0; Freeze = $false },
  @{ Name = 'Demo Member 7'; Phone = '+91 90000 00007'; Method = 'CASH'; Sell = $false; Door = 107; Scans = 1; Freeze = $false },
  @{ Name = 'Demo Member 8'; Phone = '+91 90000 00008'; Method = 'CASH'; Sell = $true;  Door = 0;   Scans = 0; Freeze = $false }
)

$base = (Get-Date).ToUniversalTime().AddHours(-3)
$i = 0
foreach ($m in $demo) {
  $i++
  $email = "demo.member$i@example.com"

  try {
    $reg = Call POST '/auth/register' @{
      email = $email; password = $DemoPassword; fullName = $m.Name; phone = $m.Phone
      acceptTerms = $true; marketingConsent = $false
      aiProcessingConsent = ($i % 2 -eq 1); biometricConsent = ($m.Door -gt 0)
    }
    $token = $reg.accessToken
  } catch {
    $reg = Call POST '/auth/login' @{ email = $email; password = $DemoPassword }
    $token = $reg.accessToken
  }
  $id = (Call GET '/auth/me' -Token $token).id

  # Sell a plan only if the member has none yet
  $subs = @(Call GET "/members/$id/subscriptions" -Token $adminToken)
  if ($m.Sell -and $subs.Count -eq 0) {
    $plan = $plans[($i - 1) % $plans.Count]
    $sale = Call POST "/members/$id/subscriptions" @{ planId = $plan.id; method = $m.Method } -Token $adminToken
    if ($m.Freeze) { Call POST "/subscriptions/$($sale.id)/freeze" @{ days = 5 } -Token $token | Out-Null }
  }

  # Link the fingerprint number (needs the member's door consent, given at registration)
  if ($m.Door -gt 0) {
    try {
      Call PUT "/members/$id/device-user" @{ deviceUserId = $m.Door } -Token $adminToken | Out-Null
    } catch {
      Write-Warning "$($m.Name): could not link device number $($m.Door). $($_.Exception.Message)"
    }
  }

  # Door scans today, spaced out so they are all distinct events
  if ($deviceKey -and $m.Door -gt 0) {
    for ($k = 1; $k -le $m.Scans; $k++) {
      $when = $base.AddMinutes(($i * 7) + ($k * 25)).ToString('o')
      Call POST '/device/events' @{ deviceSerial = 'DEMO-1'; deviceUserId = $m.Door; scannedAt = $when } `
        -Headers @{ 'X-Device-Key' = $deviceKey } | Out-Null
    }
  }
  Write-Host "Done: $($m.Name)"
}

# One fingerprint the system does not know, to show a refused scan in the door log
if ($deviceKey) {
  Call POST '/device/events' @{ deviceSerial = 'DEMO-1'; deviceUserId = 999; scannedAt = $base.AddMinutes(200).ToString('o') } `
    -Headers @{ 'X-Device-Key' = $deviceKey } | Out-Null
}

Write-Host ''
Write-Host 'Demo data loaded. Demo members log in as demo.member1@example.com ... demo.member8@example.com'
Write-Host "with the password $DemoPassword"
