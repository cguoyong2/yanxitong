# WeChat Callback Fixture Policy

Date: 2026-06-22

## Purpose

This policy defines how to collect WeChat Pay callback samples after formal service-provider/sub-merchant onboarding.

The goal is to keep regression fixtures useful without storing secrets, real payer identifiers or raw production personal data.

## Fixture Location

Use:

```text
server/src/test/resources/payment/wechat-callback-fixtures/
```

Current placeholder files:

- `success.redacted.json`
- `failed-signature.redacted.json`

## Allowed Content

Fixtures may keep these structural fields:

- callback HTTP method and path
- WeChat callback header names
- redacted header value shape
- notification event id shape
- event type
- resource type
- algorithm
- local test order number shape
- provider transaction id shape
- amount in cents
- trade state
- parser outcome

## Required Redaction

Before committing any real sample, redact:

- `Wechatpay-Signature`
- `Wechatpay-Serial`
- `Wechatpay-Nonce`
- final transaction id when it can identify a real payment
- payer OpenID
- payer name
- phone number
- raw `ciphertext`
- raw `associated_data`
- raw `nonce`
- raw decrypted personal or merchant-sensitive values
- merchant private key paths outside generic mount examples
- API v3 key
- any real certificate or public key content

Use obvious placeholders:

```text
REDACTED_SIGNATURE
REDACTED_SERIAL
REDACTED_NONCE
REDACTED_CIPHERTEXT
REDACTED_OPENID
```

## Capture Steps

1. Use an isolated test banquet and a low-value payment.
2. Save the callback log id, local order number and provider transaction id in the launch record outside this repository if real values must be retained.
3. Copy the callback headers and body from `payment_callback_log`.
4. Redact all sensitive fields before placing the sample under `server/src/test/resources/payment/wechat-callback-fixtures/`.
5. Keep one successful verified sample and one failed verification sample.
6. Add or update parser tests using the redacted sample shape.
7. Run:

   ```bash
   mvn -q -f server/pom.xml test
   bash deploy/scripts/release-readiness.sh
   ```

## Commit Rules

- Do not commit real callback signatures.
- Do not commit encrypted resource bodies from real payments unless ciphertext and nonce are replaced.
- Do not commit decrypted payer identifiers.
- Do not commit secrets or certificate contents.
- Do not overwrite audit records in the database to make a sample cleaner.

## Review Trigger

Review this policy before:

- first real WeChat payment validation
- changing certificate mode
- enabling callback retry in a staging environment
- adding automated tests that parse fixture files
