# Banquet And Invitation API

## Banquet

### Create banquet

`POST /api/banquets`

Request:

```json
{
  "name": "张三李四婚宴",
  "eventTypeCode": "WEDDING",
  "banquetTime": "2026-10-01T18:00:00",
  "location": "某某酒店",
  "customCopywriting": "",
  "templateId": 1
}
```

Behavior:

- Reads `event_type.default_theme_code`.
- Writes `banquet.theme_code`.
- Creates a base `invitation` instance.
- Creates an `invitation_share` public link record.
- Writes operation logs for banquet and invitation creation.

### List banquets

`GET /api/banquets`

### Detail

`GET /api/banquets/{id}`

Includes:

- banquet
- base invitation
- theme
- resolved gift success copywriting

## Public Invitation

### Invitation detail for editing

`GET /api/invitations/{id}`

Returns:

- `invitation`
- parsed `basicFields`
- miniapp `shareUrl`

The miniapp base invitation editor uses this endpoint to backfill all editable fields before saving. The banquet detail page shows and copies the same share URL.

### Public view

`GET /api/invitations/public/{shareSlug}`

Behavior:

- Finds active invitation by `share_slug`.
- Unknown or expired `shareSlug` returns HTTP 404 with a readable public message instead of leaking the raw service exception.
- Records `invitation_visit_log`.
- Returns invitation, banquet, referenced template, theme, resolved copywriting, parsed `basicFields`, `shareUrl` and standard `actionUrls`.
- Returns `templatePresentation` with preset rendering information: `styleCode`, `headline`, `defaultGreeting`, `defaultScheduleText` and `fallbackCoverLabel`.
- Standard action URLs cover RSVP, online gift, onsite QR gift and device selection, so miniapp/H5 pages do not need to rebuild entrance paths independently.
- `shareUrl` is the miniapp public page path `/pages/invite/public/index?slug={shareSlug}`. `actionUrls.rsvp` includes `banquetId` and `invitationId`; `actionUrls.onlineGift` and `actionUrls.onsiteGift` include `banquetId` and `entrySource`; `actionUrls.device` includes `banquetId`.
- If the invitation references a disabled or deleted template, `templateAvailable` is `false`, `templateMessage` explains the fallback, and `templatePresentation` is resolved from the banquet event type so the public page can continue rendering a base invitation.

Supported base invitation fields inside `basicFields`:

- `hostName`
- `contactPhone`
- `addressDetail`
- `scheduleText`
- `greeting`
- `showGiftEntry`
- `showDeviceEntry`

The miniapp public invitation page renders host/contact information, banquet time, venue, address detail, event schedule, gift entry buttons and device entry button from these fields. `showGiftEntry` and `showDeviceEntry` use `"0"` to hide the corresponding action button and show a disabled-entry notice.

If `greeting` or `scheduleText` is empty, the public page falls back to `templatePresentation.defaultGreeting` and `templatePresentation.defaultScheduleText`. If both invitation cover and template cover are empty, it renders a styled fallback cover using `templatePresentation.fallbackCoverLabel`.

## Template Presets

`GET /api/meta/invitation-templates` returns active templates ordered by `sortOrder`. Each item keeps the original template fields and adds `presentation`, so the miniapp creation page can filter and preview templates before creating a banquet.

The miniapp banquet creation page uses:

- event type recommendation based on template code
- free/paid/all filters
- template preview using `presentation.headline`, `presentation.defaultGreeting`, `presentation.defaultScheduleText` and `presentation.fallbackCoverLabel`
- selected `templateId` in the existing `POST /api/banquets` request

No drag-and-drop editor or template schema table is introduced in this phase.

Seeded preset templates include:

- `DEFAULT_WEDDING`
- `ELEGANT_WEDDING`
- `DEFAULT_BIRTHDAY`
- `WARM_BIRTHDAY`
- `BABY_GARDEN`
- `HOUSEWARMING_MODERN`
- `DEFAULT_SCHOOL`
- `SCHOOL_HONOR`
- `MEMORIAL_SIMPLE`
- `PREMIUM_CEREMONY`
- `CUSTOM_BRAND`
- `DEFAULT_GENERAL`

## Copywriting Priority

Gift success copywriting is resolved in this order:

1. banquet custom copywriting
2. theme copywriting
3. event type default copywriting
4. system default copywriting

## MVP Boundary

- This is a base public invitation page with lightweight template style rendering.
- Full visual invitation editor remains deferred.
