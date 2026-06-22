# Engineering Initialization

## Confirmed Boundaries

- Online gift payment and onsite QR payment share one online gift payment capability.
- Payment business code must depend on `PaymentAdapter`, not a concrete provider SDK.
- Base public invitation page is part of the second batch and supports sharing.
- MVP device orders are lightweight: need flag, rental time, price, unit, delivery method, payment status and admin visibility.
- Confirm screen can bind through `bind_code` and `banquet_id`; real hardware SN is optional in MVP.
- Copywriting priority: banquet custom copywriting, theme copywriting, event type default copywriting, system default copywriting.
- `operation_log` is a general key-operation log.
- `invitation` is an explicit backend module.

## Deferred

- Formal Excel export
- Agent, hotel and wedding-company workspaces
- Complex visual invitation editor
- Complex device inventory scheduling
- Deposit, repair and return flows
- Real cloud speaker deep integration
- `favor_compare_snapshot`
- `confirm_screen_event`

