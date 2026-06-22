# WeChat Callback Fixtures

This directory is reserved for redacted WeChat Pay service-provider callback samples.

Do not commit real signatures, key material, OpenIDs, payer names, phone numbers, raw ciphertext or unredacted decrypted payloads.

Use `docs/wechat-callback-fixture-policy.md` before adding or replacing fixtures.

Current files are placeholders that define the expected shape:

- `success.redacted.json`
- `failed-signature.redacted.json`

After real onboarding, keep at least:

- one verified success sample
- one failed verification sample

Both samples must be redacted before commit.
