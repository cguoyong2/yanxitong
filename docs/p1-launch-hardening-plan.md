# P1 Launch Hardening Plan

Date: 2026-06-22

## Branch

P1 launch hardening starts from:

```text
feature/p1-launch-hardening
```

Base branch:

```text
develop
```

## Objective

Move the accepted MVP baseline toward a pilot-ready release while preserving the existing local acceptance loop:

```bash
bash deploy/scripts/local-acceptance.sh
```

This phase should not rewrite the MVP business chain. It should harden release operations, export limits, payment readiness and miniapp dependency posture.

## Scope

### P1-H1 GitHub Release Governance

Goal: make the MVP baseline easy to recover and compare.

Tasks:

1. Create a GitHub Release from `v0.1.0-mvp-baseline`.
2. Use `docs/mvp-delivery-package.md` and `docs/mvp-release-readiness.md` as release-note sources.
3. Mark the release as an MVP local-acceptance baseline, not a production launch.
4. Keep `main` as stable release history and `develop` as integration history.
5. Require future feature work to merge through PRs.

Acceptance:

- GitHub shows one release for `v0.1.0-mvp-baseline`.
- Release notes clearly state production blockers: real payment rollout, secrets, HTTPS/ingress and provider validation.

### P1-H2 Export Production Hardening

Current status: CSV/XLSX export exists behind `EXCEL_EXPORT`, with a 10,000-row cap, explicit oversize rejection and operation logs.

Tasks:

1. Add automated tests for the export row cap. Done in `ExportServiceTests`.
2. Add tests for entitlement denial on all CSV/XLSX endpoints. Done in `ExportServiceTests`.
3. Add tests that operation logs include export format, export type and row count. Done in `ExportServiceTests`.
4. Add admin UI empty-state and error-state handling for export failures. Export failure handling and per-file loading states are done in `BusinessView`.
5. Decide whether row cap should be configurable through `config_item` or application properties. Done: `config_item` key `export.max_rows`, seeded by `V20__seed_export_row_limit_config.sql`.

Acceptance:

- CSV and XLSX remain backward compatible.
- Export denial behavior is identical across gift, RSVP and favor exports.
- Large export attempts fail predictably before workbook generation.
- `local-acceptance.sh` and `release-readiness.sh` still pass.

### P1-H3 Payment Production Readiness

Current status: Provider/Adapter boundary exists; Mock is local; WeChat service-provider adapter and SDK boundary are prepared but still need real-account validation.

Tasks:

1. Add a launch checklist endpoint or admin checklist view for missing payment provider fields.
2. Add tests for provider-disabled and provider-misconfigured create-payment failures.
3. Add callback fixture tests for invalid signature, duplicated event ID, amount mismatch and non-success trade state.
4. Keep Mock disabled by default outside local acceptance.
5. Keep business services isolated from provider SDK classes.
6. Prepare a staging callback sample folder after real WeChat onboarding.

Acceptance:

- Admin can identify which production payment fields are missing without seeing secrets.
- Misconfigured real provider fails before creating an externally unusable order.
- Replayed callbacks do not duplicate `gift_record`, `favor_entry` or `broadcast_log`.
- Real provider work remains optional until formal WeChat service-provider/sub-merchant onboarding.

### P1-H4 Miniapp Dependency Security

Current status: `miniapp npm audit` reports 41 vulnerabilities: 16 low, 14 moderate, 11 high, 0 critical.

Tasks:

1. Track the audit result in `docs/miniapp-dependency-security-audit.md`.
2. Avoid blind `npm audit fix --force` because it proposes major dependency changes in the uni-app toolchain.
3. Test a controlled uni-app toolchain upgrade in a separate branch.
4. Run `npm run build` and WeChat DevTools import after any upgrade.
5. Add an audit command to release-readiness documentation once a tolerable baseline is chosen.

Acceptance:

- Current MVP build remains reproducible.
- Any dependency upgrade preserves `uni build -p mp-weixin`.
- Security exceptions are documented with owner, reason and next review trigger.

### P1-H5 Public Invitation And Share Hardening

Current status: public invitation by slug, RSVP entry and gift entry exist.

Tasks:

1. Add explicit invalid-slug and disabled-template UI states in miniapp.
2. Add share parameter documentation for `shareSlug`, `banquetId` and public action URLs.
3. Add public-page smoke coverage for missing cover, unpublished template and disabled entry states.
4. Keep template rendering preset-based; do not start a full visual editor in this phase.

Acceptance:

- Public page failures are user-readable, not raw API errors.
- Share links remain stable across template changes.
- Public entry rate limits remain active.

## Not In This Branch

- Real WeChat merchant onboarding.
- Agent, hotel or wedding-company workspaces.
- Complex invitation visual editor.
- Device inventory scheduling, deposit, repair or return flows.
- Real cloud speaker hardware protocol.

## Verification Commands

Run before merging this branch:

```bash
bash deploy/scripts/release-readiness.sh
bash deploy/scripts/local-acceptance.sh
cd miniapp && npm audit
```

If miniapp dependency work is not touched in a sub-branch, audit findings may remain documented rather than fixed.
