# Production Security Hardening

Date: 2026-06-25

Domain: `https://yxt.yqej.cn`

## Current Result

The Yanxitong production edge has been hardened for the current pilot-preparation phase.

Validation result:

- `production-ops-check.sh`: passed with the expected readiness warning.
- `production-browser-smoke.sh`: passed after CSP/security headers were enabled.
- `production-api-acceptance.sh`: passed after edge hardening.
- `production-security-check.sh`: passed with one shared-server warning.

The remaining warning is that the same server exposes unrelated project ports `19031` and `19032`. Yanxitong containers themselves are not directly published to the public internet.

## Applied Edge Nginx Hardening

The active edge config is stored on the server at:

```text
/opt/apps/_edge/conf.d/yanxitong.conf
```

Backup created before hardening:

```text
/opt/apps/_edge/conf.d/yanxitong.conf.bak-20260625183015-security
```

Added security headers:

- `Strict-Transport-Security: max-age=15552000`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: SAMEORIGIN`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()`
- `Content-Security-Policy` scoped to same-origin scripts, styles, API/WebSocket connections and self frame ancestors.

Added rate-limit zones:

```nginx
limit_req_zone $binary_remote_addr zone=yxt_public_entry:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=yxt_payment_entry:10m rate=2r/s;
limit_req_zone $binary_remote_addr zone=yxt_login_entry:10m rate=1r/s;
```

Rate-limited endpoints:

| Endpoint | Zone | Burst |
| --- | --- | --- |
| `/api/invitations/public/` | `yxt_public_entry` | 30 |
| `/api/rsvp/submit` | `yxt_public_entry` | 20 |
| `/api/gifts/payment-orders` | `yxt_payment_entry` | 8 |
| `/api/gifts/offline` | `yxt_public_entry` | 20 |
| `/api/auth/login` | `yxt_login_entry` | 10 |

## Filesystem Hardening

Backup and cron log directories have been restricted to `700 root:root`:

```text
/opt/backups/yanxitong
/opt/backups/yanxitong/mysql
/opt/backups/yanxitong/ops-logs
```

## Public Exposure Review

Yanxitong direct container exposure:

- `yanxitong-server`: no public host port
- `yanxitong-web`: no public host port
- `yanxitong-mysql`: no public host port
- `yanxitong-redis`: no public host port
- public traffic enters through `global-edge-nginx` on `80/443`

Shared-server warning:

- `0.0.0.0:19031`
- `0.0.0.0:19032`
- `[::]:19031`
- `[::]:19032`

These ports belong to another project on the same server. They should be reviewed separately before broader production exposure.

## Repeatable Check

Run:

```bash
bash deploy/scripts/production-security-check.sh
```

The check verifies:

- required security headers
- public 80/443 listeners
- no direct public Yanxitong app/data container ports
- edge rate-limit/security config tokens
- backup directory permissions
- edge Nginx syntax

## Deferred Hardening

The following are intentionally deferred until operational constraints are clearer:

- Admin IP allowlist: requires stable office/VPN egress IPs.
- WAF/CDN layer: useful before broad public traffic.
- SSH hardening beyond current access: should be handled at the server/account level.
- Dedicated host isolation: this server currently hosts other unrelated workloads.
