# MVP Acceptance Conclusion

## Conclusion

Yanxitong MVP passed the local product and technical acceptance check.

The latest full acceptance run used:

```bash
bash deploy/scripts/local-acceptance.sh
```

Result:

- Overall status: `passed`
- Run ID: `20260622212002`
- Demo banquet ID: `106`
- Confirm-screen bind code: `DEMO-CS-local-acceptance-20260622212019`
- Consolidated summary: `/var/folders/4b/gwp_mz5x1sb7_9rq71ydgnpw0000gn/T//yanxitong-local-acceptance-20260622212002/summary.json`

## Verification Results

| Check | Result | Evidence |
| --- | --- | --- |
| Backend MVP smoke | Passed | `/var/folders/4b/gwp_mz5x1sb7_9rq71ydgnpw0000gn/T//yanxitong-smoke-20260622212013` |
| Demo data seed | Passed | `/var/folders/4b/gwp_mz5x1sb7_9rq71ydgnpw0000gn/T//yanxitong-demo-local-acceptance-20260622212019` |
| Public invitation browser smoke | Passed | 2 screenshots generated |
| Admin browser smoke | Passed | 14 passed, 0 failed |
| Confirm-screen browser smoke | Passed | 8 passed, 0 failed |
| Confirm-screen screenshots | Passed | 8 screenshots generated |
| Service cleanup | Passed | No residual listeners on `8080`, `5173`, `5174` |

Admin smoke artifacts:

```text
/var/folders/4b/gwp_mz5x1sb7_9rq71ydgnpw0000gn/T/yanxitong-admin-smoke-20260622132025
```

Public invitation smoke artifacts:

```text
/var/folders/4b/gwp_mz5x1sb7_9rq71ydgnpw0000gn/T/yanxitong-public-invitation-smoke-20260622132020
```

Confirm-screen smoke artifacts:

```text
/var/folders/4b/gwp_mz5x1sb7_9rq71ydgnpw0000gn/T/yanxitong-confirm-screen-smoke-20260622132048
```

## Accepted MVP Scope

- Platform admin login and configuration center.
- Banquet creation with event type, theme and base public invitation.
- Template selection and lightweight public-page rendering.
- Plan order, rights activation and lightweight device order loop.
- RSVP submit/update/list/stats.
- Unified online gift and onsite QR payment order flow.
- Offline cash gift recording.
- Gift record, favor ledger and live bilateral comparison.
- Confirm-screen bind, standby, success and offline pages.
- WebSocket/latest-event based confirm-screen recovery.
- Simulated cloud-speaker and confirm-screen broadcast logs.
- Operation logs for key platform and business actions.
- Admin review pages for business data, orders, payments, broadcast logs and operation logs.
- CSV/XLSX export endpoints under the `EXCEL_EXPORT` entitlement.
- Provider/Adapter payment boundary with local mock callback signature verification.

## Confirmed Boundaries

- Further export hardening, larger datasets and async delivery are outside the current MVP acceptance.
- Agent, hotel and wedding-company independent workspaces are not included.
- Complex visual invitation editor is not included.
- Device inventory scheduling, deposit, repair and return flows are not included.
- Real hardware SN is not required for confirm-screen binding.
- Production payment institution integration remains behind the adapter boundary.
- `favor_compare_snapshot` is not used.
- Separate `confirm_screen_event` table is not used; confirm-screen events are derived from gift success and traceable through broadcast logs.

## Next P1 Priorities

1. Replace the WeChat service-provider placeholder with official certificate/signature verification.
2. Harden export performance, row limits and delivery behavior for larger datasets.
3. Expand invitation template preview pages and richer template presets.
4. Add stronger visual regression storage for admin and confirm-screen screenshots.

The detailed P1 plan is recorded in `docs/p1-roadmap.md`.
