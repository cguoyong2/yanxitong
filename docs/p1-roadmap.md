# P1 Roadmap

## Planning Basis

P1 starts after MVP local acceptance has passed. The accepted baseline is recorded in:

- `docs/mvp-acceptance-conclusion.md`
- `docs/mvp-delivery-package.md`
- `docs/mvp-acceptance-demo.md`

Current MVP boundaries that drive P1:

- Payment provider production integration is reserved behind the existing Provider/Adapter boundary.
- Native `.xlsx` export writer is implemented behind `EXCEL_EXPORT`; CSV remains backward compatible.
- Invitation templates are selectable and affect lightweight public rendering; public invitation now returns parsed basic fields, template presentation presets, share URL and standard action URLs, and the miniapp public page renders contact, address, schedule, fallback cover and entry visibility fields.
- Admin, public invitation and confirm-screen smoke scripts generate screenshots and a machine-readable visual manifest under local acceptance artifacts.

## P1 Objective

Turn the accepted MVP into a stronger pilot-ready version without disturbing the existing MVP business loop.

P1 should preserve:

- `bash deploy/scripts/local-acceptance.sh` as the primary acceptance command.
- Unified payment path for online gift and onsite QR.
- Backend-driven configuration for prices, themes, templates, rights and devices.
- Lightweight device order boundary.
- Confirm-screen events derived from gift success and traceable through `broadcast_log`.

## Priority Order

| Priority | Workstream | Why First |
| --- | --- | --- |
| P1-1 | Payment provider production hardening | Real collection depends on verified provider callbacks and adapter correctness |
| P1-2 | Native `.xlsx` export writer | Completed: CSV/XLSX endpoints, admin buttons and smoke workbook checks |
| P1-3 | Invitation template experience | In progress: public invitation sections, admin template preview, richer preset rendering and miniapp template selection preview completed |
| P1-4 | Visual regression artifact storage | Completed: admin, public invitation and confirm-screen screenshots plus visual manifest |

## P1-1 Payment Provider Production Hardening

Goal: replace placeholder service-provider behavior with production-ready WeChat service-provider integration while keeping business services provider-agnostic.

Detailed design: `docs/p1-payment-production-design.md`.

Tasks:

1. Extend `PaymentAdapter` for provider create-payment requests.
2. Implement WeChat service-provider payment order creation in adapter layer.
3. Replace local HMAC placeholder with official WeChat callback signature and certificate verification.
4. Add certificate serial number and platform certificate management strategy.
5. Preserve `PaymentCallbackResult` as the handoff object to business fulfillment.
6. Keep fulfillment inside `PaymentCallbackService`.
7. Add callback replay/idempotency tests.
8. Add amount, order status and provider trade number validation tests.
9. Add admin visibility for provider health without exposing secrets.
10. Update deployment environment variable documentation.

Acceptance:

- Business services still call `PaymentService`, not provider SDKs or HTTP APIs.
- Mock provider remains available for local acceptance.
- WeChat service-provider adapter can be enabled by environment variables.
- Invalid signatures are recorded as failed callbacks.
- Repeated valid callbacks do not duplicate gift records, favor entries or broadcast logs.
- `local-acceptance.sh` still passes with the mock provider.

Risks:

- Certificate rotation and platform-certificate retrieval need precise operational ownership.
- Provider sandbox behavior may differ from production callback shape.

## P1-2 Native `.xlsx` Export Writer

Goal: add formal Excel files behind the existing export boundary without duplicating export queries.

Tasks:

1. Introduce an export writer interface for CSV and XLSX.
2. Reuse existing gift, RSVP and favor export query methods.
3. Add `.xlsx` endpoints:
   - `/api/admin/exports/banquets/{banquetId}/gifts.xlsx`
   - `/api/admin/exports/banquets/{banquetId}/rsvp.xlsx`
   - `/api/admin/exports/banquets/{banquetId}/favor.xlsx`
4. Preserve `EXCEL_EXPORT` entitlement checks.
5. Add workbook metadata and Chinese sheet names.
6. Add column widths, money/date formats and header styles.
7. Keep CSV endpoints backward compatible.
8. Add operation logs for XLSX export with module `EXPORT`.
9. Extend smoke coverage to validate workbook existence and key cells.

Acceptance:

- CSV endpoints keep passing existing smoke tests.
- XLSX files open with expected Chinese headers and rows.
- Export remains banquet-scoped and admin-only.
- Banquets without `EXCEL_EXPORT` receive the same permission denial behavior.
- Export query logic is shared between CSV and XLSX writers.

Risks:

- Large export memory usage should be capped or streamed before production volume grows.

## P1-3 Invitation Template Experience

Goal: make the public invitation and template selection richer while avoiding a full drag-and-drop editor in early P1.

Tasks:

1. Expand preset templates with stronger visual variants.
2. Add template detail/preview pages for admin and miniapp selection.
3. Add richer public-page sections:
   - host information
   - event schedule
   - venue information
   - RSVP entry
   - gift entry
   - device/confirm-screen hints where relevant
4. Add theme-copywriting slots for invitation scenes beyond gift success.
5. Add template fallback rules when cover/image fields are missing.
6. Add admin status/sort checks for template publishing.
7. Extend demo data seed to include richer invitation fields.

Acceptance:

- Banquet creation still creates one invitation instance.
- Template reference remains on the invitation.
- Public page renders without requiring visual editor data.
- Existing base invitation APIs stay compatible.
- `local-acceptance.sh` still passes.

Risks:

- Rich template rendering can drift into a full editor. Keep P1 focused on curated presets and preview/detail pages.

## P1-4 Visual Regression Artifact Storage

Goal: make frontend acceptance artifacts easier to inspect and compare across runs.

Tasks:

1. Store admin and confirm-screen smoke summaries under the local acceptance artifacts root.
2. Copy confirm-screen screenshots into the local acceptance artifacts root.
3. Optionally add admin screenshots for key pages even on success.
4. Add a machine-readable visual manifest.
5. Add failure-first links in consolidated `summary.json`.
6. Document artifact retention and cleanup expectations.

Acceptance:

- One `summary.json` points to all child summaries and screenshots.
- Confirm-screen keeps 8 required screenshots.
- Failed frontend smoke reports include screenshot paths and failure text.
- The script still cleans up only services it started.

Risks:

- Full visual diffing may require baseline management. Start with artifact collection before introducing pixel comparison gates.

## Suggested P1 Batches

### Batch P1-A: Payment Production Readiness

Deliverables:

- WeChat service-provider adapter create-payment implementation.
- Official callback verification.
- Callback idempotency hardening.
- Provider deployment documentation.

Exit criteria:

- Unit/integration tests cover mock and WeChat adapter paths.
- Mock local acceptance still passes.
- WeChat sandbox or staging callback verification is demonstrated.

### Batch P1-B: XLSX Export

Deliverables:

- XLSX writer.
- XLSX endpoints for gifts, RSVP and favor entries.
- Export smoke extension.

Exit criteria:

- CSV remains backward compatible.
- XLSX files pass automated workbook checks.
- Entitlement and operation logs are verified.

### Batch P1-C: Invitation Template Enhancement

Deliverables:

- Richer presets.
- Template preview/detail.
- Public invitation sections.
- Demo seed enrichment.

Exit criteria:

- Public invitation remains shareable by slug.
- Existing invitation update APIs remain compatible.
- Visual review passes desktop/mobile pages.

### Batch P1-D: Acceptance Artifact Improvements

Deliverables:

- Consolidated artifact manifest.
- Copied screenshots under local acceptance root.
- Optional success screenshots for admin key pages.

Exit criteria:

- One summary file is enough to find logs, screenshots, child summaries and failure details.

## Not Recommended For Early P1

- Agent/hotel/wedding-company independent workspaces.
- Complex drag-and-drop invitation editor.
- Device inventory scheduling, deposit, repair and return workflows.
- Real cloud-speaker hardware protocol.
- Persisted `favor_compare_snapshot`.
- Separate `confirm_screen_event` table unless performance or audit requirements prove it necessary.

These remain valid future work, but they should not interrupt the accepted MVP loop before payment/export/template quality is stabilized.
