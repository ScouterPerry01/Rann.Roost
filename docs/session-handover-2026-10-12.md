<!--
File path and name: docs/session-handover-2026-10-12.md
Modified On Timestamp: 2026-10-12 @ 23:59 EDT
Created On Timestamp: 2026-10-12 @ 10:18 EDT
File Description: Handover for 2026-10-12: the Store rejection of 1.0.0, its fix, release 1.1.0, workflow add-ons, Ubuntu 26.
Uses: docs/NOW.md, docs/release-checklist.md, .github/workflows/release.yml.
Used By: Claude Code and the owner when resuming.
Purpose: Keep the detail that does not belong in NOW.md or the session log.
-->

# Session handover — 2026-10-12

## 1. The rejection

- Microsoft Store certification of 1.0.0 failed (policy 10.1.2.10 Functionality): "The product crashes at
  launch", Lenovo ThinkBook, OS build 26200.8875, box "Failed to launch JVM". The report and its support
  ZIP (one screenshot, no logs) are in `docs/MS.Cert.Issue/`, ignored by git.
- That box is the jpackage launcher's: it appears whenever `main()` ends with an exception.
- Ruled out on this PC: a read-only install folder, the working directory, a first run with no saved
  settings (HKCU prefs set aside and restored), packaged identity (`Add-AppxPackage -Register`).
- **Cause, reproduced with the submitted package:** with Java Access Bridge on
  (`%USERPROFILE%\.accessibility.properties` = `assistive_technologies=com.sun.java.accessibility.AccessBridge`),
  AWT loads `jdk.accessibility` as the window opens; the runtime did not have it (`suggestRuntimeModules`
  does not list it). So 1.0.0 also fails for Windows users of screen readers using Access Bridge.

## 2. What changed (branch `fix/store-first-launch`)

- `jdk.accessibility` added to `modules(...)`; `PackagedSelfCheck` loads a class from it.
- `StartupFailure.kt` + `Main.kt`: a start-up error is written to `%LOCALAPPDATA%\RANN's Roost\startup-error.log`
  (Linux `~/.local/state/ranns-roost/`) and shown in EN/FR (`startup.failed.*`); when no window can show,
  the error goes on to the launcher's box.
- MSIX manifest: `PackageDependency` on `Microsoft.VCLibs.140.00.UWPDesktop` (ONNX Runtime needs
  `MSVCP140_1.dll`, not in the packaged JDK). Not provable here (this PC has the runtime).
- `release.yml`: step "The window opens (Java Access Bridge on)" on the MSIX layout. Not yet run on GitHub.
- `release-checklist.md` § 6: the same check by hand.
- **Calendar** moved from the Money group to the top of the menu (Dashboard, Contacts, Calendar), owner's
  request; manual and help texts EN/FR; every manual picture (EN/FR, light/dark) and Store screenshot retaken.
- Version 1.1.0 (`gradle.properties`), release notes `docs/releases/1.1.0.en.md` / `.fr.md`.
- `.gitignore`: `docs/MS.Cert.Issue/`; headers added to `.gitignore`, `release.yml`, `gradle.properties`,
  `app/desktop/build.gradle.kts` (missed in yesterday's commit), `Navigation.kt`, `AppxManifest.xml`.

## 3. Tests

- Submitted 1.0.0 package with Access Bridge on: launcher box (reproduced). Fixed package, installed with
  `-Register`, Access Bridge on, no saved settings: window opens. Self-check OK (8 JDK modules, OCR, HEIC,
  AI client, database). Forced failure (`-Djava.awt.headless=true`): log written, launcher box shown.
- Not tested: the bilingual start-up message itself (no failure that still allows a window was found).
- Full local `./gradlew build`: 985 tests, 0 failed, 1 skipped (HeifTest by design), no warnings (after
  updating `AboutDetailsTest` for the 1.1.0 notes).
- CI on PR #7 green (Windows ~33 min). Release dry run on `main` and the real `v1.1.0` run: "Window check: OK".

## 4. Resubmission note for the Store testers (suggested)

"1.1.0 fixes the crash at launch reported for 1.0.0: the app failed to start when Java Access Bridge was
enabled. Tested with Access Bridge enabled on Windows 11 build 26200."

## 5. Decisions (owner)

- Release the fix as **1.1.0 from `main`**, with the sample households, after the owner tried them.
- Calendar stands on its own at the top of the menu, under Contacts.
- `gradle/actions/setup-gradle` stays on **v5** (decisions log): v6 puts caching under Gradle's commercial
  Terms of Use.

## 6. Release 1.1.0 — where it stands

- PR #7 merged (`44eaa1e6`); tag `v1.1.0` pushed; release run green. **GitHub release is a draft, not
  published** (Linux .deb/.rpm/AppImage, signatures, SHA256SUMS, update.json). Publishing it tells 1.0.0
  Linux copies about the update.
- MSIX (`RANNsRoost-1.1.0-x64.msix`, SHA-256 `bd682121…7c1`) downloaded to the owner's
  `Downloads\RANNsRoost-1.1.0\`. Checked inside: identity `RANN.RannsRoost`, version 1.1.0.0, VCLibs
  dependency, `jdk.accessibility` in the runtime.
- Store resubmission (owner, Partner Center > **RANN's Roost**, not another product): new package,
  screenshots from `docs/store/screenshots` (EN/FR), certification note from § 4. Not confirmed done.
- Linux download link for rann.ca: `https://github.com/ScouterPerry01/Schippers.Household.Finance.Manager/releases/latest`
  (newest published release; page, not a file, since file names carry the version).

## 7. Workflow add-ons (PR #8, merged, `d524cdb7`)

- checkout v7, setup-java v6, upload-artifact v7, download-artifact v8, setup-gradle v5; headers added to
  `ci.yml` and `natives.yml`. Node 20 / setup-java warnings gone.
- Proven by: PR checks, a release dry run from the branch (window check OK), a `natives.yml` run (helper
  SHA-256 `A0DAA128…`, not compared with the committed copy; the helper did not change).

## 8. Ubuntu 26.04 (PR #9, `chore/ubuntu-26`; merged later, see § 10)

- `ubuntu-latest` -> `ubuntu-26.04` in `ci.yml` (Ubuntu tests, performance) and `release.yml` (version,
  Linux packages, sign-and-draft). GitHub moves `ubuntu-latest` to 26 from 2026-10-19.
- Proven on 26.04: libheif packages install (1.21.2); .deb, .rpm, AppImage build; self-check of the
  installed .deb and the AppImage OK (8 JDK modules, OCR, HEIC, AI client, database); .rpm on Fedora OK.
- **Not proven:** the unit tests on 26 — Gradle's build cache (`org.gradle.caching=true`) reused the
  Ubuntu 24 results because the code did not change (all test tasks FROM-CACHE).
- Owner to choose: merge (recommended; tests run on 26 at the next code change) or first add a
  `workflow_dispatch` "re-run everything" option to `ci.yml` (`--rerun-tasks`) and run it.

## 9. Housekeeping

- Branches `fix/store-first-launch` and `chore/workflow-updates` deleted locally and on GitHub (the second
  without asking again; owner may prefer to be asked each time).
- Later: GitHub repository rename to `Rann.Roost`; record the RANN's Roost Store ID in the release checklist
  once the listing is live; release checklist still names 1.0.0 file names in § 6-7 as examples.

## 10. Late session (23:20 -> 23:59): resubmitted, merged, published

- Microsoft Store: owner resubmitted 1.1.0; certification still in progress at the end of the session.
- PR #9: owner chose to merge without a no-cache test run. Notes committed on the branch (`ee2ccabe`),
  checks green again (Ubuntu job 37 s: tests still FROM-CACHE), merged with a merge commit (`287975fb`).
  `chore/ubuntu-26` deleted locally and on GitHub (owner asked). The unit tests first really run on
  Ubuntu 26 at the next code change; if they fail there, re-run without the build cache (`--rerun-tasks`).
- Release: draft checked (3 Linux packages, `.minisig` for each, `SHA256SUMS`, `update.json`, all signed)
  and published as Latest by the owner's decision, before Store certification. Not yet checked: that an
  installed 1.0.0 Linux copy actually offers 1.1.0.
- These notes were committed straight to `main` at the owner's request (no topic branch).
