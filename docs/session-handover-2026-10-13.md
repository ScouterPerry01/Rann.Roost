<!--
File path and name: docs/session-handover-2026-10-13.md
Modified On Timestamp: 2026-10-14 @ 03:45 EDT
Created On Timestamp: 2026-10-14 @ 03:45 EDT
File Description: Handover for the session of 2026-10-13/14: the Store rejection of 1.1.0, the clearer start-up message, release 1.1.1.
Uses: docs/NOW.md, docs/release-checklist.md, app/desktop/.../StartupFailure.kt.
Used By: Claude Code and the owner when resuming.
Purpose: Keep the detail that does not belong in NOW.md or the session log.
-->

# Session handover — 2026-10-13 (to 2026-10-14 03:45)

## 1. The rejection of 1.1.0

- Report (2026-10-13): policy 10.1.2.10 Functionality, "The product crashes at launch", OS build
  **22621.1265** (Windows 11 22H2, early 2023), observed on Dell Latitude 5520, tested devices Microsoft
  Surface Laptop. Error message: "RANN's Roost could not start". Saved as
  `docs/MS.Cert.Issue/Cert.Crash.Issue.2026-10-13.docx` (ignored by git); no support ZIP downloaded yet.
- That title is our own `startup.failed.title` (`StartupFailure.kt`, added in 1.1.0): Java started and
  `main()` caught the error. The body (the error text) and the log were not in the report.
- Not reproducible here: the owner registered the MSIX layout locally (build 26200) and the app opened.
  Developer Mode only affects installation, not how the app runs.

## 2. What changed (PR #10, merged as `1215ea2b`)

- `StartupFailure.summary()`: the box shows the error, "Caused by: <root cause>" (when different, chain
  followed at most 20 deep) and up to 8 `at ...` lines of the root cause. Same message keys, EN/FR.
- `StartupFailureTest` (2 tests). Version **1.1.1** (`gradle.properties`), notes `docs/releases/1.1.1.*.md`,
  `AboutDetailsTest` expects 1.1.1 as the newest notes.
- Tag `v1.1.1` (annotated); release run 38528167425 green (MSIX self-check OK, "Window check (Java Access
  Bridge on): OK" on the Windows Server runner; Linux .deb/.rpm/AppImage self-checks OK); GitHub release
  published as Latest. The MSIX is the run's `msix` artifact; **not yet uploaded** to Partner Center.

## 3. Suspects (unconfirmed until the error text arrives)

- Graphics: Compose/Skia uses DirectX 12 and falls back; older 22H2 drivers or a VM are a weak spot.
- The tray icon / notification made at start (`Main.kt`).
- `AppState`: Java preferences (registry), `HouseholdStore` (SQLCipher native unpacked to temp).
- Locale/region of the test account.
- Possible local checks once relevant: force Skia software/OpenGL rendering; other locales; a signed test
  MSIX installed into WindowsApps (test certificate created by the owner in their own PowerShell window).

## 4. Microsoft support

- Owner emailed Microsoft support asking for: a screenshot of the full message, `startup-error.log`
  (`%LOCALAPPDATA%\RANN's Roost\` or `%LOCALAPPDATA%\Packages\RANN.RannsRoost_<id>\LocalCache\Local\RANN's Roost\`),
  Event Viewer Application errors, and device details (physical/VM, graphics driver, region, assistive tech).
- Note: `reportapp@microsoft.com` is likely the policy-violation address; Partner Center support
  (https://aka.ms/storesupport) is the certification channel if no reply comes.

## 5. Tests

- `./gradlew :app:desktop:test`: 59 tests, 0 failed (before the bump).
- Full `./gradlew build` on the branch with the bump: BUILD SUCCESSFUL, 991 tests, 0 failed, 1 skipped
  (HeifTest by design), no warnings (only `:app:desktop:test` re-ran; core results from Gradle up-to-date).
- PR #10 CI green (Windows 14m, Ubuntu 26.04 5m, Performance 5m; build cache used).
- Not tested: the start-up box on screen with the longer text.

## 6. Decisions (owner)

- Release the clearer message as 1.1.1 now, before Microsoft's answer; merge, tag and publish approved
  step by step. Store upload of 1.1.1 left to the owner's timing.
