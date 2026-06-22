# Miniapp Dependency Security Audit

Date: 2026-06-22

## Command

```bash
cd miniapp
npm audit --json
npm audit fix --dry-run --json
```

## Result Summary

`npm audit` currently reports:

| Severity | Count |
| --- | ---: |
| Critical | 0 |
| High | 11 |
| Moderate | 14 |
| Low | 16 |
| Total | 41 |

`npm audit fix --dry-run` does not reduce the vulnerability count. It proposes dependency graph changes around the uni-app/Vite toolchain and still reports the same 41 vulnerabilities.

## Main Dependency Clusters

### Uni-App Toolchain

Affected packages include:

- `@dcloudio/uni-app`
- `@dcloudio/uni-app-plus`
- `@dcloudio/uni-components`
- `@dcloudio/uni-h5`
- `@dcloudio/uni-mp-weixin`
- `@dcloudio/vite-plugin-uni`
- `@dcloudio/uni-cli-shared`

The audit report marks many fixes as semver-major. These packages are core build/runtime dependencies for the WeChat miniapp build, so they should not be upgraded blindly.

### Vite And Esbuild

Affected packages:

- `vite`
- `esbuild`

Current miniapp uses `vite` 7.3.3 through the uni-app toolchain. Reported Vite issues are development-server focused, including Windows path/UNC handling. The production miniapp build still completes successfully.

### WebSocket, Express And Static Server Transitives

Affected packages:

- `ws`
- `express`
- `body-parser`
- `serve-static`
- `send`
- `qs`
- `path-to-regexp`
- `cookie`

These are transitive dependencies under the miniapp build tooling. The current MVP miniapp does not run these packages as a public Node.js server in production, but they still matter for local development security and CI hygiene.

### Image And Internationalization Transitives

Affected packages:

- `jimp`
- `jpeg-js`
- `phin`
- `@intlify/core-base`
- `@intlify/message-compiler`
- `@intlify/message-resolver`
- `@intlify/runtime`
- `@intlify/vue-devtools`

These are pulled through uni-app compiler/runtime dependencies. They require upstream-compatible uni-app updates or package override experiments.

## Current Risk Position

The findings are real, but the current operational exposure is limited because:

- The miniapp build artifact is static WeChat mini-program output.
- The affected server-side packages are primarily build/dev tooling dependencies.
- No public Node.js miniapp dev server is part of the production deployment loop.
- The current build is reproducible and passes `npm run build`.

The findings are still a release hardening item because developers and CI may run the affected tooling.

## Recommendation

Do not run:

```bash
npm audit fix --force
```

as part of the current branch. It may cross major versions in the uni-app toolchain and can break WeChat miniapp compilation or runtime behavior.

Use this safer sequence instead:

1. Create a separate branch from `develop`, for example `feature/miniapp-toolchain-upgrade`.
2. Upgrade `@dcloudio/*`, `@dcloudio/vite-plugin-uni` and `vite` as a coordinated toolchain set.
3. Run:

   ```bash
   cd miniapp
   npm ci
   npm run build
   ```

4. Import `miniapp/dist/build/mp-weixin` into WeChat DevTools.
5. Smoke-check core pages:
   - home
   - banquet create
   - public invitation
   - RSVP submit
   - gift payment order
   - gift success
   - favor ledger
   - device select
6. Re-run `npm audit` and compare counts.

## Temporary Exception

Until the controlled toolchain upgrade is tested, keep the current dependencies and record this exception:

- Owner: engineering
- Reason: vulnerabilities are concentrated in miniapp build/dev transitive dependencies; automatic major upgrades are high risk for uni-app compatibility.
- Next review trigger: before pilot release, before enabling CI-hosted miniapp builds, or when DCloud publishes a compatible fixed toolchain release.

## Verification Baseline

The current baseline still passes:

```bash
cd miniapp
npm ci
npm run build
```

Latest observed build result:

```text
DONE  Build complete.
Run method: open Weixin Mini Program Devtools, import dist/build/mp-weixin run.
```
