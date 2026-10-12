<!--
File path and name: docs/session-handover-2026-10-12.md
Modified On Timestamp: 2026-10-12 @ 10:18 EDT
Created On Timestamp: 2026-10-12 @ 10:18 EDT
File Description: Handover for 2026-10-12: the Microsoft Store rejection of 1.0.0, its fix, and version 1.1.0.
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
- Full `./gradlew build`: see the session log.

## 4. Resubmission note for the Store testers (suggested)

"1.1.0 fixes the crash at launch reported for 1.0.0: the app failed to start when Java Access Bridge was
enabled. Tested with Access Bridge enabled on Windows 11 build 26200."

## 5. Decisions (owner)

- Release the fix as **1.1.0 from `main`**, with the sample households, after the owner tried them.
- Calendar stands on its own at the top of the menu, under Contacts.
