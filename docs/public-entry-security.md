# Public Entry Security

Date: 2026-06-22

## Scope

This document records the MVP public-entry abuse protection baseline for Yanxitong.

Protected public entry points:

- `GET /api/invitations/public/{shareSlug}`
- `POST /api/rsvp/submit`
- `POST /api/gifts/payment-orders`
- `POST /api/gifts/offline`

The goal is not to build a full fraud engine in MVP. The goal is to prevent simple high-frequency abuse before pilot traffic.

## Rate Limit Strategy

The implementation uses Redis counters through `PublicRateLimitService`.

Current rules:

| Scope | Endpoint | Dimension | Limit |
| --- | --- | --- | --- |
| `invitation-public-view` | public invitation page | IP + shareSlug | 120 / minute |
| `invitation-public-missing` | missing public slug | IP + shareSlug | 12 / 5 minutes |
| `rsvp-submit` | RSVP submit | IP + banquetId + guestName | 20 / 10 minutes |
| `gift-payment-order-create` | online gift / onsite QR order | IP + banquetId + entrySource + guestName | 8 / 10 minutes |
| `gift-offline-create` | offline cash gift record | IP + banquetId + guestName | 20 / 10 minutes |

When Redis is unavailable, the limiter fails open so the accepted MVP business flow is not blocked by infrastructure instability. Production deployments should monitor Redis availability separately.

## Audit Behavior

When a request exceeds the configured threshold:

- API returns HTTP `429`.
- `operation_log` records module `SECURITY`, action `PUBLIC_RATE_LIMIT`.
- Log detail includes scope, IP, limit, window and request dimensions.

## Slug Enumeration

Public invitation access is protected in two layers:

1. General public page access limit by IP and `shareSlug`.
2. Stricter missing-slug limit when the slug cannot be found.

Missing slugs still return the normal not-found behavior. The API does not reveal extra lookup details.

## Payment Order Abuse Protection

Gift payment order creation keeps the existing `clientRequestId` idempotency behavior and adds a separate rate limit by IP, banquet, entry source and guest name.

This prevents repeated order creation bursts while preserving normal retry behavior from the same client.

## Production Notes

- Put Nginx/CDN/WAF rate limits in front of the backend for internet traffic.
- Keep Redis healthy; public rate limits rely on Redis counters.
- Review `SECURITY/PUBLIC_RATE_LIMIT` operation logs during pilot.
- If a real user is blocked, check dimensions before increasing limits globally.
- Do not treat this as payment fraud detection; provider-side risk controls remain required for real money collection.
