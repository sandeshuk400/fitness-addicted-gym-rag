# API reference (Phase 1)

Base path: `/api/v1`. Interactive docs: `http://localhost:8080/swagger-ui.html`
(use the **Authorize** button and paste the access token).

Errors use RFC 7807 problem responses (`application/problem+json`). Validation errors include an
`errors` map of field to message.

## Authentication

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/auth/register` | public | Create a MEMBER account (profile + consent), returns tokens |
| POST | `/auth/login` | public | Returns access + refresh token |
| POST | `/auth/refresh` | public | Rotates the refresh token, returns new tokens |
| POST | `/auth/logout` | public | Revokes the given refresh token |
| GET | `/auth/me` | any signed-in user | Current account |
| POST | `/auth/change-password` | any signed-in user | Changes password, revokes all refresh tokens |

Access tokens are HS256 JWTs valid for 15 minutes (`roles` claim drives authorization).
Refresh tokens are opaque, valid for 7 days, stored only as a SHA-256 hash, and rotated on every use.
Reusing an old refresh token revokes all of that user's refresh tokens.

## Members

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/members/me` | MEMBER | Own profile including health notes |
| PUT | `/members/me` | MEMBER | Update own profile |
| GET | `/members/me/consents` | MEMBER | Latest consent decision per purpose |
| PUT | `/members/me/consents/{purpose}` | MEMBER | Grant or withdraw (`HEALTH_DATA`, `MARKETING`, `AI_PROCESSING`) |
| GET | `/members?search=&page=&size=` | STAFF, ADMIN | Search by name or phone, never returns health notes |
| GET | `/members/{id}` | STAFF, ADMIN | One member; health notes only for ADMIN, and each view is audited |

## Membership plans

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/membership-plans` | any signed-in user | Plans currently on sale, cheapest first |
| GET | `/membership-plans/all` | STAFF, ADMIN | Every plan, including withdrawn ones |
| POST | `/membership-plans` | ADMIN | Create a plan (`name`, `durationDays`, `price`, optional `description`, `active`) |
| PUT | `/membership-plans/{id}` | ADMIN | Edit a plan; `"active": false` withdraws it from sale |

Editing or withdrawing a plan never changes memberships that were already sold.

## Subscriptions

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/members/me/subscriptions` | MEMBER | Own memberships, newest first |
| GET | `/members/{memberId}/subscriptions` | STAFF, ADMIN | A member's memberships |
| POST | `/members/{memberId}/subscriptions` | STAFF, ADMIN | Sell a plan (`{"planId": 1}`) |
| GET | `/subscriptions/expiring?days=7` | STAFF, ADMIN | ACTIVE memberships ending within N days (max 90) |
| POST | `/subscriptions/{id}/freeze` | MEMBER (own), STAFF, ADMIN | Freeze for `days` (`{"days": 10}`) |
| POST | `/subscriptions/{id}/unfreeze` | MEMBER (own), STAFF, ADMIN | End a freeze early |
| POST | `/subscriptions/{id}/cancel` | STAFF, ADMIN | Cancel a membership |

Rules:

- A purchase starts today, or the day after the member's latest running or queued membership ends
  (`PENDING` until then). Dates are inclusive: a 30-day plan bought today ends in 29 days.
- Freezing moves the end date out by the same number of days, so no paid time is lost, and shifts queued
  memberships by the same amount. At most 30 freeze days per subscription (`app.membership.max-freeze-days`).
  Unfreezing early gives the unused days back.
- Status changes (`PENDING` to `ACTIVE`, finished freezes, `EXPIRED`) are applied by a job that runs at
  00:05 Asia/Kolkata and once at startup. Gym "today" always uses Asia/Kolkata, not the server's zone.
- A member asking for someone else's subscription gets `404`, not `403`, so ids cannot be probed.

## Attendance

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/attendance/check-in` | STAFF, ADMIN | `{"memberId": 5}`; needs an ACTIVE membership covering today |
| POST | `/attendance/check-out` | STAFF, ADMIN | `{"memberId": 5}`; closes the open visit |
| GET | `/attendance/occupancy` | STAFF, ADMIN | Members inside right now |
| GET | `/attendance/me?page=&size=` | MEMBER | Own visit history |
| GET | `/attendance/members/{memberId}?page=&size=` | STAFF, ADMIN | A member's visit history |

Frozen, expired, cancelled and not-yet-started memberships cannot check in (`403`). Checking in twice is
`409`. A visit left open for more than 12 hours is treated as a forgotten check-out: it stops counting as
"inside" and does not block the next check-in (the row is left as it is, no check-out time is invented).

## Admin

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/admin/users` | ADMIN | Create a STAFF or ADMIN account |
| PUT | `/admin/users/{id}/status` | ADMIN | `ACTIVE` or `SUSPENDED` (suspending revokes refresh tokens) |

## First admin

Set `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` as environment variables before starting
the backend. If no admin exists, one is created at startup.

## Try it (PowerShell)

```powershell
$base = "http://localhost:8080/api/v1"

# Register a member
$body = @{ email="ravi@example.com"; password="Strong#Pass1"; fullName="Ravi Kumar"; phone="+91 98765 43210"; acceptTerms=$true; marketingConsent=$false; aiProcessingConsent=$true } | ConvertTo-Json
$tokens = Invoke-RestMethod -Method Post -Uri "$base/auth/register" -ContentType "application/json" -Body $body

# Call a protected endpoint
Invoke-RestMethod -Uri "$base/members/me" -Headers @{ Authorization = "Bearer $($tokens.accessToken)" }
```

## Known limits (planned hardening)

- Selling a plan does not record a payment yet; the payment module (Phase 2) will.
- Cancelling a running membership does not move memberships queued behind it.
- QR self check-in is not built yet (front desk only).

- No login rate limiting or account lockout yet.
- A suspended user's access token stays valid until it expires (15 minutes); refresh is blocked immediately.
- Passwords set by an admin for staff accounts are not forced to change on first login.
